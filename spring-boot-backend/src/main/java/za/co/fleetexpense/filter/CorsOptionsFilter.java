package za.co.fleetexpense.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(-100) // Run before Spring Security
public class CorsOptionsFilter implements Filter {

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            String requestOrigin = httpRequest.getHeader("Origin");
            String[] allowedOriginsArray = allowedOrigins.split(",");
            boolean isAllowed = false;
            
            if (requestOrigin != null) {
                for (String allowed : allowedOriginsArray) {
                    if (allowed.trim().equals(requestOrigin)) {
                        isAllowed = true;
                        break;
                    }
                }
            }
            
            if (isAllowed && requestOrigin != null) {
                httpResponse.setHeader("Access-Control-Allow-Origin", requestOrigin);
            } else {
                httpResponse.setHeader("Access-Control-Allow-Origin", allowedOriginsArray[0].trim());
            }
            httpResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
            httpResponse.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, X-Requested-With, Accept, Origin, Access-Control-Request-Method, Access-Control-Request-Headers");
            httpResponse.setHeader("Access-Control-Allow-Credentials", "true");
            httpResponse.setHeader("Access-Control-Max-Age", "1728000");
            httpResponse.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }

        chain.doFilter(request, response);
    }
}

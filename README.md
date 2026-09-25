# Vehicle Expense Tracking App

A South African fleet management application with SARS-compliant tax calculations, vehicle expense tracking, and logbook management.

## Prerequisites

- **Java 21+**
- **PostgreSQL** (running on localhost:5432)
- **Node.js** and **pnpm** (for frontend)
- **Git**

## Database Setup

1. **Create PostgreSQL database:**
   ```sql
   CREATE DATABASE expense_tracking_backup;
   ```

2. **Run database migrations:**
   - Navigate to `spring-boot-backend/src/main/resources/bootstrap/`
   - Execute `V0__base_schema.sql` to create the base schema
   - Navigate to `spring-boot-backend/src/main/resources/db/migration/`
   - Execute all migration files (V6 through V23) in order

3. **Create acquisition facts table:**
   - Run `create_acquisition_facts_table.bat` (Windows) or execute `create_acquisition_facts_table.sql` manually in PostgreSQL

## Environment Variables

Create a `.env` file in the project root or set these environment variables:

```bash
# Required
JWT_SECRET=your-secret-key-here

# Optional (defaults shown)
DB_HOST=localhost
DB_PORT=5432
DB_NAME=expense_tracking_backup
DB_USERNAME=stefan
DB_PASSWORD=test123
PORT=8080
```

## Running the Application

### Backend (Spring Boot)

```bash
cd spring-boot-backend
./gradlew.bat bootRun  # Windows
# or
./gradlew bootRun      # Linux/Mac
```

The backend will start on `http://localhost:8081`

### Frontend (Next.js)

```bash
cd frontend
pnpm dev
```

The frontend will start on `http://localhost:3000`

## Features

- **Vehicle Tax Profiles**: Configure tax settings per vehicle
- **Acquisition Facts**: Track vehicle acquisition details for tax calculations
- **Tax Calculations**: 
  - Actual Costs Method
  - SARS Cost Scale Method
  - Simplified Reimbursive (AA Rates)
- **Running Estimate**: Uses logged trips when closing odometer is unavailable
- **Business Share Capping**: Automatically caps at 100% to prevent invalid calculations
- **Dark Mode**: Full dark mode support
- **SARS eFiling Export**: Generate SARS-compliant logbook exports

## Database Schema

Key tables:
- `vehicles` - Vehicle information
- `vehicle_tax_profiles` - Tax configuration per vehicle
- `vehicle_tax_acquisition_facts` - Acquisition details for tax calculations
- `trips` - Trip logs with business/private classification
- `expenses` - Vehicle expenses (fuel, maintenance, etc.)
- `sars_cost_scale_brackets` - SARS tax brackets
- `sars_prescribed_rates` - AA rates per tax year

## Troubleshooting

**Backend won't start:**
- Check PostgreSQL is running on localhost:5432
- Verify database `expense_tracking_backup` exists
- Ensure JWT_SECRET environment variable is set

**Frontend login issues:**
- Ensure backend is running on port 8081
- Check browser console for errors
- Verify CORS settings in `application.yml`

**Tax calculation shows R 0,00:**
- Add fuel entries or set closing odometer verification
- Ensure qualifying expenses are classified correctly
- Click "Calculate Tax Summary" to refresh

## Development

The project uses:
- **Backend**: Spring Boot 3.3.5, Java 21, PostgreSQL, Hibernate
- **Frontend**: Next.js 16.2.6, React, Tailwind CSS, shadcn/ui
- **Build**: Gradle (backend), pnpm (frontend)

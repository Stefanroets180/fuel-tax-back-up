@echo off
set PGPASSWORD=DARKSTAR420
"C:\Program Files\PostgreSQL\16\bin\psql.exe" -h localhost -p 5432 -U postgres -d expense_tracking_backup -f "C:\Users\stefa\ZED-COPY\full project\1expense-tracking-app\create_acquisition_facts_table.sql"

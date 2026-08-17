@echo off
cd /d "%~dp0"

echo ==================================================
echo   LifeArchive - Docker Compose Start
echo ==================================================
echo.

echo Pulling latest images...
docker pull mysql:8.0 >nul 2>&1
docker pull redis:7-alpine >nul 2>&1

echo Building backend image...
docker compose build backend
if %errorlevel% neq 0 (
    echo ERROR: Build failed. Did you run "mvnw package -DskipTests" first?
    pause
    exit /b 1
)

echo.
echo Starting all services...
docker compose up -d
if %errorlevel% neq 0 (
    echo ERROR: Docker Compose failed. Check Docker Desktop.
    pause
    exit /b 1
)

echo.
echo Waiting for backend to be ready...
timeout /t 10 /nobreak >nul

echo.
echo ==================================================
echo   All services are running in Docker Desktop!
echo.
echo   MySQL    : localhost:3306
echo   Redis    : localhost:6379
echo   Backend  : http://localhost:8080
echo   Health   : http://localhost:8080/api/health
echo.
echo   Now start the frontend in a NEW terminal:
echo     cd frontend
echo     npm run dev
echo.
echo   Frontend : http://localhost:5173
echo.
echo   Stop all : stop.bat
echo ==================================================
pause

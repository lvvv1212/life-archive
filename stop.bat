@echo off
cd /d "%~dp0"
echo Stopping all LifeArchive containers...
docker compose down
echo.
echo All containers stopped. Data preserved in Docker volumes.
pause

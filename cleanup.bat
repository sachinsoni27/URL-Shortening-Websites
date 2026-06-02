@echo off
echo Removing Java/Spring Boot project files...

:: Remove files
del /f /q "d:\Link shortner\pom.xml" 2>nul
del /f /q "d:\Link shortner\Dockerfile" 2>nul
del /f /q "d:\Link shortner\docker-compose.yml" 2>nul
del /f /q "d:\Link shortner\README.md" 2>nul
del /f /q "d:\Link shortner\run.bat" 2>nul
del /f /q "d:\Link shortner\css\style.css" 2>nul

:: Remove directories
rmdir /s /q "d:\Link shortner\src" 2>nul
rmdir /s /q "d:\Link shortner\.mvn" 2>nul
rmdir /s /q "d:\Link shortner\website" 2>nul

:: Remove self
del /f /q "%~f0" 2>nul

echo Done! Only website files remain.

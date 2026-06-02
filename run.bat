@echo off
title LinkSnap - Build and Run
echo ============================================
echo  LinkSnap URL Shortener - Build and Run
echo ============================================
echo.

:: Check Java
echo [1/4] Checking Java...
java -version 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Java 17+ is required but not found.
    echo Please install from https://adoptium.net/
    pause
    exit /b 1
)

:: Check Maven
echo.
echo [2/4] Checking Maven...
set "MVN_CMD=mvn"
mvn --version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo Maven not found on PATH. Setting up local Maven Wrapper...
    if not exist "%~dp0.maven\apache-maven-3.9.6" (
        echo Downloading Apache Maven 3.9.6...
        powershell -Command "New-Item -ItemType Directory -Force -Path '%~dp0.maven' | Out-Null; Write-Host 'Downloading zip...'; Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip' -OutFile '%~dp0.maven\maven.zip'; Write-Host 'Extracting...'; Expand-Archive -Path '%~dp0.maven\maven.zip' -DestinationPath '%~dp0.maven'; Remove-Item '%~dp0.maven\maven.zip'"
    )
    set "MVN_CMD=%~dp0.maven\apache-maven-3.9.6\bin\mvn.cmd"
)

:: Verify Maven Command works
call "%MVN_CMD%" --version
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Failed to configure Maven.
    pause
    exit /b 1
)

:: Check Redis (optional, app starts in degraded mode without it)
echo.
echo [3/4] Checking Redis...
redis-cli ping >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo WARNING: Redis is not running. The app will start but caching will be disabled.
)

echo.
echo [4/4] Select database profile and start LinkSnap:
echo   [1] H2 In-Memory Database (No setup required, runs instantly - recommended)
echo   [2] Oracle SQL Database (Requires local Oracle XE running on port 1521)
echo.
set "DB_CHOICE=1"
set /p "DB_CHOICE=Enter choice [1 or 2, default is 1]: "

cd /d "%~dp0"

if "%DB_CHOICE%"=="2" (
    echo.
    echo Starting in Oracle SQL database mode...
    echo Open http://localhost:8080 in your browser once the app starts.
    echo.
    call "%MVN_CMD%" spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.cache.type=simple -Dspring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
) else (
    echo.
    echo Starting in H2 In-Memory database mode...
    echo Open http://localhost:8080 in your browser once the app starts.
    echo.
    call "%MVN_CMD%" spring-boot:run -Dspring-boot.run.profiles=dev -Dspring-boot.run.jvmArguments="-Dspring.cache.type=simple -Dspring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
)
pause

@echo off
echo ============================================
echo  LinkSnap URL Shortener - Build and Run
echo ============================================
echo.

REM Check Java
echo [1/4] Checking Java...
java -version 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Java 17+ is required but not found.
    echo Please install from https://adoptium.net/
    pause
    exit /b 1
)

REM Check Maven
echo.
echo [2/4] Checking Maven...
mvn --version 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Maven is not found on PATH.
    echo Please install from https://maven.apache.org/
    pause
    exit /b 1
)

REM Check Redis (optional, app starts in degraded mode without it)
echo.
echo [3/4] Checking Redis...
redis-cli ping 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo WARNING: Redis is not running. The app will start but caching will be disabled.
    echo To install Redis on Windows, use: choco install redis
)

echo.
echo [4/4] Building and starting LinkSnap...
echo Open http://localhost:8080 in your browser once the app starts.
echo Press Ctrl+C to stop the server.
echo.

cd /d "d:\Link shortner"
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.cache.type=none -Dspring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration" 2>&1
pause

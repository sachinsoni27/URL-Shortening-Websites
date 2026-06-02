@echo off
title URL-Shortening-Websites - Starting...
echo ============================================
echo  URL-Shortening-Websites - Dev Mode (H2)
echo ============================================
echo.
echo Starting Spring Boot with H2 database...
echo No Redis required.
echo.
echo Open http://localhost:8080 when ready.
echo.

set "MVN_CMD=mvn"
mvn --version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    set "MVN_CMD=%~dp0.maven\apache-maven-3.9.6\bin\mvn.cmd"
)

call "%MVN_CMD%" spring-boot:run ^
  -Dspring-boot.run.profiles=dev ^
  -Dspring-boot.run.jvmArguments="-Dspring.cache.type=simple -Dspring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"

pause

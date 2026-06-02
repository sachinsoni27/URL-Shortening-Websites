@echo off
title LinkSnap - Git Push Helper
echo ====================================================
echo  LinkSnap URL Shortener - Setup Git and Push to GitHub
echo ====================================================
echo.
echo This script will configure git, commit the files, 
echo and push them to your repository:
echo https://github.com/sachinsoni27/URL-Shortening-Websites
echo.
echo Press any key to start...
pause > nul

echo.
echo [1/6] Initializing git repository...
if not exist .git (
    git init
) else (
    echo Git repository already initialized.
)

echo.
echo [2/6] Configuring git credentials...
git config user.name "sachinsoni27"
git config user.email "sachinsoniofficial2003@gmail.com"
echo Configured:
echo   user.name  = sachinsoni27
echo   user.email = sachinsoniofficial2003@gmail.com

echo.
echo [3/6] Setting remote origin...
git remote remove origin >nul 2>&1
git remote add origin https://github.com/sachinsoni27/URL-Shortening-Websites.git
echo Remote origin set to: https://github.com/sachinsoni27/URL-Shortening-Websites.git

echo.
echo [4/6] Staging files...
git add .

echo.
echo [5/6] Committing files...
git commit -m "Initial commit - URL Shortener web application"

echo.
echo [6/6] Pushing to GitHub (main branch)...
git branch -M main
echo.
echo If this is your first time pushing, a GitHub authentication prompt may appear.
echo Please authenticate in the popup browser window to complete the push.
echo.
git push -u origin main

if %ERRORLEVEL% equ 0 (
    echo.
    echo ====================================================
    echo SUCCESS: Project successfully committed and pushed!
    echo ====================================================
) else (
    echo.
    echo ====================================================
    echo WARNING: Git push failed or was interrupted.
    echo Please make sure you are authenticated to GitHub and try running:
    echo   git push -u origin main
    echo ====================================================
)
echo.
pause

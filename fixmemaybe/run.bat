@echo off
REM ============================================
REM  Fix Me Maybe - double-click to start (Windows)
REM  Works even if the folder path has spaces.
REM ============================================
setlocal enabledelayedexpansion
cd /d "%~dp0"
where javac >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Java JDK not found. Install JDK 17 or newer and try again.
  pause
  exit /b
)
echo Compiling...
if exist out rmdir /s /q out
if exist sources.txt del sources.txt
for /r src %%f in (*.java) do (
  set "p=%%f"
  set "p=!p:\=/!"
  echo "!p!">>sources.txt
)
javac -encoding UTF-8 -d out @sources.txt
if errorlevel 1 (
  del sources.txt
  echo [ERROR] Compile failed. See the messages above.
  pause
  exit /b
)
del sources.txt
java -cp out fixmemaybe.Main
pause

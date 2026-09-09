@echo off
setlocal
where gradle >nul 2>&1
if %ERRORLEVEL% EQU 0 (
  gradle %*
  exit /b %ERRORLEVEL%
)
set ROOT_DIR=%~dp0
set CACHE_DIR=%USERPROFILE%\.gradle\wrapper\dists\tazieh-gradle-8.6
set GRADLE_HOME=%CACHE_DIR%\gradle-8.6
if exist "%GRADLE_HOME%\bin\gradle.bat" goto RUN
if not exist "%CACHE_DIR%" mkdir "%CACHE_DIR%"
set ZIP=%CACHE_DIR%\gradle-8.6-bin.zip
if not exist "%ZIP%" powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing -Uri 'https://services.gradle.org/distributions/gradle-8.6-bin.zip' -OutFile '%ZIP%'"
if exist "%CACHE_DIR%\gradle-8.6.tmp" rmdir /s /q "%CACHE_DIR%\gradle-8.6.tmp"
mkdir "%CACHE_DIR%\gradle-8.6.tmp"
powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -LiteralPath '%ZIP%' -DestinationPath '%CACHE_DIR%\gradle-8.6.tmp' -Force"
move "%CACHE_DIR%\gradle-8.6.tmp\gradle-8.6" "%GRADLE_HOME%" >nul
rmdir /s /q "%CACHE_DIR%\gradle-8.6.tmp"
:RUN
call "%GRADLE_HOME%\bin\gradle.bat" %*
endlocal

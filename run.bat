@echo off
setlocal
cd /d "%~dp0"

if not defined JAVA_HOME (
    echo JDK 11 atau lebih baru diperlukan. Atur JAVA_HOME ke folder instalasi JDK 11+.
    exit /b 1
)
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo JAVA_HOME tidak menunjuk ke instalasi JDK yang valid: %JAVA_HOME%
    exit /b 1
)
if not exist "%JAVA_HOME%\bin\javac.exe" (
    echo Instalasi yang ditunjuk JAVA_HOME harus berisi JDK, bukan hanya JRE.
    exit /b 1
)

for /f "tokens=3" %%V in ('"%JAVA_HOME%\bin\java.exe" -version 2^>^&1 ^| findstr /I "version"') do set "JAVA_VERSION=%%~V"
for /f "tokens=1 delims=." %%M in ("%JAVA_VERSION%") do set "JAVA_MAJOR=%%M"
if not defined JAVA_MAJOR (
    echo Versi Java tidak dapat dibaca dari JAVA_HOME.
    exit /b 1
)
if %JAVA_MAJOR% LSS 11 (
    echo Project ini memerlukan JDK 11 atau lebih baru. Versi aktif: %JAVA_VERSION%
    exit /b 1
)

set "MVN="
if defined MAVEN_HOME if exist "%MAVEN_HOME%\bin\mvn.cmd" set "MVN=%MAVEN_HOME%\bin\mvn.cmd"
if not defined MVN for /f "delims=" %%M in ('where mvn.cmd 2^>nul') do if not defined MVN set "MVN=%%M"
if not defined MVN for /d %%N in ("%ProgramFiles%\NetBeans-*") do if exist "%%~N\netbeans\java\maven\bin\mvn.cmd" set "MVN=%%~N\netbeans\java\maven\bin\mvn.cmd"
if not defined MVN (
    echo Maven tidak ditemukan. Buka project ini di NetBeans atau pasang Maven 3.8+.
    exit /b 1
)

call "%MVN%" -q compile exec:java
exit /b %errorlevel%

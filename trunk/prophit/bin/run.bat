@echo off

if not "%OS%"=="Windows_NT" goto win9xStart
:winNTStart
@setlocal

rem Need to check if we are using the 4NT shell...
if not "%FOURNT_SHELL%" == "" goto setup4NT

set SCRIPT_HOME=%~dp0

rem On NT/2K grab all arguments at once
set CMD_LINE_ARGS=%*
goto doneStart

:setup4NT
set CMD_LINE_ARGS=%$
goto doneStart

:win9xStart
rem Slurp the command line arguments.  This loop allows for an unlimited number of 
rem agruments (up to the command line limit, anyway).

set CMD_LINE_ARGS=

:setupArgs
if %1a==a goto doneStart
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto setupArgs

:doneStart
rem This label provides a place for the argument list loop to break out 
rem and for NT handling to skip to.

if exist "%SCRIPT_HOME%\lib" goto setupClasspath

SET SCRIPT_HOME=.
if exist "%SCRIPT_HOME%\lib" goto setupClasspath

:setupClasspath

echo %SCRIPT_HOME%

set LOCALCLASSPATH="%SCRIPT_HOME%\lib\orbit.jar";"%SCRIPT_HOME%\lib\data.jar";"%SCRIPT_HOME%\lib\log4jME.jar";"%SCRIPT_HOME%\lib\gl4java-glutfonts.jar";"%SCRIPT_HOME%\lib\gl4java-win32.jar";"%SCRIPT_HOME%\lib\gl4java.jar";"%SCRIPT_HOME%\lib\png.jar";"%SCRIPT_HOME%\lib\jnlp.jar"

if "%JAVA_HOME%" == "" goto noJavaHome
if "%_JAVACMD%" == "" set _JAVACMD=%JAVA_HOME%\jre\bin\java
set BACKUPPATH=%PATH%
set PATH=%JAVA_HOME%\jre\bin;"%SCRIPT_HOME%\bin";%PATH%

goto run

:noJavaHome
if "%_JAVACMD%" == "" set _JAVACMD=java
echo.
echo Warning: JAVA_HOME environment variable is not set.
echo   If application launch fails because libraries could not be found
echo   you will need to set the JAVA_HOME environment variable
echo   to the installation directory of java.
echo.

:run
@echo on
"%_JAVACMD%" %ORBIT_OPTS% -classpath "%LOCALCLASSPATH%" orbit.gui.MapFrame %CMD_LINE_ARGS%
@echo off
goto end

:end
set LOCALCLASSPATH=
set _JAVACMD=
set CMD_LINE_ARGS=
set PATH=%BACKUPPATH%

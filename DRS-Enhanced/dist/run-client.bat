@echo off
rem Starts the JavaFX client. Set JAVAFX_HOME to an unzipped JavaFX 21 SDK (https://openjfx.io).
cd /d "%~dp0"
if "%JAVAFX_HOME%"=="" (echo Set JAVAFX_HOME to your JavaFX SDK folder & exit /b 1)
java --module-path "%JAVAFX_HOME%\lib" --add-modules javafx.controls,javafx.fxml -jar drs-enhanced-client.jar

@echo off
rem Starts the DRS-Enhanced multi-threaded server. Requires Java 21+ and MySQL 8 running.
cd /d "%~dp0"
java -jar drs-enhanced-server.jar %*

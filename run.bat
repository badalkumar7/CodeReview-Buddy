@echo off
setlocal
pushd "%~dp0"
call "%~dp0tools\apache-maven-3.9.9\bin\mvn.cmd" -Dmaven.repo.local=.m2 javafx:run
set "exitCode=%ERRORLEVEL%"
popd
exit /b %exitCode%

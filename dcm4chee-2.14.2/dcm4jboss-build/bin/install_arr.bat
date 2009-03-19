@echo off
rem -----------------------------------------------------------------------------------
rem copy DCM4CHEE Audit Record Repository components into DCM4CHEE Archive installation
rem -----------------------------------------------------------------------------------

if "%OS%" == "Windows_NT"  setlocal
set DIRNAME=.\
if "%OS%" == "Windows_NT" set DIRNAME=%~dp0%

set DCM4CHEE_HOME=%DIRNAME%..
set DCM4CHEE_SERV=%DCM4CHEE_HOME%\server\default

if exist "%DCM4CHEE_SERV%" goto found_dcm4chee
echo Could not locate %DCM4CHEE_SERV%. Please check that you are in the
echo bin directory when running this script.
goto end

:found_dcm4chee
if not [%1] == [] goto found_arg1
echo "Usage: install_arr <path-to-dcm4chee-arr-installation-directory>"
goto end

:found_arg1
set ARR_HOME=%1
set ARR_SERV=%ARR_HOME%\server\default

if exist "%ARR_SERV%\deploy\dcm4chee-arr-db2-3.0.7.ear" set ARR_DB=db2
if exist "%ARR_SERV%\deploy\dcm4chee-arr-firebird-3.0.7.ear" set ARR_DB=firebird
if exist "%ARR_SERV%\deploy\dcm4chee-arr-hsql-3.0.7.ear" set ARR_DB=hsql
if exist "%ARR_SERV%\deploy\dcm4chee-arr-mssql-3.0.7.ear" set ARR_DB=mssql
if exist "%ARR_SERV%\deploy\dcm4chee-arr-mysql-3.0.7.ear" set ARR_DB=mysql
if exist "%ARR_SERV%\deploy\dcm4chee-arr-oracle-3.0.7.ear" set ARR_DB=oracle
if exist "%ARR_SERV%\deploy\dcm4chee-arr-psql-3.0.7.ear" set ARR_DB=psql
if not [%ARR_DB%] == [] goto found_arr
echo Could not locate dcm4chee-arr in %ARR_HOME%.
goto end

:found_arr
copy "%ARR_SERV%\conf\dcm4chee-auditlog\arr-udplistener-xmbean.xml" "%DCM4CHEE_SERV%\conf\dcm4chee-auditlog"

copy "%ARR_SERV%\deploy\dcm4chee-arr-%ARR_DB%-3.0.7.ear" "%DCM4CHEE_SERV%\deploy"
copy "%ARR_SERV%\deploy\arr-%ARR_DB%-ds.xml" "%DCM4CHEE_SERV%\deploy"
copy "%DCM4CHEE_HOME%\doc\dcm4chee-auditlog-service.xml" "%DCM4CHEE_SERV%\deploy"

copy "%ARR_SERV%\lib\dcm4che-core-2.0.17.jar" "%DCM4CHEE_SERV%\lib"

:end
if "%OS%" == "Windows_NT" endlocal

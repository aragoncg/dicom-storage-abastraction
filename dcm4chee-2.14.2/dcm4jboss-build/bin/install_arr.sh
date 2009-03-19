#!/bin/sh
# -----------------------------------------------------------------------------------
# copy DCM4CHEE Audit Record Repository components into DCM4CHEE Archive installation
# -----------------------------------------------------------------------------------

DIRNAME=`dirname $0`
DCM4CHEE_HOME="$DIRNAME"/..
DCM4CHEE_SERV="$DCM4CHEE_HOME"/server/default

if [ x$1 = x ]; then
  echo "Usage: $0 <path-to-dcm4chee-arr-installation-directory>"
  exit 1
fi

ARR_HOME="$1"
ARR_SERV="$ARR_HOME"/server/default

if [ -f "$ARR_SERV"/deploy/dcm4chee-arr-db2-3.0.7.ear ]; then
  ARR_DB=db2
elif [ -f "$ARR_SERV"/deploy/dcm4chee-arr-firebird-3.0.7.ear ]; then
  ARR_DB=firebird
elif [ -f "$ARR_SERV"/deploy/dcm4chee-arr-hsql-3.0.7.ear ]; then
  ARR_DB=hsql
elif [ -f "$ARR_SERV"/deploy/dcm4chee-arr-mssql-3.0.7.ear ]; then
  ARR_DB=mssql
elif [ -f "$ARR_SERV"/deploy/dcm4chee-arr-mysql-3.0.7.ear ]; then
  ARR_DB=mysql
elif [ -f "$ARR_SERV"/deploy/dcm4chee-arr-oracle-3.0.7.ear ]; then
  ARR_DB=oracle
elif [ -f "$ARR_SERV"/deploy/dcm4chee-arr-psql-3.0.7.ear ]; then
  ARR_DB=psql
else
  echo Could not locate dcm4chee-arr in "$ARR_HOME"
  exit 1
fi

cp -v "$ARR_SERV"/conf/dcm4chee-auditlog/arr-udplistener-xmbean.xml \
  "$DCM4CHEE_SERV"/conf/dcm4chee-auditlog

cp -v "$ARR_SERV"/deploy/dcm4chee-arr-${ARR_DB}-3.0.7.ear \
  "$ARR_SERV"/deploy/arr-${ARR_DB}-ds.xml \
  "$DCM4CHEE_HOME"/doc/dcm4chee-auditlog-service.xml \
  "$DCM4CHEE_SERV"/deploy

cp -v "$ARR_SERV"/lib/dcm4che-core-2.0.17.jar \
  "$DCM4CHEE_SERV"/lib

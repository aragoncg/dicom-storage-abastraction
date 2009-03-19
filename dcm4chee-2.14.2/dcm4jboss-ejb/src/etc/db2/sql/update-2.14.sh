#!/bin/bash
# Update DB from dcm4chee-2.13.x to dcm4chee-2.14.x

db2 CONNECT TO pacsdb
db2 SET SCHEMA pacsdb

db2 -stf update-2.14.db2sql

tables="ae code patient other_pid study study_permission mpps series \
  series_req media instance verify_observer filesystem files study_on_fs \
  mwl_item gpsps gpsps_perf gpsps_req gppps hp hpdef priv_patient priv_study \
  priv_series priv_instance priv_file device"


for t in $tables ; 
do
  pk_start=`db2 -x "SELECT MAX(pk)+1 FROM $t"`
  if [ $pk_start != "-" ]; then
    db2 -v "ALTER TABLE $t ALTER COLUMN pk RESTART WITH $pk_start";
  fi
done

db2 TERMINATE

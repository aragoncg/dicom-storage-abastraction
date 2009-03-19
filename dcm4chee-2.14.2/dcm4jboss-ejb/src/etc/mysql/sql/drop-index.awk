#!/usr/bin/awk -f
/^CREATE INDEX/ {
	 split($5,T,"(");
	 print "ALTER TABLE " T[1] " DROP INDEX " $3 ";"
}
/^CREATE UNIQUE INDEX/ {
	 split($6,T,"(");
	 print "ALTER TABLE " T[1] " DROP INDEX " $4 ";"
}

BUILD INSTRUCTIONS FOR DCM4CHEE ARCHIVE:
========================================

In order to build all components of the dcm4chee archive application you have
to download and install:

- Ant 1.7.0 [http://ant.apache.org]

- XDoclet 1.2.3 [http://xdoclet.sourceforge.net]

- JBoss Application Server 4.2.3.GA [http://labs.jboss.com/jbossas/downloads]

- Apache FOP 0.20.5 [http://xmlgraphics.apache.org/fop/]

- Jakarta Cactus 1.8.0 [http://jakarta.apache.org/cactus/]
(only needed to build test suite for dcm4jboss-ejb - not need to build
application)

dcm4chee-2.14.2 archive also relies on

- dcm4che-1.4.21 : DICOM Library

which binary and source distribution package is also available at
http://sourceforge.net/projects/dcm4che. The binary package is sufficient to
build dcm4chee archive. If you download the source package, you have to build
all components of dcm4che-1.4.21 (ant release) before building dcm4chee-2.14.2.

dcm4chee-2.14.2 archive also make use of

- dcm4che-audit-logger : JMX Service for emitting DICOM Supp95 Audit Trail
  Messages, and
- dcm4che-audit-login : JAAS login module emitting User Authentication message

which sources are not included by dcm4chee-2.14.2-src.zip, but provided in a
separate ZIP archive: dcm4chee-audit-2.14.2-src.zip at
http://sourceforge.net/projects/dcm4che. Follow the build instructions include
in dcm4chee-audit, to build and install these artifacts in your local Maven
Repository (default: ~/.m2/repository) for usage by following build task for 
dcm4chee-2.14.2 archive.

dcm4chee-2.14.2 archive also make use of

- dcm4chee-docstore : Generic Document Storage (used by RID - Retrieve Information for Display) 

which sources are not included by dcm4chee-2.14.2-src.zip, but provided in a
separate ZIP archive: dcm4chee-docstore-0.0.2-src.zip at
http://sourceforge.net/projects/dcm4che. Follow the build instructions include
in dcm4chee-docstore, to build and install these artifacts in your local Maven
Repository (default: ~/.m2/repository) for usage by following build task for 
dcm4chee-2.14.2 archive.

dcm4chee-2.14.2 archive has compile dependency to

- dcm4chee-xds-infoset : XDS Infoset classes (XDS Query for Consumer and Document Lifecycle)

which sources are not included by dcm4chee-2.14.2-src.zip, but provided in a
separate ZIP archive: dcm4chee-xds-infoset-0.0.2-src.zip at
http://sourceforge.net/projects/dcm4che. Follow the build instructions include
in dcm4chee-xds-infoset, to build and install these artifacts in your local Maven
Repository (default: ~/.m2/repository) for usage by following build task for 
dcm4chee-2.14.2 archive.

Create an environment variable pointing to the Ant home directory (ANT_HOME)
and add the $ANT_HOME/bin directory to your PATH.

The build scripts (dcm4jboss-all/dcm4jboss-xxx/build.xml) assumes following
locations of dcm4che-1.4.18, XDoclet, JBoss, Apache FOP, Jakarta Cactus and
your local Maven2 Repository:

dcm4che14.home=${user.home}/dcm4che14/build/dcm4che-1.4.21
xdoclet.home=${user.home}/xdoclet-1.2.3
jboss.home=${user.home}/jboss-4.2.3.GA
fop.home=${user.home}/fop-0.20.5
cactus.home=${user.home}/jakarta-cactus-13-1.7
m2.repos=${user.home}/.m2/repository.

Instead of modifying the build scripts directly, you may adjust these values
to your build environment by renaming file 

dcm4jboss-all/build.properties.default
to
dcm4jboss-all/build.properties

and adjust these values there.

Change the working directory:
>cd dcm4jboss-all/dcm4jboss-build

and invoke:
>ant dist

to build all components and pack them in binary distribution packages:

target/dcm4chee-db2-<version>.zip
target/dcm4chee-hsql-<version>.zip
target/dcm4chee-mssql-<version>.zip
target/dcm4chee-mysql-<version>.zip
target/dcm4chee-oracle-<version>.zip
target/dcm4chee-psql-<version>.zip
.

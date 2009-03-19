Put style sheets defining attribute coercion of incomming/outgoing DICOM
messages (DIMSE) in this directory:

cstorerq.xsl     - coerce Data Set of received C-STORE-RQ
cfindrq.xsl      - coerce Identifier of received Query C-FIND-RQ
cfindrsp.xsl     - coerce Identifier of returned Query C-FIND-RSP
mwl-cfindrq.xsl  - coerce Identifier of received Modality Worklist C-FIND-RQ
mwl-cfindrsp.xsl - coerce Identifier of returned Modality Worklist C-FIND-RSP

General attribute coercion configuration in the base directory can be
overwritten by AE specific coercion configuration, located in sub-directories
matching the Calling AE Title. E.g.:

MOD_AET/mwl-cfindrq.xsl

<?xml version="1.0" encoding="UTF-8"?>
<!-- 
Sample cstorerq2mwl-cfindrq.xsl, using Study Instance UID (0020,000D) and
Scheduled Procedure Step ID (0040,0009) in Request Attributes Sequence
(0040,0275) as matching key and dcm4chee proprietary value 'ANY' for 
Scheduled Procedure Step Status (0040,0020) to match also worklist entries,
for which MPPS N-CREATE was already received. 
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="xml" indent="no"/>
  <!-- overwritten by application with actual values -->
  <xsl:param name="calling" select="'SAMPLE_MOD'"/>
  <xsl:param name="called" select="'DCM4CHEE'"/>
  <xsl:param name="date" select="'20051206'"/>
  <xsl:param name="time" select="'115600.000'"/>
  <xsl:template match="/dataset">
    <dataset>
      <!-- Accession Number -->
      <attr tag="00080050" vr="SH"/>
      <!-- Admitting Diagnoses Description -->
      <attr tag="00081080" vr="LO"/>
      <!-- Referring Physican Name -->
      <attr tag="00080090" vr="PN"/>
      <!-- Patient Name -->
      <attr tag="00100010" vr="PN"/>
      <!-- Patient ID -->
      <attr tag="00100020" vr="LO"/>
      <!-- Issuer Of Patient ID -->
      <attr tag="00100021" vr="LO"/>
      <!-- Patient Birthdate -->
      <attr tag="00100030" vr="DA"/>
      <!-- Patient Sex -->
      <attr tag="00100040" vr="CS"/>
      <!-- Study Instance UID -->
      <xsl:copy-of select="attr[@tag='0020000D']"/>
      <!-- Requesting Physican -->
      <attr tag="00321032" vr="PN"/>
      <!-- Requesting Service -->
      <attr tag="00321033" vr="LO"/>
      <!-- Requested Procedure Description -->
      <attr tag="00321060" vr="LO"/>
      <!-- Requested Procedure Code Sequence -->
      <attr tag="00321064" vr="SQ"/>
      <!-- Scheduled Procedure Step Sequence -->
      <attr tag="00400100" vr="SQ">
        <item>
          <!-- Scheduled Procedure Step Description -->
          <attr tag="00400007" vr="LO"/>
          <!-- Scheduled Protocol Code Sequence -->
          <attr tag="00400008" vr="SQ"/>
          <!-- Scheduled Procedure Step ID -->
          <xsl:copy-of select="attr[@tag='00400275']/item/attr[@tag='00400009']"/>
        </item>
      </attr>
      <!-- Requested Procedure ID -->
      <attr tag="00401001" vr="SH"/>
      <!-- Reason for the Requested Procedure -->
      <attr tag="00401002" vr="LO"/>
    </dataset>
  </xsl:template>
</xsl:stylesheet>

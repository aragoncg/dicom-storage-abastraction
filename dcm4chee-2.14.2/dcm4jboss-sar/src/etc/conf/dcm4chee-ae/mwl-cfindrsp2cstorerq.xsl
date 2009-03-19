<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="xml" indent="no"/>
  <xsl:template match="/dataset">
    <dataset>
      <!-- Accession Number -->
      <xsl:copy-of select="attr[@tag='00080050']"/>
      <!-- Admitting Diagnoses Description -->
      <attr tag="00081080" vr="LO"/>
      <xsl:copy-of select="attr[@tag='00081080']"/>      
      <!-- Referring Physican Name -->
      <xsl:copy-of select="attr[@tag='00080090']"/>
      <!-- Patient Name -->
      <xsl:copy-of select="attr[@tag='00100010']"/>
      <!-- Patient ID -->
      <xsl:copy-of select="attr[@tag='00100020']"/>
      <!-- Issuer Of Patient ID -->
      <xsl:copy-of select="attr[@tag='00100021']"/>
      <!-- Patient Birthdate -->
      <xsl:copy-of select="attr[@tag='00100030']"/>
      <!-- Patient Sex -->
      <xsl:copy-of select="attr[@tag='00100040']"/>
      <!-- (0040,0275) SQ #-1 Request Attributes Sequence -->
      <attr tag="00400275" vr="SQ">
        <item>
          <!-- Study Instance UID -->
          <xsl:copy-of select="attr[@tag='0020000D']"/>
          <!-- Requesting Physican -->
          <xsl:copy-of select="attr[@tag='00321032']"/>
          <!-- Requesting Service -->
          <xsl:copy-of select="attr[@tag='00321033']"/>
          <!-- Requested Procedure Description -->
          <xsl:copy-of select="attr[@tag='00321060']"/>
          <!-- Requested Procedure Code Sequence -->
          <xsl:copy-of select="attr[@tag='00321064']"/>
          <!-- Requested Procedure ID -->
          <xsl:copy-of select="attr[@tag='00401001']"/>
          <!-- Reason for the Requested Procedure -->
          <xsl:copy-of select="attr[@tag='00401002']"/>
          <xsl:apply-templates select="attr[@tag='00400100']/item"/>
        </item>
      </attr>
    </dataset>
  </xsl:template>
  <xsl:template match="item">
    <!-- Scheduled Procedure Step Description -->
    <xsl:copy-of select="attr[@tag='00400007']"/>
    <!-- Scheduled Protocol Code Sequence -->
    <xsl:copy-of select="attr[@tag='00400008']"/>
    <!-- Scheduled Procedure Step ID -->
    <xsl:copy-of select="attr[@tag='00400009']"/>
  </xsl:template>
</xsl:stylesheet>

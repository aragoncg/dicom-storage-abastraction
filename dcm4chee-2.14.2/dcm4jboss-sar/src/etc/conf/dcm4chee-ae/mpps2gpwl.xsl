<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="xml" indent="yes"/>
  <xsl:param name="date">20070319</xsl:param>
  <xsl:param name="time">093000.000</xsl:param>
  <xsl:template match="/dataset">
    <dataset>
      <!-- Private Worklist Item Sequence (0043,0020) -->
      <attr tag="00430020" vr="SQ">
        <xsl:if test="normalize-space(attr[@tag=00400252])='COMPLETED'">
          <xsl:call-template name="wkitem"/>
        </xsl:if>
      </attr>
    </dataset>
  </xsl:template>
  <xsl:template name="wkitem">
    <item>
      <!-- Specific Character Set -->
      <xsl:copy-of select="attr[@tag='00080005']"/>
      <!-- SOP Class UID -->
      <attr tag="00080016" vr="UI">1.2.840.10008.5.1.4.32.2</attr>
      <!-- SOP Instance UID (0008,0018) will be created by the application -->

      <!-- Referenced Performed Procedure Step Sequence -->
      <attr tag="00081111" vr="SQ">
        <item>
          <!-- Referenced SOP Class UID -->
          <attr tag="00081150" vr="UI">
            <xsl:value-of select="attr[@tag='00080016']"/>
          </attr>
          <!-- Referenced SOP Instance UID -->
          <attr tag="00081155" vr="UI">
            <xsl:value-of select="attr[@tag='00080018']"/>
          </attr>
        </item>
      </attr>
      
      <!-- Patient's Name (0010,0010) will be supplemented from Patient Record in DB-->
      <!-- Patient ID -->
      <xsl:copy-of select="attr[@tag='00100020']"/>
      <!-- Issuer of Patient ID -->
      <xsl:copy-of select="attr[@tag='00100021']"/>
      <!-- Patient's Birth Date (0010, 0030) will be supplemented from Patient Record in DB-->
      <!-- Patient's Sex (0010, 0040) will be supplemented from Patient Record in DB-->

      <!-- General Purpose Scheduled Procedure Step Status -->
      <attr tag="00404001" vr="CS">SCHEDULED</attr>
      <!-- General Purpose Scheduled Procedure Step Priority -->
      <attr tag="00404003" vr="CS">MEDIUM</attr>
      <!-- Scheduled Procedure Step Start Date and Time -->
      <attr tag="00404005" vr="DT">
        <xsl:value-of select="$date"/>
        <xsl:value-of select="$time"/>
      </attr>
      <!-- Multiple Copies Flag -->
      <attr tag="00404006" vr="CS">N</attr>
      <!-- Scheduled Procedure Step ID (0040,4009) will be created by the application -->
      <!-- Scheduled Workitem Code Sequence -->
      <attr tag="00404018" vr="SQ">
        <item>
          <!-- Code Value -->
          <attr tag="00080100" vr="SH">110005</attr>
          <!-- Coding Scheme Designator -->
          <attr tag="00080102" vr="SH">DCM</attr>
          <!-- Code Meaning -->
          <attr tag="00080104" vr="LO">Interpretation</attr>
        </item>
      </attr>
      <!-- Input Availability Flag -->
      <attr tag="00404020" vr="CS">COMPLETE</attr>
      <!-- Input Information Sequence -->
      <attr tag="00404021" vr="SQ">
        <item>
          <!-- Study Instance UID -->
          <xsl:copy-of select="attr[@tag='00400270']/item/attr[@tag='0020000D']"/>
          <!-- >Referenced Series Sequence (0008,1115) -->
          <attr tag="00081115" vr="SQ">
            <xsl:apply-templates select="attr[@tag='00400340']/item" mode="refseries"/>
          </attr>
        </item>
      </attr>
      <!-- Scheduled Station Class Code Sequence -->
      <attr tag="00404026" vr="SQ"/>
      <!-- Scheduled Station Geographic Location Code Sequence -->
      <attr tag="00404027" vr="SQ"/>
      <!-- Scheduled Human Performers Sequence -->
      <attr tag="00404034" vr="SQ"/>
      <!-- Referenced Request Sequence -->
      <attr tag="0040A370" vr="SQ">
        <xsl:apply-templates
          select="attr[@tag='00400270']/item[string(attr[@tag='00401001'])]"
          mode="request"/>
      </attr>
    </item>
  </xsl:template>
  <xsl:template match="item" mode="request">
    <xsl:variable name="rpid">
      <xsl:value-of select="string(attr[@tag='00401001'])"/>
    </xsl:variable>
    <xsl:if test="not(preceding-sibling::*[attr[@tag=00401001]=$rpid])">
      <item>
        <!-- Accession Number -->
        <xsl:copy-of select="attr[@tag='00080050']"/>
        <!-- Referenced Study Sequence -->
        <xsl:copy-of select="attr[@tag='00081110']"/>
        <!-- Study Instance UID -->
        <xsl:copy-of select="attr[@tag='0020000D']"/>
        <!-- Requesting Physician -->
        <xsl:copy-of select="attr[@tag='00321032']"/>
        <!-- Requesting Service -->
        <xsl:copy-of select="attr[@tag='00321033']"/>
        <!-- Requested Procedure Description -->
        <xsl:copy-of select="attr[@tag='00321060']"/>
        <!-- Requested Procedure Code Sequence -->
        <xsl:copy-of select="attr[@tag='00321064']"/>
        <!-- Requested Procedure ID -->
        <xsl:copy-of select="attr[@tag='00401001']"/>
        <!-- Placer Order Number/Imaging Service Request -->
        <xsl:copy-of select="attr[@tag='00402016']"/>
        <!-- Filler Order Number/Imaging Service Request -->
        <xsl:copy-of select="attr[@tag='00402017']"/>
      </item>
    </xsl:if>
  </xsl:template>
  <xsl:template match="item" mode="refseries">
    <item>
      <!-- Retrieve AE Title -->
      <xsl:copy-of select="attr[@tag='00080054']"/>
      <!-- Referenced SOP Sequence -->
      <attr tag="00081199" vr="SQ">
        <xsl:apply-templates select="attr[@tag='00081140']/item" mode="refsop"/>
        <xsl:apply-templates select="attr[@tag='00400220']/item" mode="refsop"/>
      </attr>
      <!-- Series Instance UID -->
      <xsl:copy-of select="attr[@tag='0020000E']"/>
    </item>
  </xsl:template>
  <xsl:template match="item" mode="refsop">
    <item>
      <!-- Referenced SOP Class UID -->
      <xsl:copy-of select="attr[@tag='00081150']"/>
      <!-- Referenced SOP Instance UID -->
      <xsl:copy-of select="attr[@tag='00081155']"/>
    </item>
  </xsl:template>
</xsl:stylesheet>

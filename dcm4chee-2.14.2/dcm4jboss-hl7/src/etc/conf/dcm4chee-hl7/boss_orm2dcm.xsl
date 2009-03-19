<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="xml" indent="yes"/>
  <xsl:include href="common.xsl"/>
  <!--  root -->
  <xsl:template match="/hl7">
    <dataset>
      <attr tag="00080005" vr="CS">ISO_IR 100</attr>
      <xsl:apply-templates select="PID"/>
      <xsl:apply-templates select="PV1"/>
      <xsl:apply-templates select="AL1"/>
      <xsl:apply-templates select="ORC[1]"/>
      <xsl:apply-templates select="OBR[1]"/>
      <!-- Scheduled Procedure Step Sequence -->
      <attr tag="00400100" vr="SQ">
        <xsl:apply-templates select="ORC" mode="sps"/>
      </attr>
    </dataset>
  </xsl:template>
  <!-- PV1 -->
  <xsl:template match="PV1">
    <!-- HL7:Assigned Patient Location.1 -> DICOM:Requesting Service -->
    <xsl:call-template name="attr">
      <xsl:with-param name="tag" select="'00321033'"/>
      <xsl:with-param name="vr" select="'LO'"/>
      <xsl:with-param name="val" select="substring(field[3]/text(),1,64)"/>
    </xsl:call-template>
    <!-- HL7:Referring Doctor -> DICOM:Referring Physican Name --> 
    <xsl:call-template name="cn2pnAttr">
    <xsl:with-param name="tag" select="'00080090'"/>
    <xsl:with-param name="cn" select="field[7]"/>
    </xsl:call-template> 

    <!-- HL7:Ordering Provider -> DICOM Requesting Physician -->
    <xsl:call-template name="cn2pnAttr">
      <xsl:with-param name="tag" select="'00321032'"/>
      <xsl:with-param name="cn" select="field[8]"/>
    </xsl:call-template>

    <!-- HL7:Visit Number -> DICOM:Admission ID + Issuer -->
    <xsl:call-template name="cx2attrs">
      <xsl:with-param name="idtag" select="'00380010'"/>
      <xsl:with-param name="istag" select="'00380011'"/>
      <xsl:with-param name="cx" select="field[19]"/>
    </xsl:call-template>
  </xsl:template>
  <!-- AL1 -->
  <xsl:template match="AL1">
    <!-- HL7:Allergy Code/Mnemonic/Description -> Medical Alerts + Contrast Allergies -->
    <xsl:variable name="al1_3" select="string(field[3]/text())"/>
    <xsl:choose>
      <xsl:when test="$al1_3 = '&quot;&quot;'">
        <attr tag="00102000" vr="LO"/>
        <attr tag="00102110" vr="LO"/>
      </xsl:when>
      <xsl:when test="$al1_3">
        <attr tag="00102000" vr="LO">
          <xsl:value-of select="substring(substring-after($al1_3,'$'),1,64)"/>
        </attr>
        <attr tag="00102110" vr="LO">
          <xsl:value-of select="substring(substring-before($al1_3,'$'),1,64)"/>
        </attr>
      </xsl:when>
    </xsl:choose>
  </xsl:template>
  <!-- ORC[1] -->
  <xsl:template match="ORC[1]">
    <!-- HL7:Quantity/Timing -> DICOM:Requested Procedure Priority -->
    <xsl:call-template name="procedurePriority">
      <xsl:with-param name="priority" select="string(field[7]/component[5]/text())"/>
    </xsl:call-template>
  </xsl:template>
  <xsl:template name="procedurePriority">
    <xsl:param name="priority"/>
    <xsl:if test="normalize-space($priority)">
      <attr tag="00401003" vr="CS">
        <xsl:choose>
          <xsl:when test="$priority = 'S'">STAT</xsl:when>
          <xsl:when test="$priority = 'A' or $priority = 'P' or $priority = 'C' ">HIGH</xsl:when>
          <xsl:when test="$priority = 'R'">ROUTINE</xsl:when>
          <xsl:when test="$priority = 'T'">MEDIUM</xsl:when>
        </xsl:choose>
      </attr>
    </xsl:if>
  </xsl:template>
  <!--  OBR[1] -->
  <xsl:template match="OBR[1]">
    <xsl:variable name="ordno" select="string(field[3]/text())"/>
    <!-- HL7:Filler Order Number -> DICOM:Placer Order Number -->
    <attr tag="00402016" vr="LO">
      <xsl:value-of select="$ordno"/>
    </attr>
    <!-- HL7:Filler Order Number -> DICOM:Filler Order Number -->
    <attr tag="00402017" vr="LO">
      <xsl:value-of select="$ordno"/>
    </attr>
    <!-- HL7:Filler Order Number -> DICOM:Accession Number -->
    <attr tag="00080050" vr="SH">
      <xsl:value-of select="$ordno"/>
    </attr>
     <!-- Study Instance UID -> DICOM:Scheduled Procedure Step ID -->
    <attr tag="00400009" vr="SH">
      <xsl:value-of select="$ordno"/>
    </attr>
    <!--  HL7:Danger Code -> DICOM:Patient State -->
    <xsl:call-template name="attr">
      <xsl:with-param name="tag" select="'00380500'"/>
      <xsl:with-param name="vr" select="'LO'"/>
      <xsl:with-param name="val" select="substring(field[12]/text(),1,64)"/>
    </xsl:call-template>
    <!--  HL7:Relevant Clinical Info -> DICOM:Reason for the Requested Procedure + Admitting Diagnoses Description -->
    <xsl:variable name="obr13" select="normalize-space(field[13])"/>
    <xsl:choose>
      <xsl:when test="$obr13 = '&quot;&quot;'">
        <attr tag="00081080" vr="LO"/>
        <attr tag="00401002" vr="LO"/>
      </xsl:when>
      <xsl:when test="$obr13">
        <attr tag="00081080" vr="LO">
          <xsl:value-of select="substring(substring-after(substring-after(substring-after($obr13,'$'),'$'),'$'),1,64)"/>
        </attr>
        <attr tag="00401002" vr="LO">
          <xsl:value-of select="substring(substring-before(substring-after($obr13,'$'),'$'),1,64)"/>
        </attr>
      </xsl:when>
    </xsl:choose>
    <!-- HL7:Transportation Mode  -> DICOM:Patient Transport Arrangements -->
    <xsl:call-template name="attr">
      <xsl:with-param name="tag" select="'00401004'"/>
      <xsl:with-param name="vr" select="'LO'"/>
      <xsl:with-param name="val" select="substring(field[30]/text(),1,64)"/>
    </xsl:call-template>
  </xsl:template>
  <!-- ORC - sps -->
  <xsl:template match="ORC" mode="sps">
    <item>
      <xsl:apply-templates select="following-sibling::OBR[1]" mode="sps"/> 
    </item>
  </xsl:template>
  <!-- OBR - sps  -->
  <xsl:template match="OBR" mode="sps">
    <!-- HL7:Results Rpt/Status Chng - Date/Time -> DICOM:Scheduled Procedure Step Start Date/Time -->
    <xsl:variable name="obr36" select="normalize-space(field[36]/text())"/>
    <xsl:variable name="dt">
      <xsl:choose>
        <xsl:when test="$obr36">
          <xsl:value-of select="$obr36"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="normalize-space(field[22]/text())"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:if test="string-length($dt) >= 8">
      <xsl:call-template name="attrDATM">
        <xsl:with-param name="datag" select="'00400002'"/>
        <xsl:with-param name="tmtag" select="'00400003'"/>
        <xsl:with-param name="val">
          <xsl:value-of select="$dt"/>
          <xsl:if test="string-length($dt) &lt; 10">00</xsl:if>
        </xsl:with-param>
      </xsl:call-template>
    </xsl:if>
    <!-- HL7:Filler Order Number -> DICOM:Study Instance UID -->
    <xsl:variable name="accnr" select="string(field[3]/text())"/>
    <attr tag="0020000D" vr="UI">
      <xsl:value-of select="'1.2.40.0.13.0.11.257300.4.1'"/>
      <xsl:value-of select="$accnr"/>
      <xsl:value-of select="'.54321.'"/>
      <xsl:value-of select="$obr36"/>
      <xsl:value-of select="'00'"/>
</attr>
 <!-- HL7:Entering Device -> DICOM:Scheduled Station Name -->
      <xsl:call-template name="attr">
        <xsl:with-param name="tag">00400010</xsl:with-param>
        <xsl:with-param name="vr">SH</xsl:with-param>
        <xsl:with-param name="val" select="substring(field[19]/text(),1,16)"/>
      </xsl:call-template>
      <!-- HL7:Entering Device -> DICOM:Modality -->
      <xsl:call-template name="attr">
        <xsl:with-param name="tag">00080060</xsl:with-param>
        <xsl:with-param name="vr">CS</xsl:with-param>
        <xsl:with-param name="val" select="substring(field[19]/component/text(),1,16)"/>
      </xsl:call-template>


    <!-- Study Instance UID -> DICOM:Requested Procedure -->
 <!-- Study Instance UID -> DICOM:Scheduled Procedure Step ID -->
    <attr tag="00400009" vr="SH">
      <xsl:value-of select="$accnr"/>
    </attr>
    <attr tag="00401001" vr="SH">
      <xsl:value-of select="$accnr"/>
    </attr>
    <!-- HL7:Universal Service Identifier.2 -> DICOM:Requested Procedure Description -->
    <xsl:variable name="desc" select="substring(field[4]/component/text(),1,64)"/>
    <xsl:call-template name="attr">
      <xsl:with-param name="tag" select="'00321060'"/>
      <xsl:with-param name="vr" select="'LO'"/>
      <xsl:with-param name="val" select="$desc"/>
    </xsl:call-template>
    <!-- HL7:Universal Service Identifier -> DICOM:Requested Procedure Code -->
    <xsl:call-template name="codeItem">
      <xsl:with-param name="sqtag" select="'00321064'"/>
      <xsl:with-param name="code" select="string(field[4]/text())"/>
      <xsl:with-param name="scheme" select="'77BOSS'"/>
      <xsl:with-param name="meaning" select="$desc"/>
    </xsl:call-template>
    <!-- HL7:Universal Service Identifier.2 -> DICOM:Scheduled Procedure Step Description -->
    <xsl:call-template name="attr">
      <xsl:with-param name="tag" select="'00400007'"/>
      <xsl:with-param name="vr" select="'LO'"/>
      <xsl:with-param name="val" select="$desc"/>
    </xsl:call-template>
    <!-- HL7:Universal Service Identifier -> DICOM:Scheduled Protocol Code -->
    <xsl:call-template name="codeItem">
      <xsl:with-param name="sqtag" select="'00400008'"/>
      <xsl:with-param name="code" select="string(field[4]/text())"/>
      <xsl:with-param name="scheme" select="'77BOSS'"/>
      <xsl:with-param name="meaning" select="$desc"/>
    </xsl:call-template>
    <!-- HL7:Technician -> Scheduled Performing Physican Name -->
    <xsl:call-template name="cn2pnAttr">
      <xsl:with-param name="tag" select="'00400006'"/>
      <xsl:with-param name="cn" select="field[34]"/>
      <xsl:with-param name="cn26" select="field[34]/subcomponent"/>
    </xsl:call-template>
  </xsl:template>
</xsl:stylesheet>

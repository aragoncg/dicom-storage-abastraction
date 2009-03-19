<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
 version="1.0">
 <xsl:output indent="yes" method="xml" />
 <xsl:template match="/hl7">
  <dataset>
   <xsl:call-template name="common-attrs" />
   <xsl:apply-templates select="PID" />
   <xsl:apply-templates select="OBR" />
   <xsl:apply-templates select="TXA" />
   <xsl:apply-templates select="OBX" />
  </dataset>
 </xsl:template>
 <xsl:template name="common-attrs">
  <!--Specific Character Set-->
  <attr tag="00080005" vr="CS">ISO_IR 100</attr>
  <!--SOP Class UID-->
  <attr tag="00080016" vr="UI">1.2.840.10008.5.1.4.1.1.104.1</attr>
  <!--Study Date-->
  <attr tag="00080020" vr="DA" />
  <!--Study Time-->
  <attr tag="00080030" vr="TM" />
  <!--Accession Number-->
  <attr tag="00080050" vr="SH" />
  <!--Modality-->
  <attr tag="00080060" vr="CS">OT</attr>
  <!--Conversion Type-->
  <attr tag="00080064" vr="CS">SD</attr>
  <!--Manufacturer-->
  <attr tag="00080070" vr="LO" />
  <!--Referring Physician's Name-->
  <attr tag="00080090" vr="PN" />
  <!--Study ID-->
  <attr tag="00200010" vr="SH" />
  <!--Series Number-->
  <attr tag="00200011" vr="IS">1</attr>
  <!--Instance Number-->
  <attr tag="00200013" vr="IS">1</attr>
  <!--Burned In Annotation-->
  <attr tag="00280301" vr="CS">YES</attr>
  <!--MIME Type of Encapsulated Document-->
  <attr tag="00420012" vr="LO">application/pdf</attr>
 </xsl:template>
 <xsl:template match="PID">
  <!--Patient's Name-->
  <attr tag="00100010" vr="PN">
   <xsl:call-template name="xpn2pn">
    <xsl:with-param name="xpn" select="field[5]" />
   </xsl:call-template>
  </attr>
  <!--Patient ID-->
  <attr tag="00100020" vr="LO">
   <xsl:value-of select="field[3]/text()" />
  </attr>
  <!--Issuer of Patient ID-->
  <attr tag="00100021" vr="LO">
   <xsl:value-of select="field[3]/component[3]" />
  </attr>
  <!--Patient's Birth Date-->
  <attr tag="00100030" vr="DA">
   <xsl:value-of select="field[7]/text()" />
  </attr>
  <!--Patient's Sex-->
  <attr tag="00100040" vr="CS">
   <xsl:value-of select="field[8]/text()" />
  </attr>
 </xsl:template>
 <xsl:template match="OBR">
  <!-- Acquisition DateTime -->
  <attr tag="0008002A" vr="DT">
   <xsl:value-of select="field[7]/text()" />
  </attr>
  <xsl:variable name="status" select="field[25]/text()" />
  <!--Completion Flag-->
  <xsl:choose>
   <xsl:when test="$status='P'">
    <attr tag="0040A491" vr="CS">PARTIAL</attr>
   </xsl:when>
   <xsl:when test="$status='F'">
    <attr tag="0040A491" vr="CS">COMPLETE</attr>
   </xsl:when>
  </xsl:choose>
 </xsl:template>
 <xsl:template match="TXA">
  <xsl:variable name="ts">
   <xsl:choose>
    <!--Transcription Date/Time -->
    <xsl:when test="field[7]/text()">
     <xsl:value-of select="field[7]/text()" />
    </xsl:when>
    <!--Origination date/time -->
    <xsl:otherwise>
     <xsl:value-of select="field[6]/text()" />
    </xsl:otherwise>
   </xsl:choose>
  </xsl:variable>
  <!-- Content Date  -->
  <attr tag="00080023" vr="DA">
   <xsl:value-of select="substring($ts,1,8)" />
  </attr>
  <!-- Content Time -->
  <attr tag="00080033" vr="TM">
   <xsl:value-of select="substring($ts,9)" />
  </attr>
  <xsl:variable name="iuid" select="field[12]/text()" />
  <!--SOP Instance UID-->
  <attr tag="00080018" vr="UI">
   <xsl:value-of select="$iuid" />
  </attr>
  <!--Series Instance UID-->
  <attr tag="0020000E" vr="UI">
   <xsl:value-of select="concat($iuid,'.1')" />
  </attr>
  <xsl:variable name="parent_iuid" select="field[13]/text()" />
  <xsl:if test="$parent_iuid">
   <!-- Predecessor Documents Sequence-->
   <attr tag="0040A360" vr="SQ">
    <item>
     <!-- Referenced Series Sequence -->
     <attr tag="00081115" vr="SQ">
      <item>
       <!-- Referenced SOP Sequence -->
       <attr tag="00081199" vr="SQ">
        <item>
         <!-- Referenced SOP Class UID -->
         <attr tag="00081150" vr="UI">1.2.840.10008.5.1.4.1.1.104.1</attr>
         <!-- Referenced SOP Instance UID -->
         <attr tag="00081155" vr="UI">
          <xsl:value-of select="$parent_iuid" />
         </attr>
        </item>
       </attr>
       <!--Series Instance UID-->
       <attr tag="0020000E" vr="UI">
        <xsl:value-of select="concat($parent_iuid,'.1')" />
       </attr>
      </item>
     </attr>
     <!--Study Instance UID-->
     <attr tag="0020000D" vr="UI">
      <xsl:value-of select="../OBX[field[2]='HD']/field[5]/text()" />
     </attr>
    </item>
   </attr>
  </xsl:if>
  <!--Verification Flag-->
  <xsl:variable name="txa17" select="field[17]/text()" />
  <attr tag="0040A493" vr="CS">
   <xsl:choose>
    <xsl:when test="$txa17='AU' or $txa17='LA'">VERIFIED</xsl:when>
    <xsl:otherwise>UNVERIFIED</xsl:otherwise>
   </xsl:choose>
  </attr>
  <xsl:if test="field[22]/component">
   <!-- Verifying Observer Sequence -->
   <attr tag="0040A073" vr="SQ">
    <item>
     <!-- Verification DateTime -->
     <attr tag="0040A030" vr="DT">
      <xsl:value-of select="field[22]/component[14]" />
     </attr>
     <!-- Verifying Observer Name -->
     <attr tag="0040A075" vr="PN">
      <xsl:call-template name="ppn2pn">
       <xsl:with-param name="ppn" select="field[22]" />
      </xsl:call-template>
     </attr>
    </item>
   </attr>
  </xsl:if>
 </xsl:template>
 <xsl:template match="OBX[field[2]='HD']">
  <!--Study Instance UID-->
  <attr tag="0020000D" vr="UI">
   <xsl:value-of select="field[5]/text()" />
  </attr>
 </xsl:template>
 <xsl:template match="OBX[field[5]/component[2]='PDF']">
  <xsl:variable name="obsid" select="field[3]" />
  <xsl:variable name="obsidcode" select="$obsid/text()" />
  <xsl:variable name="obsidmeaning">
   <xsl:choose>
    <xsl:when test="$obsid/component[1]">
     <xsl:value-of select="$obsid/component[1]" />
    </xsl:when>
    <xsl:otherwise><!-- missing ^ prefix -->
     <xsl:value-of select="$obsidcode" />
    </xsl:otherwise>
   </xsl:choose>
  </xsl:variable>
  <xsl:variable name="obsidscheme" select="$obsid/component[2]" />
  <xsl:choose>
   <xsl:when test="$obsidscheme">
    <xsl:call-template name="conceptName">
     <xsl:with-param name="code" select="$obsidcode" />
     <xsl:with-param name="scheme" select="$obsidscheme" />
     <xsl:with-param name="meaning" select="$obsidmeaning" />
    </xsl:call-template>
   </xsl:when>
   <!-- Encode Document Title -->
   <xsl:when test="$obsidmeaning='Cardiac Catheterization Report'">
    <xsl:call-template name="conceptName">
     <xsl:with-param name="code">18745-0</xsl:with-param>
     <xsl:with-param name="scheme">LN</xsl:with-param>
     <xsl:with-param name="meaning">Cardiac Catheterization Report</xsl:with-param>
    </xsl:call-template>
   </xsl:when>
   <xsl:when test="$obsidmeaning='Cardiac Electrophysiology Report'">
    <xsl:call-template name="conceptName">
     <xsl:with-param name="code">18750-0</xsl:with-param>
     <xsl:with-param name="scheme">LN</xsl:with-param>
     <xsl:with-param name="meaning">Cardiac Electrophysiology Report</xsl:with-param>
    </xsl:call-template>
   </xsl:when>
   <xsl:when test="$obsidmeaning='CT Abdomen Report'">
    <xsl:call-template name="conceptName">
     <xsl:with-param name="code">11540-2</xsl:with-param>
     <xsl:with-param name="scheme">LN</xsl:with-param>
     <xsl:with-param name="meaning">CT Abdomen Report</xsl:with-param>
    </xsl:call-template>
   </xsl:when>
   <xsl:when test="$obsidmeaning='CT Chest Report'">
    <xsl:call-template name="conceptName">
     <xsl:with-param name="code">11538-6</xsl:with-param>
     <xsl:with-param name="scheme">LN</xsl:with-param>
     <xsl:with-param name="meaning">CT Chest Report</xsl:with-param>
    </xsl:call-template>
   </xsl:when>
   <xsl:when test="$obsidmeaning='CT Head Report'">
    <xsl:call-template name="conceptName">
     <xsl:with-param name="code">11539-4</xsl:with-param>
     <xsl:with-param name="scheme">LN</xsl:with-param>
     <xsl:with-param name="meaning">CT Head Report</xsl:with-param>
    </xsl:call-template>
   </xsl:when>
   <xsl:when test="$obsidmeaning='CT Report'">
    <xsl:call-template name="conceptName">
     <xsl:with-param name="code">18747-6</xsl:with-param>
     <xsl:with-param name="scheme">LN</xsl:with-param>
     <xsl:with-param name="meaning">CT Report</xsl:with-param>
    </xsl:call-template>
   </xsl:when>
   <xsl:when test="$obsidmeaning='Diagnostic Imaging Report'">
    <xsl:call-template name="conceptName">
     <xsl:with-param name="code">18748-4</xsl:with-param>
     <xsl:with-param name="scheme">LN</xsl:with-param>
     <xsl:with-param name="meaning">Diagnostic Imaging Report</xsl:with-param>
    </xsl:call-template>
   </xsl:when>
   <xsl:when test="$obsidmeaning='Echocardiography Report'">
    <xsl:call-template name="conceptName">
     <xsl:with-param name="code">11522-0</xsl:with-param>
     <xsl:with-param name="scheme">LN</xsl:with-param>
     <xsl:with-param name="meaning">Echocardiography Report</xsl:with-param>
    </xsl:call-template>
   </xsl:when>
   <xsl:when test="$obsidmeaning='ECG Report'">
    <xsl:call-template name="conceptName">
     <xsl:with-param name="code">11524-0</xsl:with-param>
     <xsl:with-param name="scheme">LN</xsl:with-param>
     <xsl:with-param name="meaning">ECG Report</xsl:with-param>
    </xsl:call-template>
   </xsl:when>
   <xsl:when test="$obsidmeaning='Exercise Stress Test Report'">
    <xsl:call-template name="conceptName">
     <xsl:with-param name="code">18752-6</xsl:with-param>
     <xsl:with-param name="scheme">LN</xsl:with-param>
     <xsl:with-param name="meaning">Exercise Stress Test Report</xsl:with-param>
    </xsl:call-template>
   </xsl:when>
   <xsl:when test="$obsidmeaning='Holter Study Report'">
    <xsl:call-template name="conceptName">
     <xsl:with-param name="code">18754-2</xsl:with-param>
     <xsl:with-param name="scheme">LN</xsl:with-param>
     <xsl:with-param name="meaning">Holter Study Report</xsl:with-param>
    </xsl:call-template>
   </xsl:when>
   <xsl:when test="$obsidmeaning='Ultrasound Report'">
    <xsl:call-template name="conceptName">
     <xsl:with-param name="code">18760-9</xsl:with-param>
     <xsl:with-param name="scheme">LN</xsl:with-param>
     <xsl:with-param name="meaning">Ultrasound Report</xsl:with-param>
    </xsl:call-template>
   </xsl:when>
   <xsl:when test="$obsidmeaning='MRI Head Report'">
    <xsl:call-template name="conceptName">
     <xsl:with-param name="code">11541-0</xsl:with-param>
     <xsl:with-param name="scheme">LN</xsl:with-param>
     <xsl:with-param name="meaning">MRI Head Report</xsl:with-param>
    </xsl:call-template>
   </xsl:when>
   <xsl:when test="$obsidmeaning='MRI Report'">
    <xsl:call-template name="conceptName">
     <xsl:with-param name="code">18755-9</xsl:with-param>
     <xsl:with-param name="scheme">LN</xsl:with-param>
     <xsl:with-param name="meaning">MRI Report</xsl:with-param>
    </xsl:call-template>
   </xsl:when>
   <xsl:when test="$obsidmeaning='MRI Spine Report'">
    <xsl:call-template name="conceptName">
     <xsl:with-param name="code">18756-7</xsl:with-param>
     <xsl:with-param name="scheme">LN</xsl:with-param>
     <xsl:with-param name="meaning">MRI Spine Report</xsl:with-param>
    </xsl:call-template>
   </xsl:when>
   <xsl:when test="$obsidmeaning='Nuclear Medicine Report'">
    <xsl:call-template name="conceptName">
     <xsl:with-param name="code">18757-5</xsl:with-param>
     <xsl:with-param name="scheme">LN</xsl:with-param>
     <xsl:with-param name="meaning">Nuclear Medicine Report</xsl:with-param>
    </xsl:call-template>
   </xsl:when>
   <xsl:when test="$obsidmeaning='Ultrasound Obstetric and Gyn Report'">
    <xsl:call-template name="conceptName">
     <xsl:with-param name="code">11525-3</xsl:with-param>
     <xsl:with-param name="scheme">LN</xsl:with-param>
     <xsl:with-param name="meaning">Ultrasound Obstetric and Gyn Report</xsl:with-param>
    </xsl:call-template>
   </xsl:when>
   <xsl:when test="$obsidmeaning='PET Scan Report'">
    <xsl:call-template name="conceptName">
     <xsl:with-param name="code">18758-3</xsl:with-param>
     <xsl:with-param name="scheme">LN</xsl:with-param>
     <xsl:with-param name="meaning">PET Scan Report</xsl:with-param>
    </xsl:call-template>
   </xsl:when>
   <xsl:when test="$obsidmeaning='Radiology Report'">
    <xsl:call-template name="conceptName">
     <xsl:with-param name="code">11528-7</xsl:with-param>
     <xsl:with-param name="scheme">LN</xsl:with-param>
     <xsl:with-param name="meaning">Radiology Report</xsl:with-param>
    </xsl:call-template>
   </xsl:when>
   <!-- fallback to general Diagnostic Imaging Report -->
   <xsl:otherwise>
    <xsl:call-template name="conceptName">
     <xsl:with-param name="code">18748-4</xsl:with-param>
     <xsl:with-param name="scheme">LN</xsl:with-param>
     <xsl:with-param name="meaning">Diagnostic Imaging Report</xsl:with-param>
    </xsl:call-template>
   </xsl:otherwise>
  </xsl:choose>
  <!--Document Title-->
  <attr tag="00420010" vr="ST">
   <xsl:value-of select="$obsidmeaning" />
  </attr>
 </xsl:template>
 <xsl:template name="conceptName">
  <xsl:param name="code" />
  <xsl:param name="scheme" />
  <xsl:param name="meaning" />
  <!--Concept Name Code Sequence-->
  <attr tag="0040A043" vr="SQ">
   <item>
    <!--Code Value-->
    <attr tag="00080100" vr="SH">
     <xsl:value-of select="$code" />
    </attr>
    <!--Coding Scheme Designator-->
    <attr tag="00080102" vr="SH">
     <xsl:value-of select="$scheme" />
    </attr>
    <!--Code Meaning-->
    <attr tag="00080104" vr="LO">
     <xsl:value-of select="$meaning" />
    </attr>
   </item>
  </attr>
 </xsl:template>
 <xsl:template name="xpn2pn">
  <xsl:param name="xpn" />
  <xsl:param name="xpn25" select="$xpn/component" />
  <xsl:value-of select="$xpn/text()" />
  <xsl:text>^</xsl:text>
  <xsl:value-of select="$xpn25[1]" />
  <xsl:text>^</xsl:text>
  <xsl:value-of select="$xpn25[2]" />
  <xsl:text>^</xsl:text>
  <xsl:value-of select="$xpn25[4]" />
  <xsl:text>^</xsl:text>
  <xsl:value-of select="$xpn25[3]" />
 </xsl:template>
 <xsl:template name="ppn2pn">
  <xsl:param name="ppn" />
  <xsl:param name="ppn26" select="$ppn/component" />
  <xsl:value-of select="$ppn26[1]" />
  <xsl:text>^</xsl:text>
  <xsl:value-of select="$ppn26[2]" />
  <xsl:text>^</xsl:text>
  <xsl:value-of select="$ppn26[3]" />
  <xsl:text>^</xsl:text>
  <xsl:value-of select="$ppn26[5]" />
  <xsl:text>^</xsl:text>
  <xsl:value-of select="$ppn26[4]" />
 </xsl:template>
</xsl:stylesheet>

<?xml version="1.0" encoding="UTF-8"?>
<!-- 
 Sample C-STORE-RQ attribute coercion for encoding of document titles of
 received Encapsulated PDF Documents. 
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
 version="1.0">
 <xsl:output method="xml" indent="no" />
 <!-- overwritten by application with actual values -->
 <xsl:param name="calling" select="'SAMPLE_MOD'" />
 <xsl:param name="called" select="'DCM4CHEE'" />
 <xsl:param name="date" select="'20051206'" />
 <xsl:param name="time" select="'115600.000'" />
 <xsl:template match="/dataset">
  <dataset>
   <!-- if Encapsulated PDF without or empty Concept Name Code Sequence -->
   <xsl:if test="normalize-space(attr[@tag='00080016'])='1.2.840.10008.5.1.4.1.1.104.1' and not(attr[@tag='0040A043']/item)">
    <xsl:variable name="title" select="normalize-space(attr[@tag='00420010'])" />
    <xsl:choose>
     <xsl:when test="$title='Cardiac Catheterization Report'">
      <xsl:call-template name="conceptName">
       <xsl:with-param name="code">18745-0</xsl:with-param>
       <xsl:with-param name="scheme">LN</xsl:with-param>
       <xsl:with-param name="meaning">Cardiac Catheterization Report</xsl:with-param>
      </xsl:call-template>
     </xsl:when>
     <xsl:when test="$title='Cardiac Electrophysiology Report'">
      <xsl:call-template name="conceptName">
       <xsl:with-param name="code">18750-0</xsl:with-param>
       <xsl:with-param name="scheme">LN</xsl:with-param>
       <xsl:with-param name="meaning">Cardiac Electrophysiology Report</xsl:with-param>
      </xsl:call-template>
     </xsl:when>
     <xsl:when test="$title='CT Abdomen Report'">
      <xsl:call-template name="conceptName">
       <xsl:with-param name="code">11540-2</xsl:with-param>
       <xsl:with-param name="scheme">LN</xsl:with-param>
       <xsl:with-param name="meaning">CT Abdomen Report</xsl:with-param>
      </xsl:call-template>
     </xsl:when>
     <xsl:when test="$title='CT Chest Report'">
      <xsl:call-template name="conceptName">
       <xsl:with-param name="code">11538-6</xsl:with-param>
       <xsl:with-param name="scheme">LN</xsl:with-param>
       <xsl:with-param name="meaning">CT Chest Report</xsl:with-param>
      </xsl:call-template>
     </xsl:when>
     <xsl:when test="$title='CT Head Report'">
      <xsl:call-template name="conceptName">
       <xsl:with-param name="code">11539-4</xsl:with-param>
       <xsl:with-param name="scheme">LN</xsl:with-param>
       <xsl:with-param name="meaning">CT Head Report</xsl:with-param>
      </xsl:call-template>
     </xsl:when>
     <xsl:when test="$title='CT Report'">
      <xsl:call-template name="conceptName">
       <xsl:with-param name="code">18747-6</xsl:with-param>
       <xsl:with-param name="scheme">LN</xsl:with-param>
       <xsl:with-param name="meaning">CT Report</xsl:with-param>
      </xsl:call-template>
     </xsl:when>
     <xsl:when test="$title='Diagnostic Imaging Report'">
      <xsl:call-template name="conceptName">
       <xsl:with-param name="code">18748-4</xsl:with-param>
       <xsl:with-param name="scheme">LN</xsl:with-param>
       <xsl:with-param name="meaning">Diagnostic Imaging Report</xsl:with-param>
      </xsl:call-template>
     </xsl:when>
     <xsl:when test="$title='Echocardiography Report'">
      <xsl:call-template name="conceptName">
       <xsl:with-param name="code">11522-0</xsl:with-param>
       <xsl:with-param name="scheme">LN</xsl:with-param>
       <xsl:with-param name="meaning">Echocardiography Report</xsl:with-param>
      </xsl:call-template>
     </xsl:when>
     <xsl:when test="$title='ECG Report'">
      <xsl:call-template name="conceptName">
       <xsl:with-param name="code">11524-0</xsl:with-param>
       <xsl:with-param name="scheme">LN</xsl:with-param>
       <xsl:with-param name="meaning">ECG Report</xsl:with-param>
      </xsl:call-template>
     </xsl:when>
     <xsl:when test="$title='Exercise Stress Test Report'">
      <xsl:call-template name="conceptName">
       <xsl:with-param name="code">18752-6</xsl:with-param>
       <xsl:with-param name="scheme">LN</xsl:with-param>
       <xsl:with-param name="meaning">Exercise Stress Test Report</xsl:with-param>
      </xsl:call-template>
     </xsl:when>
     <xsl:when test="$title='Holter Study Report'">
      <xsl:call-template name="conceptName">
       <xsl:with-param name="code">18754-2</xsl:with-param>
       <xsl:with-param name="scheme">LN</xsl:with-param>
       <xsl:with-param name="meaning">Holter Study Report</xsl:with-param>
      </xsl:call-template>
     </xsl:when>
     <xsl:when test="$title='Ultrasound Report'">
      <xsl:call-template name="conceptName">
       <xsl:with-param name="code">18760-9</xsl:with-param>
       <xsl:with-param name="scheme">LN</xsl:with-param>
       <xsl:with-param name="meaning">Ultrasound Report</xsl:with-param>
      </xsl:call-template>
     </xsl:when>
     <xsl:when test="$title='MRI Head Report'">
      <xsl:call-template name="conceptName">
       <xsl:with-param name="code">11541-0</xsl:with-param>
       <xsl:with-param name="scheme">LN</xsl:with-param>
       <xsl:with-param name="meaning">MRI Head Report</xsl:with-param>
      </xsl:call-template>
     </xsl:when>
     <xsl:when test="$title='MRI Report'">
      <xsl:call-template name="conceptName">
       <xsl:with-param name="code">18755-9</xsl:with-param>
       <xsl:with-param name="scheme">LN</xsl:with-param>
       <xsl:with-param name="meaning">MRI Report</xsl:with-param>
      </xsl:call-template>
     </xsl:when>
     <xsl:when test="$title='MRI Spine Report'">
      <xsl:call-template name="conceptName">
       <xsl:with-param name="code">18756-7</xsl:with-param>
       <xsl:with-param name="scheme">LN</xsl:with-param>
       <xsl:with-param name="meaning">MRI Spine Report</xsl:with-param>
      </xsl:call-template>
     </xsl:when>
     <xsl:when test="$title='Nuclear Medicine Report'">
      <xsl:call-template name="conceptName">
       <xsl:with-param name="code">18757-5</xsl:with-param>
       <xsl:with-param name="scheme">LN</xsl:with-param>
       <xsl:with-param name="meaning">Nuclear Medicine Report</xsl:with-param>
      </xsl:call-template>
     </xsl:when>
     <xsl:when test="$title='Ultrasound Obstetric and Gyn Report'">
      <xsl:call-template name="conceptName">
       <xsl:with-param name="code">11525-3</xsl:with-param>
       <xsl:with-param name="scheme">LN</xsl:with-param>
       <xsl:with-param name="meaning"> Ultrasound Obstetric and Gyn Report</xsl:with-param>
      </xsl:call-template>
     </xsl:when>
     <xsl:when test="$title='PET Scan Report'">
      <xsl:call-template name="conceptName">
       <xsl:with-param name="code">18758-3</xsl:with-param>
       <xsl:with-param name="scheme">LN</xsl:with-param>
       <xsl:with-param name="meaning">PET Scan Report</xsl:with-param>
      </xsl:call-template>
     </xsl:when>
     <xsl:when test="$title='Radiology Report'">
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
   </xsl:if>
  </dataset>
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
</xsl:stylesheet>

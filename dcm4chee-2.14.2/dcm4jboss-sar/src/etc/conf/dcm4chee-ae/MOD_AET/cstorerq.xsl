<?xml version="1.0" encoding="UTF-8"?>
<!-- 
Sample C-STORE-RQ attribute coercion for copying request attributes exported
on root level to item of Request Attributes Sequence (0040,0275), which is
extracted into DB series record by default attribute filter configuration.
Therefore request attributes becomes available by DICOM Query C-FIND on 
SERIES level. 
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
      <xsl:variable name="reqPhysician" select="attr[@tag='00321032']"/>
      <xsl:variable name="reqService" select="attr[@tag='00321033']"/>
      <xsl:variable name="reqProcDesc" select="attr[@tag='00321060']"/>
      <xsl:variable name="reqProcCodeSeq" select="attr[@tag='00321064']"/>
      <xsl:if test="$reqPhysician or $reqService or $reqProcDesc or $reqProcCodeSeq">
        <!-- (0040,0275) SQ #-1 Request Attributes Sequence -->
        <attr tag="00400275" vr="SQ">
          <item>
            <xsl:copy-of select="$reqPhysician"/>
            <xsl:copy-of select="$reqService"/>
            <xsl:copy-of select="$reqProcDesc"/>
            <xsl:copy-of select="$reqProcCodeSeq"/>
          </item>
        </attr>
      </xsl:if>
    </dataset>
  </xsl:template>
</xsl:stylesheet>

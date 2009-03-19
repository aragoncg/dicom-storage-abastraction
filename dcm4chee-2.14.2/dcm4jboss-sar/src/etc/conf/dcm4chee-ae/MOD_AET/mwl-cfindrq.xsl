<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="xml" indent="no"/>
  <!-- overwritten by application with actual values -->
  <xsl:param name="calling" select="'SAMPLE_MOD'"/>
  <xsl:param name="called" select="'DCM4CHEE'"/>
  <xsl:param name="date" select="'20051206'"/>
  <xsl:param name="time" select="'115600.000'"/>
  <xsl:template match="/">
    <dataset>
      <!-- Scheduled Procedure Step Sequence -->
      <attr tag="00400100" vr="SQ">
        <item>
          <!-- Scheduled Procedure Step Sequence -->
          <attr tag="00400001" vr="AE">
            <xsl:value-of select="$calling"/>
          </attr>
          <!-- Scheduled Procedure Step Start Date -->
          <attr tag="00400002" vr="DA">
            <xsl:value-of select="$date"/>
          </attr>
          <!-- Scheduled Procedure Step Status -->
          <attr tag="00400020" vr="CS">
            <xsl:text>ARRIVED\READY</xsl:text>
          </attr>
        </item>
      </attr>
    </dataset>
  </xsl:template>
</xsl:stylesheet>

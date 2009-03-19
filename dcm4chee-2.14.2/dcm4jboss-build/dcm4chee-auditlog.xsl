<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="xml" omit-xml-declaration="yes" indent="yes"/>
  <xsl:param name="mbean"/>
  <xsl:template match="/">
    <mbean name="{$mbean}">
      <xsl:apply-templates select="//attribute[@access!='read-only']"/>
      <attribute name="State"/>
    </mbean>
  </xsl:template>
  <xsl:template match="attribute">
    <attribute name="{name}">
      <xsl:variable name="type" select="normalize-space(comment())"/>
      <xsl:if test="$type='Network' or $type='Security' or $type='Hardware'">
        <xsl:attribute name="type">
          <xsl:value-of select="$type"/>
        </xsl:attribute>
      </xsl:if>
     </attribute>
  </xsl:template>
</xsl:stylesheet>

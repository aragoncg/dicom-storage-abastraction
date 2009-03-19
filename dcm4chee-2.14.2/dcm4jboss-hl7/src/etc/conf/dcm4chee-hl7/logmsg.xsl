<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:output method="text"/>
    <xsl:param name="maxlen">64</xsl:param>
    <xsl:template match="/hl7">
        <xsl:apply-templates select="*"/>
    </xsl:template>
    <xsl:template match="*">
        <xsl:apply-templates select="field">
            <xsl:with-param name="seg" select="name()"/>
        </xsl:apply-templates>
    </xsl:template>
    <xsl:template match="field">
        <xsl:param name="seg"/>
        <xsl:if test="node()">
        <xsl:text>
   </xsl:text>
        <xsl:value-of select="$seg"/>
        <xsl:text>-</xsl:text>
        <xsl:choose>
            <xsl:when test="$seg = 'MSH'">
                <xsl:value-of select="position()+2"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="position()"/>
            </xsl:otherwise>
        </xsl:choose>
        <xsl:text>:</xsl:text>
        <xsl:call-template name="prompt">
          <xsl:with-param name="value" select="text()"/>
        </xsl:call-template>
        <xsl:apply-templates select="subcomponent"/>
        <xsl:apply-templates select="component"/>
            </xsl:if>
    </xsl:template>
    <xsl:template match="component">
        <xsl:text>^</xsl:text>
        <xsl:call-template name="prompt">
          <xsl:with-param name="value" select="text()"/>
        </xsl:call-template>
      <xsl:apply-templates select="subcomponent"/>
    </xsl:template>
    <xsl:template match="subcomponent">
        <xsl:text>&amp;</xsl:text>
        <xsl:call-template name="prompt">
          <xsl:with-param name="value" select="text()"/>
        </xsl:call-template>
    </xsl:template>
    <xsl:template name="prompt">
      <xsl:param name="value"/>
      <xsl:value-of select="substring($value,1,$maxlen)"/>
      <xsl:if test="string-length($value)&gt;$maxlen">
        <xsl:text>[..]</xsl:text>
      </xsl:if>
    </xsl:template>
</xsl:stylesheet>

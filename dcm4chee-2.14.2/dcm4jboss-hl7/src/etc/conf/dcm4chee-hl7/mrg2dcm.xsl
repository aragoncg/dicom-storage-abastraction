<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:output method="xml" indent="yes"/>
    <xsl:include href="common.xsl"/>
    <xsl:template match="/hl7">
        <dataset>
            <attr tag="00080005" vr="CS">ISO_IR 100</attr>
            <xsl:apply-templates select="MRG"/>
        </dataset>
    </xsl:template>    
    <xsl:template match="MRG">
        <!-- Patient Name -->
        <xsl:call-template name="xpn2pnAttr">
            <xsl:with-param name="tag" select="'00100010'"/>
            <xsl:with-param name="xpn" select="field[7]"/>
        </xsl:call-template>
        <!-- Patient ID -->
        <xsl:call-template name="cx2attrs">
            <xsl:with-param name="idtag" select="'00100020'"/>
            <xsl:with-param name="istag" select="'00100021'"/>
            <xsl:with-param name="cx" select="field[1]"/>
        </xsl:call-template>
    </xsl:template>
</xsl:stylesheet>

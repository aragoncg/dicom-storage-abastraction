<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
  xmlns:redirect="http://xml.apache.org/xalan/redirect"
  extension-element-prefixes="redirect" version="1.0">

<xsl:template name="formatPN">
	<xsl:param name="pn"/>
	<xsl:value-of select="translate($pn,'^',',')"/>
</xsl:template>

<xsl:template name="formatDate">
	<xsl:param name="date"/>
	<xsl:variable name="len" select="string-length($date)"/>
	<xsl:choose>
		<xsl:when test="$len = 10">
			<xsl:value-of select="$date"/>
		</xsl:when>
		<xsl:when test="$len = 8">
			<xsl:value-of select="substring($date,1,4)"/>
			<xsl:text>-</xsl:text>
			<xsl:value-of select="substring($date,5,2)"/>
			<xsl:text>-</xsl:text>
			<xsl:value-of select="substring($date,7)"/>
		</xsl:when>
		<xsl:otherwise>
			<xsl:text>????-??-??</xsl:text>
		</xsl:otherwise>
	</xsl:choose>
</xsl:template>

<xsl:template name="formatTime">
	<xsl:param name="time"/>
	<xsl:variable name="len" select="string-length($time)"/>
	<xsl:choose>
		<xsl:when test="contains($time,':')">
			<xsl:value-of select="substring($time,1,5)"/>
		</xsl:when>
		<xsl:when test="$len &gt; 3">
			<xsl:value-of select="substring($time,1,2)"/>
			<xsl:text>:</xsl:text>
			<xsl:value-of select="substring($time,3,2)"/>
		</xsl:when>
		<xsl:when test="$len &gt; 1">
			<xsl:value-of select="substring($time,1,2)"/>
			<xsl:text>:??</xsl:text>
		</xsl:when>
		<xsl:otherwise>
			<xsl:text>??:??</xsl:text>
		</xsl:otherwise>
	</xsl:choose>
</xsl:template>

<xsl:template name="formatDateTime">
	<xsl:param name="date"/>
	<xsl:param name="time"/>
	<xsl:variable name="d">
		<xsl:call-template name="formatDate">
			<xsl:with-param name="date" select="$date"/>
		</xsl:call-template>
	</xsl:variable>
	<xsl:variable name="t">
		<xsl:call-template name="formatTime">
			<xsl:with-param name="time" select="$time"/>
		</xsl:call-template>
	</xsl:variable>
	<xsl:value-of select="concat($d,' ',$t)"/>
</xsl:template>

<xsl:template name="formatDateWithTime">
	<xsl:param name="dateTime"/>
	<xsl:variable name="d">
		<xsl:call-template name="formatDate">
			<xsl:with-param name="date" select="substring($dateTime,1,8)"/>
		</xsl:call-template>
	</xsl:variable>
	<xsl:variable name="t">
		<xsl:call-template name="formatTime">
			<xsl:with-param name="time" select="substring($dateTime,9,6)"/>
		</xsl:call-template>
	</xsl:variable>
	<xsl:value-of select="concat($d,' ',$t)"/>
</xsl:template>

</xsl:stylesheet>

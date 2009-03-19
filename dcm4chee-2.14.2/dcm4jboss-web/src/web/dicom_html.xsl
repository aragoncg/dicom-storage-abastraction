<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="html"
		doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN"
		doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"
		encoding="UTF-8"/>
		
    <xsl:param name="title">DICOM dataset</xsl:param>
	
	<xsl:template match="/">
		<html>
			<head>
				<title><xsl:value-of select="$title" /></title>
				<link rel="stylesheet" type="text/css" href="dcm-style.css"/>
			</head>
			<body bgcolor="#FFFFFF" leftmargin="0" topmargin="0" marginwidth="0" marginheight="0" link="#FF0000" alink="#FF0000" vlink="#FF0000">
				<table class="dcmds">
					<xsl:apply-templates select="dataset"/>
				</table>
			</body>
		</html>
	</xsl:template>
	
	<xsl:template match="dataset">
		<table class="dcmds">
            <tr class="head"><td>Group</td><td>Element</td><td>Name</td><td>VR</td>
                <td>Length</td><td>VM</td><td>Value</td></tr>
                <xsl:apply-templates select="attr"/>
        </table>
	</xsl:template>
    
    <xsl:template match="item">
        <tr>
        	<td class="itemno"><xsl:value-of select="position()"/></td>
            <td>
                <table class="dcmds">
                    <tr class="head"><td>Group</td><td>Element</td><td>Name</td><td>VR</td>
                        <td>Length</td><td>VM</td><td>Value</td></tr>
                        <xsl:apply-templates select="attr"/>
                </table>
            </td>
        </tr>
    </xsl:template>
	
    <xsl:template match="attr">
        <xsl:variable name="pos" select="(position() mod 2)"/>
        <tr class="row{$pos}">
            <td><xsl:value-of select="substring(@tag,1,4)"/></td><td><xsl:value-of select="substring(@tag,5,4)"/></td>
            <td><xsl:value-of select="@name"/></td><td><xsl:value-of select="@vr"/></td>
            <xsl:choose>
            	<xsl:when test="@vr='SQ'">
   				    <td>--</td>
	        		<td><xsl:value-of select="@vm"/></td>
	        		<td>
			            <table class="dcmds">
			                <tr class="head"><td><i>Items</i></td></tr>
			                <xsl:apply-templates select="item"/>
			            </table>
			        </td>
            	</xsl:when>
            	<xsl:otherwise>
		   		    <td><xsl:value-of select="@len"/></td>
	        		<td><xsl:value-of select="@vm"/></td>
	        		<td><xsl:value-of select="."/></td>
            	</xsl:otherwise>
            </xsl:choose>
        </tr>
    </xsl:template>
    
</xsl:stylesheet>


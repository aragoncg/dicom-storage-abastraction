<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="html" indent="yes" encoding="UTF-8" />
<xsl:variable name="page_title">XDS-I Export</xsl:variable>

<xsl:template match="model">
	<html>
		<head>
			<meta http-equiv="content-type" content="text/html; charset=UTF-8" />
			<title>XDS-I Manifest Overview</title>
			<link rel="stylesheet" href="stylesheet.css" type="text/css"/>
		</head>
		<table class="xds_show" width="100%">
				<colgroup>
					<col width="2%"/>
					<col width="55%"/>
					<col width="28%"/>
					<col width="10%"/>
					<col width="5%"/>
				</colgroup>
			<tr>
				<td class="xds_show_header" colspan="5" >
					Instances referenced in Manifest: <xsl:value-of select="documentID" />
		   		</td>
	        </tr>
			<tr>
				<td class="xds_show_line" >&#160;</td>
				<td class="xds_show_title" >SOP Instance UID</td>
				<td class="xds_show_title" >SOP Class UID</td>
				<td class="xds_show_title" >Type</td>
				<td class="xds_show_title" >&#160;</td>
	    	</tr>
	    	<xsl:apply-templates select="wadoUrls/item" />
	    </table>
	</html>
</xsl:template>

<xsl:template match="item[@type='org.dcm4chex.archive.web.maverick.xdsi.ShowManifestCtrl$ManifestInstance']">
	<tr>
		<td class="xds_show_line" >&#160;</td>
		<td class="xds_show_line" ><xsl:value-of select="iuid" /></td>
		<td class="xds_show_line" ><xsl:value-of select="cuid" /></td>
		<td class="xds_show_line" ><xsl:value-of select="type" /></td>
		<td class="xds_show_line" >
			<xsl:choose>
				<xsl:when test="type='image'">
					<a title="Show Manifest Item" href="{url}"> 
						<img src="images/image.gif" alt="download" border="0" title="Download DICOM instance"/>
					</a>
				</xsl:when>
				<xsl:when test="type='text'">
					<a title="Show Manifest Item" href="{url}">
						<img src="images/sr.gif" alt="download" border="0" title="Open DICOM SR/KOS (text/html)"/>
					</a>
				</xsl:when>
				<xsl:otherwise>
					<a title="Download Manifest Item" href="{url}&amp;contentType=application/dicom">
						<img src="images/save.gif" alt="download" border="0" title="Download DICOM object"/>
					</a>
				</xsl:otherwise>
			</xsl:choose>
		</td>
	</tr>
</xsl:template>

</xsl:stylesheet>


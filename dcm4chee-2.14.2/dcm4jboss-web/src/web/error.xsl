<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:internal="urn:my-internal-data">

   <xsl:variable name="page_title">Error:<xsl:apply-templates select="model/errorType"/></xsl:variable>
   <xsl:include href  = "page.xsl" />

   <xsl:template match="model">
			<table border="1" cellspacing="0" cellpadding="0" width="100%"><tr><td>
				<table border="0">
					<tr>
						<td bgcolor="#eeeeee"> Error </td>
					</tr>
					<tr>
						<td><xsl:apply-templates select="errorType"/>:<xsl:apply-templates select="message"/></td>
					</tr>
					<tr>
						<td class="xsmallText">click <a href="{backURL}">here</a> to resume.</td>
					</tr>
				</table>
			</td></tr></table>
   </xsl:template>

</xsl:stylesheet>


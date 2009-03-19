<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:internal="urn:my-internal-data">

<xsl:param name="ae_mgr.edit" select="'false'" />

<xsl:output method="html" indent="yes" encoding="UTF-8"/>
<xsl:variable name="page_title">AE List</xsl:variable>
<xsl:include href="page.xsl"/>
<xsl:template match="model">

		<table width="100%" border="1" bordercolor="#ffffff" cellspacing="1" cellpadding="2">
		<tr>	<center>
			<td>
				<tr>
					<td width="10%"><h2>AE Title</h2></td>
					<td width="10%"><h2>Hostname</h2></td>
					<td width="5%"><h2>Port</h2></td>	
					<td width="15%"><h2>Cipher</h2></td>
					<td width="10%"><h2>Issuer</h2></td>
					<td width="10%"><h2>User ID</h2></td>
					<td width="10%"><h2>FS Group ID</h2></td>
					<td width="20%"><h2>Description</h2></td>
					<xsl:if test="$ae_mgr.edit='true'">	
						<td colspan="3" width="10%" align="center"><a href="aenew.m"><img src="images/add_aet.gif" alt="add new AET" border="0"/></a></td>
					</xsl:if>
				</tr>
					<xsl:apply-templates select="AEs/item">
						<xsl:sort data-type="text" order="ascending" select="title"/>
					</xsl:apply-templates>
			</td>	</center>
		</tr>
		</table>


</xsl:template>

	<xsl:template match="item[@type='org.dcm4chex.archive.ejb.interfaces.AEDTO']">
		<tr>
	        <td title="AE Title" valign="top" >
				<xsl:value-of select="title"/>&#160;
			</td>
	        <td title="Hostname" valign="top" >
				<xsl:value-of select="hostName"/>&#160;
	        </td>
	        <td title="Port" valign="top" >
					<xsl:value-of select="port"/>&#160;
	        </td>
	        <td title="Cipher" valign="top" >
	        	<xsl:for-each select="cipherSuites/item">
	        		<xsl:value-of select="."/><br/>
				</xsl:for-each>&#160;
	        </td>
	        <td title="Issuer of patient ID" valign="top" >
					<xsl:value-of select="issuerOfPatientID"/>&#160;
	        </td>
	        <td title="User ID" valign="top" >
					<xsl:value-of select="userID"/>&#160;
	        </td>
	        <td title="File System Group ID" valign="top" >
					<xsl:value-of select="fileSystemGroupID"/>&#160;
	        </td>
	        <td title="Description" valign="top" >
					<xsl:value-of select="description"/>&#160;
	        </td>
	        <xsl:if test="$ae_mgr.edit='true'">
				<td align="center" valign="top" >
					<a href="aeedit.m?aet={title}">
						<img src="images/edit.gif" alt="edit" border="0"/>		
					</a>&#160;
		        </td>
				<td align="left" valign="top" >
						<a href="aedelete.m?title={title}" onclick="return confirm('Are you sure you want to delete?')">
						<img src="images/delete.gif" alt="delete" border="0"/>							
						</a>&#160;				
				</td>
			</xsl:if>
			<td align="left" valign="top" >
					<a href="aeecho.m?aet={title}" >
					<xsl:attribute name="onclick" >return doEchoAET('<xsl:value-of select="title"/>')</xsl:attribute>
					<img src="images/echo.gif" alt="Check AET({title}) with echo." border="0"/>							
					</a>&#160;					
			</td>
		</tr>
	</xsl:template>
 
</xsl:stylesheet>



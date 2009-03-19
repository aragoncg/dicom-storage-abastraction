<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:internal="urn:my-internal-data">

<xsl:output method="html" indent="yes" encoding="UTF-8"/>
   <xsl:variable name="page_title">New User</xsl:variable>
   <xsl:include href  = "../page.xsl" />

   <xsl:template match="model">
	 <html><body bgcolor="#FFFFFF" cellpadding="0" cellspacing="0" border="0">
	 	<form name="user_new" action="user_editsubmit.m" method="post" accept-charset="UTF-8" >
			<table border="0" cellspacing="0" cellpadding="0" width="45%" align="center" ><tr><td>
				<table border="0">
					<tr>
						<td width="50">User ID</td>
				        <td title="User ID" >
			                <input size="25" name="userID" type="text" value=""/>
						</td>
					</tr>
					<tr>
						<td width="50">Password</td>
				        <td title="Password" >
			                <input size="25" name="passwd" type="password" value=""/>
				        </td>
					</tr>
					<tr>
						<td width="50">retype Password</td>
				        <td title="Password" >
			                <input size="25" name="passwd1" type="password" value=""/>
				        </td>
					</tr>
					<xsl:apply-templates select="/model/webRoles/roles/item" mode="role-header">
						<xsl:sort data-type="text" order="descending" select="type"/>
						<xsl:sort data-type="text" order="ascending" select="displayName"/>
					</xsl:apply-templates>
					<tr>
						<td colspan="2">
							<center>
								<input type="submit" name="cmd" value="Create"/>
			                  	<input type="submit" name="cancel" value="Cancel" />
			                </center>
			             </td>
					</tr>
				</table>

			</td></tr></table>
			</form>

			</body></html>
   </xsl:template>

<xsl:template match="item" mode="role-header">
	<tr>
		<td width="50"><xsl:value-of select="displayName" /></td>														
		<td title="{displayName} ({name})">
			<input type="checkbox" name="role" value="{name}" />
		</td>
	</tr>
</xsl:template>
   
   
</xsl:stylesheet>



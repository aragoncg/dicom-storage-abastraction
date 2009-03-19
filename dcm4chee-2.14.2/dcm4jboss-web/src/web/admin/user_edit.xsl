<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:internal="urn:my-internal-data">

<xsl:output method="html" indent="yes" encoding="UTF-8"/>

   <xsl:variable name="page_title">Edit User</xsl:variable>
   <xsl:include href  = "../page.xsl" />

   <xsl:template match="model">
	 <html><body bgcolor="#FFFFFF" cellpadding="0" cellspacing="0" border="0">
	 	<center><form name="user_edit" action="user_editsubmit.m" method="post" accept-charset="UTF-8" >
			<table border="0" cellspacing="0" cellpadding="0" width="35%"><tr><td>
				<center><table border="0">
					<tr>
						<td width="50">User ID</td>
				        <td title="User ID" >
				        	<b><xsl:value-of select="editUser/userID" /></b>
			                <input name="userID" type="hidden" value="{editUser/userID}"/>
			                <input type="hidden" name="userHash" value="{editUser/userHash}"/>
						</td>
						
					</tr>
					<tr>				        
						<td width="50">WebUser</td>														
				        <td title="WebUser">
							<input type="checkbox" name="webUser" value="true">
								<xsl:if test="editUser/webUser = 'true'">
									<xsl:attribute name="checked"/>
								</xsl:if>
							</input>
				        </td>
					</tr>
					<tr>				        
						<td width="50">McmUser</td>														
				        <td title="McmUser">
							<input type="checkbox" name="mcmUser" value="true">
								<xsl:if test="editUser/mcmUser = 'true'">
									<xsl:attribute name="checked"/>
								</xsl:if>
							</input>
				        </td>
					</tr>
					<tr>				        
						<td width="50">Datacare</td>														
				        <td title="Datacare">
							<input type="checkbox" name="datacareUser" value="true">
								<xsl:if test="editUser/datacareUser = 'true'">
									<xsl:attribute name="checked"/>
								</xsl:if>
							</input>
				        </td>
					</tr>
					<tr>				        
						<td width="50">WebAdmin</td>														
				        <td title="WebAdmin">
							<input type="checkbox" name="webAdmin" value="true">
								<xsl:if test="editUser/webAdmin = 'true'">
									<xsl:attribute name="checked"/>
								</xsl:if>
							</input>
				        </td>
					</tr>
					<tr>				        
						<td width="50">Admin</td>														
				        <td title="Admin">
							<input type="checkbox" name="JBossAdmin" value="true">
								<xsl:if test="editUser/JBossAdmin = 'true'">
									<xsl:attribute name="checked"/>
								</xsl:if>
							</input>
				        </td>
					</tr>
					<tr>				        
						<td width="50">AuditRep</td>														
				        <td title="Admin">
							<input type="checkbox" name="arrUser" value="true">
								<xsl:if test="editUser/arrUser = 'true'">
									<xsl:attribute name="checked"/>
								</xsl:if>
							</input>
				        </td>
					</tr>
					<tr>
						<td colspan="2">
							<center>
								<input type="submit" name="update" value="Update"/>
			                  	<input type="submit" name="cancel" value="Cancel" />
			                </center>
			             </td>
					</tr>
				</table>
				</center>
			</td></tr></table>
			</form>
		</center>
			</body></html>
   </xsl:template>
   
   
</xsl:stylesheet>



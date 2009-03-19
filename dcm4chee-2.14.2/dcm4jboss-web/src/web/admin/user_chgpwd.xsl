<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:internal="urn:my-internal-data">
	
<xsl:output method="html" indent="yes" encoding="UTF-8"/>

   <xsl:variable name="page_title">Change User password</xsl:variable>
   <xsl:include href  = "../page.xsl" />

   <xsl:template match="model">
	 <html><body bgcolor="#FFFFFF" cellpadding="0" cellspacing="0" border="0">
	 	<center><form name="user_chgpwd" action="user_chgpwdsubmit.m" method="post" accept-charset="UTF-8" >
			<table border="0" cellspacing="0" cellpadding="0" width="35%"><tr><td>
				<center><table border="0">
					<tr>
						<td width="50">User ID</td>
				        <td title="User:" >
				        	<b><xsl:value-of select="userID" /></b>
			                <input name="userID" type="hidden" value="{userID}"/>
						</td>
					</tr>
					<tr>
						<td width="50">Old Password</td>
				        <td title="Old Password" >
			                <input size="25" name="oldPasswd" type="password" value=""/>
				        </td>
					</tr>
					<tr>
						<td width="50">New Password</td>
				        <td title="Password" >
			                <input size="25" name="passwd" type="password" value=""/>
				        </td>
					</tr>
					<tr>
						<td width="50">Retype New Password</td>
				        <td title="Password" >
			                <input size="25" name="passwd1" type="password" value=""/>
				        </td>
					</tr>
					<tr>
						<td colspan="2">
							<center>
								<input type="submit" name="change" value="Change"/>
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



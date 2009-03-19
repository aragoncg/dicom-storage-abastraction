<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:internal="urn:my-internal-data">

<xsl:output method="html" indent="yes" encoding="UTF-8"/>
<xsl:variable name="page_title">User Admin Console</xsl:variable>
<xsl:include href="../page.xsl"/>
<xsl:template match="model">

		<table width="90%" border="1" bordercolor="#ffffff" cellspacing="1" cellpadding="2">
		<tr>	
			<td>
				<tr>
					<td width="15%"><h2>User ID</h2></td>
					<td colspan="{count(/model/webRoles/roles/*)}" width="80%"><h2>Roles</h2></td>
					<td colspan="2" width="5%" align="center">
						<a href="user_new.m">
							<img src="images/addpat.gif" alt="add new user" border="0"/>
						</a>
					</td>
				</tr>
				<tr>
				    <td title="User ID" valign="top" >&#160;</td>
					<xsl:apply-templates select="/model/webRoles/roles/item" mode="role-header">
						<xsl:sort data-type="text" order="descending" select="type"/>
						<xsl:sort data-type="text" order="ascending" select="displayName"/>
					</xsl:apply-templates>
				</tr>
				<xsl:apply-templates select="userList/item">
					<xsl:sort data-type="text" order="ascending" select="userID"/>
				</xsl:apply-templates>
			</td>
		</tr>
		</table>
		<DL>
			<xsl:apply-templates select="webRoles/roles/item" mode="description">
				<xsl:sort data-type="text" order="descending" select="type"/>
				<xsl:sort data-type="text" order="ascending" select="displayName"/>
			</xsl:apply-templates>
		</DL>


</xsl:template>

<xsl:template match="item[@type='org.dcm4chex.archive.web.maverick.admin.DCMUser']">
	<tr>
        <td title="User ID" valign="top" >
			<xsl:value-of select="userID"/>
		</td>
		<xsl:apply-templates select="/model/webRoles/roles/item" mode="role-info">
			<xsl:sort data-type="text" order="descending" select="type"/>
			<xsl:sort data-type="text" order="ascending" select="displayName"/>
			<xsl:with-param name="userID" select="userID"/>
		</xsl:apply-templates>
																
		<td title="Delete user {userID}!" align="left" valign="top" border="0" >
				<a href="user_editsubmit.m?userID={userID}&amp;cmd=deleteUser" 
					onclick="return confirm('Are you sure you want to delete?')">
				<img src="images/delete.gif" alt="delete" border="0"/>							
				</a>					
		</td>
		<xsl:if test="/model/currentUser=userID">
			<td title="Change password for current user ({userID})!" align="left" valign="top" border="0" >
				<a href="useradmin_console.m?chgpwd='true'">
				 Password							
				</a>					
			</td>
		</xsl:if>
	</tr>
</xsl:template>

<xsl:template match="item" mode="role-header">
      <td title="{descr}" align="center"><xsl:value-of select="displayName" />&#160;</td>
</xsl:template>

<xsl:template match="item" mode="role-info">
	<xsl:param name="userID" />
	<xsl:variable name="role" select="name" />
	<xsl:variable name="displayName" select="displayName" />
	<td title="$displayName" align="center">
        <xsl:attribute name="bgcolor" >
        	<xsl:choose>
				<xsl:when test="(type='StudyPermission')">#ccddcc</xsl:when>
        		<xsl:when test="(type='ClientRole')">#779977</xsl:when>
            	<xsl:otherwise>#cecece</xsl:otherwise>
      		</xsl:choose>
      	</xsl:attribute>&#160;
        <xsl:choose>
			<xsl:when test="/model/userList/item[userID=$userID]/roles[item=$role]">
            	<a title="Remove {$displayName} from user {$userID}"
                	href="user_editsubmit.m?userID={$userID}&amp;cmd=removeRole&amp;role={$role}">
                	<img src="images/granted_xxs.gif" alt="granted" border="0" />
            	</a>
            </xsl:when>
            <xsl:otherwise>
            	<a title="Add {$displayName} to user {$userID}"
                	href="user_editsubmit.m?userID={$userID}&amp;cmd=addRole&amp;role={$role}">
                    <img src="images/denied_xxs.gif" alt="denied" border="0" />
                </a>
            </xsl:otherwise>
      	</xsl:choose>&#160;
	</td>
</xsl:template>

<xsl:template match="item" mode="description">
	<DT><xsl:value-of select="displayName" /></DT>
	<DD><xsl:value-of select="descr" /></DD>
</xsl:template>
 
</xsl:stylesheet>



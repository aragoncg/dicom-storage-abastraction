<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="html" indent="yes" encoding="UTF-8" />
<xsl:variable name="page_title">Study Permissions Overview</xsl:variable>
<xsl:include href  = "../page.xsl" />

<xsl:param name="folder.study_permission.free_role_action" select="'false'"/>
<xsl:param name="folder.study_permission.show_only_studyPermissionRoles" select="'false'"/>

<xsl:template match="model">
	<p>
	<div align="center" style="font-size : 22px;" >
		<xsl:text>Permission list for </xsl:text>
		<xsl:choose>
			<xsl:when test="string(studyIUID)">
				<xsl:text>Study IUID:</xsl:text><xsl:value-of select="studyIUID" />
				<xsl:text>(patient: </xsl:text><xsl:value-of select="patient/patientName" />):
			</xsl:when>
			<xsl:otherwise>
				<xsl:text>all studies of patient </xsl:text><xsl:value-of select="patient/patientName" /><xsl:text>:</xsl:text>
			</xsl:otherwise>
		</xsl:choose>
	</div>
	</p>
	<p>
	<div align="center" style="background : green">
		<table width="80%" border="4" align="center">
			<xsl:call-template name="add_colgroup" />
			<tr>
				<td>&#160;</td>
				<xsl:apply-templates select="rolesConfig/actions/item" mode="action_header"/>
			</tr>
			<xsl:choose>
	            <xsl:when test="$folder.study_permission.show_only_studyPermissionRoles = 'true'" >
					<xsl:apply-templates select="rolesConfig/roles/item[type='StudyPermission']" mode="role_line">
					<xsl:sort data-type="text" order="ascending" select="displayName"/>
					</xsl:apply-templates>
				</xsl:when>
	            <xsl:otherwise >
					<xsl:apply-templates select="rolesConfig/roles/item" mode="role_line">
					<xsl:sort data-type="text" order="ascending" select="displayName"/>
					</xsl:apply-templates>
				</xsl:otherwise>
			</xsl:choose>
   		</table>
	</div>
	</p>
		<div align="center" style="background-color: #eeeeee;" >
			<form action="studyPermissionUpdate.m" name="permForm" method="post" accept-charset="UTF-8" >
				<br/>&#160;<br/>
	            <xsl:if test="$folder.study_permission.free_role_action = 'true'" >
					<xsl:if test="/model/studyPermissionCheckDisabled='true' or /model/grantPrivileg='true'" >
						role:&#160;<input size="10" name="role" type="text" value="" />
						&#160;
						action:&#160;<input size="10" name="action" type="text" value="" />
						&#160;
						<input type="submit" name="cmd" value="Add"/>
						<br/>&#160;<br/>
					</xsl:if>
    			</xsl:if>
				<input type="submit" name="cmd" value="Cancel"/>
				<br/>&#160;<br/>
			</form>
		</div>
</xsl:template>

<xsl:template match="item" mode="action_header">
	<th title="{.}" >
   		<xsl:value-of select="@key"/> (<xsl:value-of select="."/>)&#160;
	</th>
</xsl:template>

<xsl:template match="item" mode="role_line">
   	<xsl:variable name="updateAllAllowed" select="/model/studyPermissionCheckDisabled='true' or /model/grantPrivileg='true'" />
	<tr>
		<th title="Role:{name}({descr})"><xsl:value-of select="displayName" /></th>
		<xsl:apply-templates select="/model/rolesConfig/actions/item" mode="action_line_edit">
			<xsl:with-param name="role" select="name"/>
			<xsl:with-param name="updateAllAllowed" select="$updateAllAllowed"/>
		</xsl:apply-templates>	
	</tr>
</xsl:template>

<xsl:template match="item" mode="action_line_edit">
   	<xsl:param name="role" />
  	<xsl:param name="updateAllAllowed" />
   	<xsl:variable name="action" select="@key" />
   	<xsl:variable name="countPermissions" select="count(/model/rolesWithActions/item[@key=$role]/item[@key=$action]/item)" />
   	<xsl:variable name="updateThisAllowed" select="$updateAllAllowed or (/model/grantOwnPrivileg and /model/grantedActions[item=$action])" />
	<td title="{$role}:{.}" align="center">
		<xsl:if test="/model/grantOwnPrivileg and $updateThisAllowed!='true'" >
			<xsl:attribute name="bgcolor">#eeeeee</xsl:attribute>
		</xsl:if>
		<xsl:choose>
			<xsl:when test="$countPermissions = /model/countStudies">
				<xsl:choose>
					<xsl:when test="$updateThisAllowed">
						<a title="remove permission for {$role}:{.}" 
							href="studyPermissionUpdate.m?cmd=remove&amp;role={$role}&amp;action={$action}" >
		   					<img src="images/granted.gif" alt="granted" border="0" />
		   				</a>
			   		</xsl:when>
					<xsl:otherwise>
	   					<img src="images/granted.gif" alt="granted" border="0" />
			   		</xsl:otherwise>
			   	</xsl:choose>
	   		</xsl:when>
			<xsl:when test="$countPermissions > 0">
				<xsl:choose>
					<xsl:when test="$updateThisAllowed">
						<a title="remove permission for {$role}:{.}" 
							href="studyPermissionUpdate.m?cmd=remove&amp;role={$role}&amp;action={$action}">
			   				<img src="images/granted_part.gif" alt="some studies granted" border="0" />
			   			</a>
			   		</xsl:when>
					<xsl:otherwise>
	   					<img src="images/granted_part.gif" alt="some studies granted" border="0" />
			   		</xsl:otherwise>
			   	</xsl:choose>
	   			<xsl:value-of select="$countPermissions" />/<xsl:value-of select="/model/countStudies"/>
	   		</xsl:when>
			<xsl:otherwise>
				<xsl:choose>
					<xsl:when test="$updateThisAllowed">
						<a title="add permission for {$role}:{.}" 
							href="studyPermissionUpdate.m?cmd=add&amp;role={$role}&amp;action={$action}" >
				   			<img src="images/denied.gif" alt="denied" border="0" />
			   			</a>
			   		</xsl:when>
					<xsl:otherwise>
	   					<img src="images/denied.gif" alt="denied" border="0" />
			   		</xsl:otherwise>
			   	</xsl:choose>
	   		</xsl:otherwise>
	   	</xsl:choose>&#160;
	</td>
</xsl:template>

<xsl:template name="add_colgroup">
	<xsl:variable name="firstColumn" select="10"/>
	<xsl:variable name="actionCount" select="count(/model/rolesConfig/actions/item)"/>
	<xsl:variable name="colWidth" select="(100-$firstColumn) div $actionCount"/>
	<colgroup>
		<col width="{$firstColumn}%"/>
		<xsl:for-each select="/model/rolesConfig/actions/item" >
			<col width="{$colWidth}"/>
		</xsl:for-each>
	</colgroup>
</xsl:template>

</xsl:stylesheet>


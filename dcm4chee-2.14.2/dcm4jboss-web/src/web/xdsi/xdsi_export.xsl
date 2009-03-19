<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="html" indent="yes" encoding="UTF-8" />
<xsl:variable name="page_title">XDS-I Export</xsl:variable>
<xsl:include href  = "../page.xsl" />

<xsl:template match="model">
	<form action="xdsiExport.m" method="post" accept-charset="UTF-8" >
		<table border="0" cellspacing="0" cellpadding="0" width="90%">
			<tr>
				<td colspan="4" align="center">
					Export <xsl:value-of select="numberOfInstances" /> selected Instances of <xsl:value-of select="sourcePatName" />:
		   		</td>
            </tr>
			<tr>
		   		<td>&#160;</td>
        	</tr>
			<tr>
		   		<td align="right">KOS Document Title:&#160;</td>
		        <td title="Document Title">
					<select name="selectedDocTitle" class="xdsiField" >
						<xsl:apply-templates select="docTitles/item">
						    <xsl:with-param name="selectedPos"><xsl:value-of select="/model/selectedDocTitle"/> </xsl:with-param>
						</xsl:apply-templates>
					</select>
		        </td>
        	</tr>
			<tr>
		   		<td align="right">Author Person:&#160;</td>
		        <td title="Author Person">
					<input size="25" name="authorPerson" type="text" value="{authorPerson}" class="xdsiField" />
		        </td>
		   		<td align="right">Class Code:&#160;</td>
		        <td title="Class Code">
					<select name="selectedClassCode" class="xdsiField" >
						<xsl:apply-templates select="classCodes/item">
						    <xsl:with-param name="selectedPos" select="/model/selectedClassCode" />
						</xsl:apply-templates>
					</select>
		        </td>
        	</tr>
			<tr>
		   		<td align="right">Author Role:&#160;</td>
		        <td title="Author Role">
					<select name="selectedAuthorRole" class="xdsiField" >
						<xsl:apply-templates select="authorRoles/item">
						    <xsl:with-param name="selectedPos" select="/model/selectedAuthorRole" />
						</xsl:apply-templates>
					</select>
		        </td>
		   		<td align="right">Content Type:&#160;</td>
		        <td title="Class Code">
					<select name="selectedContentTypeCode" class="xdsiField" >
						<xsl:apply-templates select="contentTypeCodes/item">
						    <xsl:with-param name="selectedPos" select="/model/selectedContentTypeCode" />
						</xsl:apply-templates>
					</select>
		        </td>
        	</tr>
			<tr>
		   		<td align="right">Author Speciality:&#160;</td>
		        <td title="Author Speciality">
					<input size="25" name="authorSpeciality" type="text" value="{authorSpeciality}" disabled="true" class="xdsiField" />
		        </td>
		   		<td align="right">Healthcare Type:&#160;</td>
		        <td title="Class Code">
					<select name="selectedHealthCareTypeCode" class="xdsiField" >
						<xsl:apply-templates select="healthCareFacilityTypeCodes/item">
						    <xsl:with-param name="selectedPos" select="/model/selectedHealthCareTypeCode" />
						</xsl:apply-templates>
					</select>
		        </td>
        	</tr>
			<tr>
		   		<td align="right">Author Institution:&#160;</td>
		        <td title="Author Institution">
					<input size="25" name="authorInstitution" type="text" value="{authorInstitution}" disabled="true" class="xdsiField" />
		        </td>
        	</tr>
			<tr>
		   		<td>&#160;</td>
        	</tr>
			<tr>
		   		<td align="right" >Event Codes:&#160;</td>
		        <td title="Event Code" >
					<select name="selectedEventCode" size="10" class="xdsiListSelection">
						<xsl:apply-templates select="eventCodes/item">
						    <xsl:with-param name="selectedPos" select="/model/selectedEventCode" />
						</xsl:apply-templates>
					</select>
		        </td>
		        <td>
		        	<table>
				   		<tr><td align="center" >						
				   			<input type="submit" name="addEventCode" value="&gt;"/>
				   			<input type="submit" name="delEventCode" value="&lt;"/>
				   			<input type="submit" name="deselectAllEventCodes" value="&lt;&lt;"/>
		   				</td></tr>
		   			</table>
		   		</td>
		        <td title="Selected Event Codes">
					<select name="removeEventCode" size="10" class="xdsiListSelection">
						<xsl:apply-templates select="selectedEventCodes/item">
						    <xsl:with-param name="selectedPos" select="/model/removeEventCode" />
						</xsl:apply-templates>
					</select>
		        </td>
        	</tr>
			<tr>
		   		<td>&#160;</td>
        	</tr>
            <xsl:if test="/model/XDSQuery='true'">
				<tr>
			   		<td align="right" >
			   			<table>
			   				<tr><td>Existing Documents:</td></tr>
				   			<tr><td align="center"><input type="submit" name="query" value="Query"/></td></tr>
			   			</table>
			   		</td>
			        <td title="Existing Documents" >
						<select name="selectedDocument" size="10" class="xdsiListSelection">
							<xsl:apply-templates select="documents/item">
							    <xsl:with-param name="selectedPos" select="/model/selectedDocument" />
							</xsl:apply-templates>
						</select>
			        </td>
			        <td>
			        	<table>
			   				<tr><td align="center">Add Document Association</td></tr>
			   				<tr><td align="center">Association Type:</td></tr>
							<tr><td align="center">
								<select name="assocType" >
									<option value="HasMember" selected="true">HasMember</option>
									<option value="RPLC">RPLC</option>
									<option value="APND">APND</option>
									<option value="XFRM">XFRM</option>
								</select></td></tr>
							<tr><td align="center">Association Status:</td></tr>
							<tr><td align="center">
								<select name="assocStatus" >
									<option value="Approved" selected="true">Approved</option>
									<option value="Deprecated">Deprecated</option>
									<option value="Reference">Reference</option>
								</select>
							</td></tr>
			   				<tr><td align="center">
			   					<input type="submit" name="addAssociation" value="&gt;"/>
			   					<input type="submit" name="delAssociation" value="&lt;"/>
			   					<input type="submit" name="deselectAllAssociations" value="&lt;&lt;"/>
			   				</td></tr>
			   			</table>
			   		</td>
			        <td title="Associations">
						<select name="selectedAssociation" size="10" class="xdsiListSelection">
							<xsl:apply-templates select="associations/item">
							    <xsl:with-param name="selectedPos" select="/model/selectedAssociation" />
							</xsl:apply-templates>
						</select>
			        </td>
	        	</tr>
				<tr>
			   		<td>&#160;</td>
	        	</tr>
				<tr>
			   		<td align="right" >
			   			<table>
			   				<tr><td>Existing Folders:&#160;</td></tr>
			   				<tr><td align="center"><input type="submit" name="queryFolder"  value="Query "/></td></tr>
			   				<tr><td>Folder Name:</td></tr>
			   				<tr><td><input type="text" name="folderName" value="Create" size="13"/></td></tr>
			   				<tr><td>Comment</td></tr>
			   				<tr><td><input type="text" name="folderComment" value="Create" size="13"/></td></tr>
			   				<tr><td align="center"><input type="submit" name="createFolder" value="Create"/></td></tr>
			   			</table>
			   		</td>
			        <td title="Existing Folders">
						<select name="selectedFolder" size="10" class="xdsiListSelection">
							<xsl:apply-templates select="folders/item">
							    <xsl:with-param name="selectedPos" select="/model/selectedFolder" />
							</xsl:apply-templates>
						</select>
			        </td>
			        <td>
			        	<table>
					   		<tr><td align="center" >						
					   			<input type="submit" name="addFolder" value="&gt;"/>
					   			<input type="submit" name="delFolder" value="&lt;"/>
					   			<input type="submit" name="deselectAllFolders" value="&lt;&lt;"/>
			   				</td></tr>
			   			</table>
			   		</td>
	 		        <td title="LinkFolders">
						<select name="selectedLinkFolder" size="10" class="xdsiListSelection">
							<xsl:apply-templates select="linkFolders/item">
							    <xsl:with-param name="selectedPos" select="/model/selectedLinkFolder" />
							</xsl:apply-templates>
						</select>
			        </td>
	        	</tr>
	        </xsl:if>
			<tr>
		   		<td>&#160;</td>
        	</tr>
			<!-- uncommentto test submitting two documents one manifest with a PDF doc 
			<tr>
				<td>IUID of a PDF document:</td>
		   		<td><input type="text" name="pdfIUID" value="" /></td>
        	</tr> 
        	-->
			<tr>
				<td colspan="4" align="center">
						<input type="submit" name="clear" value="Clear"/>
						&#160;&#160;&#160;
						<input type="submit" name="export" value="Export"/>
						&#160;&#160;
		   				<input type="submit" name="submitAndCreateFolder" value="Export&amp;New Folder"/>
		   				&#160;&#160;
						<input type="submit" name="cancel" value="Cancel" />
    			</td>
			</tr>
			<tr>
		   		<td>&#160;</td>
        	</tr>
			<tr>
		   		<td>&#160;</td>
        	</tr>
	    </table>
	</form>
	
	
	
</xsl:template>

<xsl:template match="item[@type='org.dcm4chex.archive.web.maverick.util.CodeItem']">
  <xsl:param name="selectedPos">?</xsl:param>
	<option>
		<xsl:attribute name="value"><xsl:value-of select="position()-1"/></xsl:attribute>
		<xsl:if test="$selectedPos=position()-1">
			<xsl:attribute name="selected">true</xsl:attribute>
		</xsl:if>
		<xsl:value-of select="codeMeaning"/>
	</option>
</xsl:template>

<xsl:template match="item[@type='java.lang.String']">
  <xsl:param name="selectedPos">?</xsl:param>
	<option>
		<xsl:attribute name="value"><xsl:value-of select="position()-1"/></xsl:attribute>
		<xsl:if test="$selectedPos=position()-1">
			<xsl:attribute name="selected">true</xsl:attribute>
		</xsl:if>
		<xsl:value-of select="."/>
	</option>
</xsl:template>

<xsl:template match="item[@type='org.dcm4chex.archive.web.maverick.xdsi.XDSDocumentObject']">
  <xsl:param name="selectedPos">?</xsl:param>
	<option>
		<xsl:attribute name="value"><xsl:value-of select="position()-1"/></xsl:attribute>
		<xsl:if test="$selectedPos=position()-1">
			<xsl:attribute name="selected">true</xsl:attribute>
		</xsl:if>
		<xsl:value-of select="name"/> (<xsl:value-of select="creationTime"/>):<xsl:value-of select="status"/>
	</option>
</xsl:template>
<xsl:template match="item[@type='org.dcm4chex.archive.web.maverick.xdsi.XDSFolderObject']">
  <xsl:param name="selectedPos">?</xsl:param>
	<option>
		<xsl:attribute name="value"><xsl:value-of select="position()-1"/></xsl:attribute>
		<xsl:if test="$selectedPos=position()-1">
			<xsl:attribute name="selected">true</xsl:attribute>
		</xsl:if>
		<xsl:value-of select="name"/>
	</option>
</xsl:template>
<xsl:template match="item[@type='org.dcm4chex.archive.web.maverick.xdsi.XDSAssociation']">
  <xsl:param name="selectedPos">?</xsl:param>
	<option>
		<xsl:attribute name="value"><xsl:value-of select="position()-1"/></xsl:attribute>
		<xsl:if test="$selectedPos=position()-1">
			<xsl:attribute name="selected">true</xsl:attribute>
		</xsl:if>
		<xsl:value-of select="target/name"/>|<xsl:value-of select="type"/>|<xsl:value-of select="status"/>
	</option>
</xsl:template>

</xsl:stylesheet>


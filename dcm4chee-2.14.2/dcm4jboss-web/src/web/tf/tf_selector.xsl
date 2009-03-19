<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="html" indent="yes" encoding="UTF-8" />
<xsl:variable name="page_title">Teaching File Selector</xsl:variable>
<xsl:include href  = "../page.xsl" />

<xsl:template match="model">
	<form action="tfSelector.m" method="post" accept-charset="UTF-8" >
		<table border="0" cellspacing="0" cellpadding="0" width="90%">
			<tr>
				<td colspan="2" align="center">
					Export <xsl:value-of select="numberOfInstances" /> selected Instances:
		   		</td>
            </tr>
			<tr>
		   		<td>&#160;</td>
        	</tr>
			<tr>
		   		<td align="right">Document Title:&#160;</td>
		        <td title="Document Title">
					<select name="selectedTitle" >
						<xsl:for-each select="docTitles/item">
							<option>
								<xsl:attribute name="value"><xsl:value-of select="position()-1"/></xsl:attribute>
								<xsl:if test="/model/selectedDocTitle=position()-1">
									<xsl:attribute name="selected">true</xsl:attribute>
								</xsl:if>
								<xsl:value-of select="."/>
							</option>
						</xsl:for-each>
					</select>
		        </td>
        	</tr>
			<tr>
		   		<td align="right">Delay Reason:&#160;</td>
		        <td title="Delay Reason">
					<select name="selectedDelayReason" >
						<option value="-1" selected="">-</option>
						<xsl:for-each select="delayReasons/item">
							<option>
								<xsl:attribute name="value"><xsl:value-of select="position()-1"/></xsl:attribute>
								<xsl:if test="/model/selectedDelayReason=position()-1">
									<xsl:attribute name="selected">true</xsl:attribute>
								</xsl:if>
								<xsl:value-of select="."/>
							</option>
						</xsl:for-each>
					</select>
		        </td>
        	</tr>
			<tr>
		   		<td align="right">Disposition:&#160;</td>
		        <td title="Disposition">
					<select name="disposition" >
						<option value="" selected="">-</option>
						<xsl:for-each select="dispositions/item">
							<option>
								<xsl:attribute name="value"><xsl:value-of select="."/></xsl:attribute>
								<xsl:if test=". = /model/disposition">
									<xsl:attribute name="selected">true</xsl:attribute>
								</xsl:if>
								<xsl:value-of select="."/>
							</option>
						</xsl:for-each>
					</select>
		        </td>
        	</tr>
			<tr>
		   		<td>&#160;</td>
        	</tr>
			<tr>
				<td colspan="2" align="center">
        			<b>Teaching File Structured Report Manifest</b>
					&#160;&#160;Create
					<input type="radio" name="useManifest" value="yes" >
						<xsl:if test="manifestModel/useManifest='true'">
							<xsl:attribute name="checked">true</xsl:attribute>
						</xsl:if>
					</input>
					&#160;Ignore
					<input type="radio" name="useManifest" value="no" >
						<xsl:if test="manifestModel/useManifest!='true'">
							<xsl:attribute name="checked">true</xsl:attribute>
						</xsl:if>
					</input>
		   		</td>
            </tr>
			<tr>
		   		<td>&#160;</td>
        	</tr>
        	<xsl:apply-templates select="manifestModel/textFields"/>
			<tr>
		   		<td align="right">Category:&#160;</td>
		        <td title="Category of teaching file">
					<select name="category" >
						<xsl:for-each select="manifestModel/categories/item">
							<option>
								<xsl:attribute name="value"><xsl:value-of select="position()-1"/></xsl:attribute>
								<xsl:if test="/model/manifestModel/selectedCategory=position()-1">
									<xsl:attribute name="selected">true</xsl:attribute>
								</xsl:if>
								<xsl:value-of select="."/>
							</option>
						</xsl:for-each>
					</select>
		        </td>
        	</tr>
			<tr>
		   		<td align="right">Level:&#160;</td>
		        <td title="Level of teaching file">
					<select name="level" >
						<option value="-1">
							<xsl:if test="manifestModel/selectedLevel=-1">
								<xsl:attribute name="selected">true</xsl:attribute>
							</xsl:if>
						-
						</option>
						<xsl:for-each select="manifestModel/levels/item">
							<option>
								<xsl:attribute name="value"><xsl:value-of select="position()-1"/></xsl:attribute>
								<xsl:if test="/model/manifestModel/selectedLevel=position()-1">
									<xsl:attribute name="selected">true</xsl:attribute>
								</xsl:if>
								<xsl:value-of select="."/>
							</option>
						</xsl:for-each>
					</select>
		        </td>
        	</tr>
			<tr>
		   		<td>&#160;</td>
        	</tr>
			<tr>
		   		<td align="right">Diagnosis confirmed:&#160;</td>
		        <td title="Diagnosis confirmed">
					&#160;&#160;Yes<input type="radio" name="confirmed" value="yes" >
						<xsl:if test="/model/manifestModel/confirmed='true'">
							<xsl:attribute name="checked">true</xsl:attribute>
						</xsl:if>
					</input>
					&#160;No
					<input type="radio" name="confirmed" value="no">
						<xsl:if test="/model/manifestModel/confirmed!='true'">
							<xsl:attribute name="checked">true</xsl:attribute>
						</xsl:if>
					</input>
		        </td>
        	</tr>
			<tr>
		   		<td>&#160;</td>
        	</tr>
			<tr>
				<td colspan="2" align="center">
						<input type="submit" name="clear" value="Clear"/>
						&#160;&#160;&#160;
						<input type="submit" name="export" value="Export"/>
						<input type="submit" name="cancel" value="Cancel" />
    			</td>
			</tr>
	    </table>
	</form>
</xsl:template>


<xsl:template match="item[@type='org.dcm4chex.archive.web.maverick.tf.SRManifestModel$SRItem']">
	<tr>
		<td align="right"><xsl:value-of select="name"/>:&#160;</td>
		<td title="{name}">
			<xsl:choose>
				<xsl:when test="rows=-1">
					<input size="{size}" name="{name}" type="text" value="{value}"/>
				</xsl:when>
				<xsl:otherwise>
					<textarea rows="{rows}" cols="{size}" type="text" name="{name}">
						<xsl:value-of select="value"/>
					</textarea>
				</xsl:otherwise>
			</xsl:choose>
		</td>
   </tr>
</xsl:template>


</xsl:stylesheet>


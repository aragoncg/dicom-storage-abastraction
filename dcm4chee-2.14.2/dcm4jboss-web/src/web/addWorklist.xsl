<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="html" indent="yes" encoding="UTF-8" />
<xsl:variable name="page_title">Add worklist item</xsl:variable>
<xsl:include href  = "page.xsl" />

<xsl:template match="model">
	<form action="addWorklist.m" method="post" accept-charset="UTF-8" >
		<table border="0" cellspacing="0" cellpadding="0" width="90%">
			<tr>
				<td>
					<center>Add Worklist item for study <xsl:value-of select="studyPk"/>!</center>
	<center>        			<input type="hidden" name="studyPk" value="{studyPk}"/>
	
		<table border="0" width="500">
			<tr>
				<th title="Template" >Template</th>
				<th title="Human Perfomer" >Human Performer</th>
				<th title="Schedule date">Schedule date</th>
			</tr>	
			<tr>
				<td width="33%" align="center">
					<select size="1" name="template" title="Worklist template">
						<xsl:for-each select="templateList/item">
								<xsl:sort data-type="text" order="ascending" />
							<option>
								<xsl:attribute name="value"><xsl:value-of select="."/></xsl:attribute>
								<xsl:if test="/model/template = .">
									<xsl:attribute name="selected"/>
								</xsl:if>
								<xsl:value-of select="."/>
							</option>
						</xsl:for-each>						
					</select>
				</td>
				<td width="33%" align="center">
					<select size="1" name="humanPerformer" title="Human performer">
						<xsl:for-each select="humanPerformerList/item">
							<xsl:sort data-type="text" order="ascending" select="codeMeaning"/>
							<option>
								<xsl:attribute name="value"><xsl:value-of select="codeValue"/></xsl:attribute>
								<xsl:if test="/model/humanPerformer = codeValue">
									<xsl:attribute name="selected"/>
								</xsl:if>
								<xsl:value-of select="codeMeaning"/>
							</option>
						</xsl:for-each>						
					</select>
				</td>
				<td width="33%" align="center">
        			<input size="15" name="scheduleDate" type="text" value="{scheduleDate}"/>
        		</td>
			</tr>	
			<tr>
        		<td colspan="3" align="center">
      				<input type="submit" name="add" value="Add" />
	        		<input type="submit" name="cancel" value="Cancel" />
    			</td>
      		</tr>
	  	</table>
	</center>
       	</td>
			</tr>
		</table>
	</form>
</xsl:template>

</xsl:stylesheet>


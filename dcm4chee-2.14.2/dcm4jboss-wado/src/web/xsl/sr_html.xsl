<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  
	<xsl:output method="html" indent="yes" media-type="text/html" encoding="UTF-8"/>
  <xsl:param name="wadoURL" select="'wado'"/>
  <xsl:param name="srImageRows" />

		<!-- the stylesheet processing entry point -->
	<xsl:template match="/">
	  <xsl:apply-templates select="dataset"/>
	</xsl:template>

	<xsl:template match="dataset">
		<html>
		<head>
			<title>
			<xsl:value-of select="attr[@tag='0040A043']/item/attr[@tag='00080104']"/>
			</title>
		</head>
		<body>
		<font size="-1">
		By <xsl:value-of select="attr[@tag='00080080']"/>, Ref. Phys. <xsl:value-of select="attr[@tag='00080090']"/>
		</font>
		<br/>
		<table border="0">
		<tr><td>Patient Name:</td><td><xsl:value-of select="attr[@tag='00100010']"/>
</td></tr>
		<tr><td>Patient ID:</td><td><xsl:value-of select="attr[@tag='00100020']"/>
</td></tr>
		<tr><td>Patient Birthdate:</td><td><xsl:value-of select="attr[@tag='00100030']"/>
</td></tr>
		<tr><td>Patient Sex:</td><td><xsl:value-of select="attr[@tag='00100040']"/>
</td></tr>
		</table>
<hr/>
    
	  <xsl:apply-templates select="attr[@tag='0040A730']/item" mode="content"/>
	
		</body>
    </html>
	</xsl:template>

<!--
  Contentsequence output starts here
-->

	<xsl:template match="item" mode="content">
	  <font size="+2"><xsl:value-of select="attr[@tag='0040A043']/item/attr[@tag='00080104']"/></font>
          <xsl:apply-templates select="." mode="contentItem" />
          <br />
	</xsl:template>
	

        <!-- Displays the content in the context of a list -->
	<xsl:template match="item" mode="contentLI">
          <li><font size="+1"><xsl:value-of select="attr[@tag='0040A043']/item/attr[@tag='00080104']"/></font>
            <xsl:apply-templates select="." mode="contentItem" />
          </li>
        </xsl:template>

        <xsl:template mode="contentItem" match="item">
	  <xsl:choose>
	    <xsl:when test="attr[@tag='0040A040']='TEXT'">
             <p><xsl:value-of select="attr[@tag='0040A160']"/></p>
		</xsl:when>
	  
	  <xsl:when test="attr[@tag='0040A040']='IMAGE ' or attr[@tag='0040A040']='IMAGE'">
                <xsl:apply-templates select="attr[@tag='00081199']/item" mode="imageref"/>
		</xsl:when>
		
	  <xsl:when test="attr[@tag='0040A040']='CODE'">
             <xsl:value-of select="concat(': ',attr[@tag='0040A168']/item/attr[@tag='00080104'])"/>
	  </xsl:when>		

	  <xsl:when test="attr[@tag='0040A040']='PNAME ' or attr[@tag='0040A040']='PNAME'">
		:<xsl:value-of select="attr[@tag='0040A123']"/>
	  </xsl:when>		

	  <xsl:when test="attr[@tag='0040A040']='NUM ' or attr[@tag='0040A040']='NUM'">
             <xsl:value-of select="concat(': ',attr[@tag='0040A300']/item/attr[@tag='0040A30A'])" />
			<xsl:if test="attr[@tag='0040A300']/item/attr[@tag='004008EA']/item/attr[@tag='00080100'] != 1" > <!-- No unit (UCUM) -->
				<xsl:value-of select="concat(' ',attr[@tag='0040A300']/item/attr[@tag='004008EA']/item/attr[@tag='00080100'])"/> 
			</xsl:if>
      </xsl:when>		
				
 
	  <xsl:when test="attr[@tag='0040A040']='CONTAINER ' or attr[@tag='0040A040']='CONTAINER'">
             <ul><xsl:apply-templates select="attr[@tag='0040A730']/item" mode="contentLI"/></ul>
		</xsl:when>
		
	  <xsl:otherwise>
		<i>[<xsl:value-of select="attr[@tag='0040A040']"/>] (This Value Type is not supported yet)</i>
	  </xsl:otherwise>
	</xsl:choose>
	</xsl:template>
	

        <xsl:template match="item" mode="imageref">
		Image 
		<img align="top">
			<xsl:attribute name="src"><xsl:value-of select="$wadoURL"/>?requestType=WADO&amp;studyUID=1&amp;seriesUID=1&amp;objectUID=<xsl:value-of select="attr[@tag='00081155']"/>
				<xsl:if test="$srImageRows">&amp;rows=<xsl:value-of select="$srImageRows" /></xsl:if>
			</xsl:attribute>
		</img>
<br/>
	</xsl:template>
	
</xsl:stylesheet>



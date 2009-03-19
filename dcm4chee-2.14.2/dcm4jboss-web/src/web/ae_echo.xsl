<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:internal="urn:my-internal-data">
<xsl:output method="html" indent="yes" encoding="UTF-8"/>

   <xsl:template match="/">
	 <html>
	    <body>
			<xsl:choose>
				<xsl:when test="model/echoSucceed = 'true'">
                  <xsl:attribute name="bgcolor">white</xsl:attribute>
				</xsl:when>
				<xsl:otherwise>
                  <xsl:attribute name="bgcolor">red</xsl:attribute>
				</xsl:otherwise>
			</xsl:choose>
	    	<center>
	    		<b>Echo result:</b> <p>
	    		<xsl:value-of select="model/echoResultMsg"/>
	    		</p>
	    	</center>
		</body>
	 </html>
   </xsl:template>

   
</xsl:stylesheet>


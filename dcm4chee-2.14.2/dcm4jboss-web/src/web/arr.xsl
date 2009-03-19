<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:internal="urn:my-internal-data">

<xsl:output method="html" indent="yes" encoding="UTF-8" />
<xsl:variable name="page_title">Audit Repository</xsl:variable>
<xsl:include href  = "page.xsl" />

<xsl:template match="model">

<table border="0" width="100%" height="100%">
<tr><td>
	<iframe src="../dcm4chee-arr" width="100%" height="100%">
		IFrame is not support!
	</iframe>
</td></tr>
</table>

</xsl:template>
</xsl:stylesheet>


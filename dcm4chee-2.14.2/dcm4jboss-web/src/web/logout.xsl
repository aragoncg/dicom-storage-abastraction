<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:internal="urn:my-internal-data">
<xsl:output method="html" indent="yes" encoding="UTF-8"/>

   <xsl:template match="/">
	 <html>
	    <head>
	    	<link href="style.css" rel="stylesheet" type="text/css"/>
	    </head>
	    <body>
		<table border="0" cellspacing="0" cellpadding="0" width="100%">
		 <tr>
		  <td><img src="white48.jpg" width="100%" height="5px"/></td>
		</tr>
		<tr>
		<td background="white48.jpg">
		  <img src="white48.jpg" width="10px" height="24px"/><img src="logo.gif" alt="DCM4CHEE"/>
		</td>
		</tr>
		<tr>
		<td><img src="line.jpg" width="100%" height="20px" alt="line"/></td>
		</tr>
		</table>

	    	<center>
	    		<b><div class="text">You have logged out.</div></b> 
	    		<p>
	    		<a href="foldersubmit.m?filter=">Login</a>
	    		</p>
	    	</center>
	    </body>
	 </html>
   </xsl:template>

   
</xsl:stylesheet>


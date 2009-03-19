<?xml version="1.0" encoding="UTF-8"?>
<!-- $Id: trash.xsl 6214 2008-04-24 22:32:09Z gunterze $ -->
<!DOCTYPE stylesheet SYSTEM "translate.dtd" [
<!ENTITY trash-tpl SYSTEM "../trash-tpl.xsl">
]>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="html" indent="yes" encoding="UTF-8"/>
<xsl:variable name="page_title">&Trash;</xsl:variable>
<xsl:include href="page.xsl"/>
<xsl:include href="../modality_sel.xsl"/>
&trash-tpl;
</xsl:stylesheet>

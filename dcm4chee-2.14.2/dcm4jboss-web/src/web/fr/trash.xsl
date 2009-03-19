<?xml version="1.0" encoding="UTF-8"?>
<!-- $Id$ -->
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

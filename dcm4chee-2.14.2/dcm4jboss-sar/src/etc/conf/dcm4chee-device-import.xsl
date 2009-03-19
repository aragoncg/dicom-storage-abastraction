<xsl:stylesheet version="1.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:template match="devices">
	<dicomfile>
		<dataset>
            <attr tag="00080005" vr="CS">ISO_IR 100</attr>
			<attr tag="00400100" vr="SQ" name="Scheduled Procedure Step Sequence">
				<xsl:apply-templates select="device"/>
			</attr>
		</dataset>
	</dicomfile>
			
</xsl:template>

	<xsl:template match="device">
		<item id="{position()}">
			<attr tag="00080060" vr="CS" name="Modality" vm="1"><xsl:value-of select="@modality" /> </attr> 
			<attr tag="00400001" vr="AE" name="Scheduled Station AE Title" vm="1" ><xsl:value-of select="@aet" /></attr>
			<attr tag="00400010" vr="SH" name="Scheduled Station Name" vm="1" ><xsl:value-of select="@name" /></attr>
			<attr tag="00400008" vr="SQ" name="Scheduled Protocol Code Sequence" >
				<xsl:apply-templates select="protocol"/>
			</attr>
		</item>
	</xsl:template>

    	<xsl:template match="protocol">
		<item id="{position()}">
	        <xsl:variable name="prot" select="."/>
	        <xsl:variable name="code" select="substring-before($prot, '^')"/>
	        <xsl:variable name="meanDesign" select="substring-after($prot, '^')"/>
	        <xsl:variable name="meaning" select="substring-before($meanDesign, '^')"/>
	        <xsl:variable name="designator" select="substring-after($meanDesign, '^')"/>
			<attr tag="00080100" vr="SH" name="Code Value" vm="1" ><xsl:value-of select="$code" /> </attr> 
            <attr tag="00080102" vr="SH" pos="498" name="Coding Scheme Designator" vm="1"><xsl:value-of select="$designator" /></attr> 
            <attr tag="00080104" vr="LO" pos="514" name="Code Meaning" vm="1"><xsl:value-of select="$meaning" /></attr> 
		</item>
	</xsl:template>

</xsl:stylesheet>

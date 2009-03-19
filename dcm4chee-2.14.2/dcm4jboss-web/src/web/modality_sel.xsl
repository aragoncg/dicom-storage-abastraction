<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:template name="modalityList">
    	<xsl:param name="name" />
    	<xsl:param name="title" />
    	<xsl:param name="selected" />
    	<!-- edit input field -->
       		<input size="10" name="{$name}" type="text" value="{$selected}"/>
    	<!-- selection field - ->
		<select size="1">
			<xsl:attribute name="name"><xsl:value-of select="$name" /></xsl:attribute>
			<xsl:attribute name="title"><xsl:value-of select="$title" /></xsl:attribute>
			<option>
				<xsl:if test="$selected = ''"><xsl:attribute name="selected"/></xsl:if>
			</option>
			<option>
				<xsl:if test="$selected = 'EPS'"><xsl:attribute name="selected"/></xsl:if>EPS
			</option>
			<option>
				<xsl:if test="$selected = 'CR'"><xsl:attribute name="selected"/></xsl:if>CR
			</option>
			<option>
				<xsl:if test="$selected = 'CT'"><xsl:attribute name="selected"/></xsl:if>CT
			</option>
			<option>
				<xsl:if test="$selected = 'DX'"><xsl:attribute name="selected"/></xsl:if>DX
			</option>
			<option>
				<xsl:if test="$selected = 'ECG'"><xsl:attribute name="selected"/></xsl:if>ECG
			</option>
			<option>
				<xsl:if test="$selected = 'ES'"><xsl:attribute name="selected"/></xsl:if>ES
			</option>
			<option>
				<xsl:if test="$selected = 'XC'"><xsl:attribute name="selected"/></xsl:if>XC
			</option>
			<option>
				<xsl:if test="$selected = 'GM'"><xsl:attribute name="selected"/></xsl:if>GM
			</option>
			<option>
				<xsl:if test="$selected = 'HD'"><xsl:attribute name="selected"/></xsl:if>HD
			</option>
			<option>
				<xsl:if test="$selected = 'IO'"><xsl:attribute name="selected"/></xsl:if>IO
			</option>
			<option>
				<xsl:if test="$selected = 'IVUS'"><xsl:attribute name="selected"/></xsl:if>IVUS
			</option>
			<option>
				<xsl:if test="$selected = 'MR'"><xsl:attribute name="selected"/></xsl:if>MR
			</option>
			<option>
				<xsl:if test="$selected = 'MG'"><xsl:attribute name="selected"/></xsl:if>MG
			</option>
			<option>
				<xsl:if test="$selected = 'NM'"><xsl:attribute name="selected"/></xsl:if>NM
			</option>
			<option>
				<xsl:if test="$selected = 'OP'"><xsl:attribute name="selected"/></xsl:if>OP
			</option>
			<option>
				<xsl:if test="$selected = 'PX'"><xsl:attribute name="selected"/></xsl:if>PX
			</option>
			<option>
				<xsl:if test="$selected = 'PT'"><xsl:attribute name="selected"/></xsl:if>PT
			</option>
			<option>
				<xsl:if test="$selected = 'RF'"><xsl:attribute name="selected"/></xsl:if>RF
			</option>
			<option>
				<xsl:if test="$selected = 'RG'"><xsl:attribute name="selected"/></xsl:if>RG
			</option>
			<option>
				<xsl:if test="$selected = 'RTIMAGE'"><xsl:attribute name="selected"/></xsl:if>RTIMAGE
			</option>
			<option>
				<xsl:if test="$selected = 'SR'"><xsl:attribute name="selected"/></xsl:if>SR
			</option>
			<option>
				<xsl:if test="$selected = 'SM'"><xsl:attribute name="selected"/></xsl:if>SM
			</option>
			<option>
				<xsl:if test="$selected = 'US'"><xsl:attribute name="selected"/></xsl:if>US
			</option>
			<option>
				<xsl:if test="$selected = 'XA'"><xsl:attribute name="selected"/></xsl:if>XA
			</option>
			<option>
				<xsl:if test="$selected = 'OT'"><xsl:attribute name="selected"/></xsl:if>OT
			</option>
		</select>
		<!- - -->
	</xsl:template>
</xsl:stylesheet>
	

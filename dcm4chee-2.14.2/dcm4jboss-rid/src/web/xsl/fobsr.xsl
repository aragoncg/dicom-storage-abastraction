<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:fo="http://www.w3.org/1999/XSL/Format" version="1.0">
    <xsl:output method="xml" indent="yes" media-type="text/xml-fo"
      encoding="UTF-8"/>
    <xsl:include href="common.xsl"/>
    <xsl:variable name="SR_1" select="'1.2.840.10008.5.1.4.1.1.88.11'"/><!-- BasicTextSR -->
    <xsl:variable name="SR_2" select="'1.2.840.10008.5.1.4.1.1.88.22'"/><!-- EnhancedSR -->
    <xsl:variable name="SR_3" select="'1.2.840.10008.5.1.4.1.1.88.33'"/><!-- ComprehensiveSR -->
    <xsl:variable name="SR_4" select="'1.2.840.10008.5.1.4.1.1.88.59'"/><!-- KeyObjectSelectionDocument -->
    
    <xsl:param name="wadoURL" select="'http://localhost:8080/wado'"/>
    <xsl:param name="srImageRows" />
    <!-- the stylesheet processing entry point -->
	<xsl:template match="/">
	  <xsl:apply-templates select="dicomfile/dataset"/>
	</xsl:template>
    
    <xsl:template match="dataset">
    	<xsl:variable name="cuid" select="attr[@tag='00080016']"/>
        <fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">
            <fo:layout-master-set>
                <fo:simple-page-master master-name="page" page-height="297mm" page-width="210mm"
                    margin-left="45mm" margin-right="45mm" margin-top="20mm" margin-bottom="20mm">
                      <fo:region-before extent="3cm"/>
                      <fo:region-body margin-top="3cm"/>
                      <fo:region-after extent="1.5cm"/>
                </fo:simple-page-master>
            </fo:layout-master-set>
            <fo:page-sequence master-reference="page">
            	<fo:static-content flow-name="xsl-region-before">
          <fo:block text-align="center">
            <fo:external-graphic src="http://localhost:8080/images/logo.gif"/>
         </fo:block>
            		<fo:block font-size="20pt"  text-align="center" font-weight="bold" >
            			<xsl:value-of select="attr[@tag='0040A043']/item/attr[@tag='00080104']"/>
            		</fo:block>
            	</fo:static-content>
                <fo:flow flow-name="xsl-region-body">
            		<fo:block font-size="10pt" text-align="center">
                		By <xsl:value-of select="attr[@tag='00080080']"/>, Ref. Phys. 
							<xsl:call-template name="formatPN">
								<xsl:with-param name="pn" select="attr[@tag='00080090']"/>
							</xsl:call-template>
            		</fo:block>
            		<fo:table border-style="solid" table-layout="fixed">
						<fo:table-column column-number="1" column-width="50mm"/>
						<fo:table-column column-number="2" column-width="50mm"/>
						<fo:table-body>
							<fo:table-row>
								<fo:table-cell border-style="solid">
									<fo:block font-size="10pt" padding="3mm">Name:</fo:block>
								</fo:table-cell>
								<fo:table-cell border-style="solid">
									<fo:block font-size="10pt" padding="3mm">
										<xsl:call-template name="formatPN">
											<xsl:with-param name="pn" select="attr[@tag='00100010']"/>
										</xsl:call-template>
									</fo:block>
								</fo:table-cell>
							</fo:table-row>
							<fo:table-row>
								<fo:table-cell border-style="solid">
									<fo:block font-size="10pt" padding="3mm">Patient Name:</fo:block>
								</fo:table-cell>
								<fo:table-cell border-style="solid">
									<fo:block font-size="10pt" padding="3mm">
										<xsl:call-template name="formatPN">
											<xsl:with-param name="pn" select="attr[@tag='00100010']"/>
										</xsl:call-template>
									</fo:block>
								</fo:table-cell>
							</fo:table-row>
							<fo:table-row>
								<fo:table-cell border-style="solid">
									<fo:block font-size="10pt" padding="3mm">Patient ID:</fo:block>
								</fo:table-cell>
								<fo:table-cell border-style="solid">
									<fo:block font-size="10pt" padding="3mm"><xsl:value-of select="attr[@tag='00100020']"/></fo:block>
								</fo:table-cell>
							</fo:table-row>
							<fo:table-row>
								<fo:table-cell border-style="solid">
									<fo:block font-size="10pt" padding="3mm">Patient Birthdate:</fo:block>
								</fo:table-cell>
								<fo:table-cell border-style="solid">
									<fo:block font-size="10pt" padding="3mm">
										<xsl:call-template name="formatDate">
											<xsl:with-param name="date" select="attr[@tag='00100030']"/>
										</xsl:call-template>
									</fo:block>
								</fo:table-cell>
							</fo:table-row>
							<fo:table-row>
								<fo:table-cell border-style="solid">
									<fo:block font-size="10pt" padding="3mm">Patient Sex:</fo:block>
								</fo:table-cell>
								<fo:table-cell border-style="solid">
									<fo:block font-size="10pt" padding="3mm"><xsl:value-of select="attr[@tag='00100040']"/></fo:block>
								</fo:table-cell>
							</fo:table-row>
						</fo:table-body>
					</fo:table>
					<xsl:choose>
						<xsl:when test="$cuid=$SR_1 or $cuid=$SR_2 or $cuid=$SR_3 or $cuid=$SR_4">
		  					<xsl:apply-templates select="attr[@tag='0040A730']/item" mode="content"/>
						</xsl:when>
						<xsl:when test="$cuid='1.2.840.10008.5.1.4.1.1.11.1'"><!-- GrayscaleSoftcopyPresentationStateStorage -->
						</xsl:when>
						<xsl:otherwise> <!-- image -->
    		            		<fo:block font-size="10pt" text-align="center">
                        	        <fo:external-graphic >
                        	            <xsl:attribute name="src">url(<xsl:value-of select="$wadoURL"/>?requestType=WADO&amp;studyUID=1&amp;seriesUID=1&amp;objectUID=<xsl:value-of select="attr[@tag='00080018']"/>)</xsl:attribute>
                        	        </fo:external-graphic >
    		            		</fo:block>

						</xsl:otherwise>
		  			</xsl:choose>
			   </fo:flow>
            </fo:page-sequence>
        </fo:root>
    </xsl:template>

<!--
  Contentsequence output starts here
-->
	<xsl:template match="item" mode="content">

	<fo:block font-size="12pt" font-weight="bold"><xsl:value-of select="attr[@tag='0040A043']/item/attr[@tag='00080104']"/></fo:block>

	<fo:list-block>
	<fo:list-item>
		<fo:list-item-label end-indent="label-end()">
			<fo:block>-</fo:block>
		</fo:list-item-label>
		<fo:list-item-body start-indent="body-start()">
		  <xsl:choose>
			  <xsl:when test="attr[@tag='0040A040']='TEXT'">
				<fo:block font-size="12pt"><xsl:value-of select="attr[@tag='0040A160']"/></fo:block>
			              <xsl:apply-templates select="attr[@tag='0040A730']/item" mode="content"/>
			  </xsl:when>

			  <xsl:when test="attr[@tag='0040A040']='IMAGE '">
			              <xsl:apply-templates select="attr[@tag='00081199']/item" mode="image"/>
			  </xsl:when>
	
			  <xsl:when test="attr[@tag='0040A040']='CODE'">
				<fo:block font-size="12pt"><xsl:value-of select="attr[@tag='0040A168']/item/attr[@tag='00080104']"/></fo:block>
		      </xsl:when>		
		      
			  <xsl:when test="attr[@tag='0040A040']='PNAME '">
				<fo:block font-size="12pt">
					<xsl:call-template name="formatPN">
						<xsl:with-param name="pn" select="attr[@tag='0040A123']"/>
					</xsl:call-template>
				</fo:block>
			  </xsl:when>		

	  		  <xsl:when test="attr[@tag='0040A040']='NUM '">
    			<xsl:apply-templates select="attr[@tag='0040A300']/item" mode="measurement"/>
			  </xsl:when>		

			  <xsl:when test="attr[@tag='0040A040']='CONTAINER '">
    			<xsl:apply-templates select="attr[@tag='0040A730']/item" mode="content"/>
			  </xsl:when>
		
	  		  <xsl:otherwise>
				<fo:block font-size="12pt"><xsl:value-of select="attr[@tag='0040A040']"/> (Value Type not supported yet)</fo:block>
	  		  </xsl:otherwise>
		  </xsl:choose>
		</fo:list-item-body>
	</fo:list-item>
	</fo:list-block>
	
	</xsl:template>
 
	<xsl:template match="item" mode="image">
	    <fo:block>
	        <fo:block>IMAGE</fo:block>
	        <fo:external-graphic >
	            <xsl:attribute name="src">url(<xsl:value-of select="$wadoURL"/>?requestType=WADO&amp;studyUID=1&amp;seriesUID=1&amp;objectUID=<xsl:value-of select="attr[@tag='00081155']"/>
					<xsl:if test="$srImageRows">&amp;rows=<xsl:value-of select="$srImageRows" /></xsl:if>)
	            </xsl:attribute>
	        </fo:external-graphic >
	    </fo:block>

	</xsl:template>
	    
 	<xsl:template match="item" mode="measurement">
	    <fo:block>
			<xsl:value-of select="attr[@tag='0040A30A']"/>
			<xsl:text> </xsl:text>
			<xsl:if test="attr[@tag='004008EA']/item/attr[@tag='00080100'] != 1" > <!-- No unit (UCUM) -->
				<xsl:value-of select="attr[@tag='004008EA']/item/attr[@tag='00080100']"/> 
			</xsl:if>
	    </fo:block>

	</xsl:template>
    
</xsl:stylesheet>

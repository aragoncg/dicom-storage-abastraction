<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:output indent="yes" method="xml"/>
    <xsl:param name="SendingApplication">SendingApplication</xsl:param>
    <xsl:param name="SendingFacility">SendingFacility</xsl:param>
    <xsl:param name="ReceivingApplication">ReceivingApplication</xsl:param>
    <xsl:param name="ReceivingFacility">ReceivingFacility</xsl:param>
    
    <xsl:template match="/dataset">
        <hl7>
            <MSH fieldDelimiter="|" componentDelimiter="^" repeatDelimiter="~" escapeDelimiter="\" subcomponentDelimiter="&amp;">
                <field><xsl:value-of select="$SendingApplication"/></field>
                <field><xsl:value-of select="$SendingFacility"/></field>
                <field><xsl:value-of select="$ReceivingApplication"/></field>
                <field><xsl:value-of select="$ReceivingFacility"/></field>
                <field/> <!-- Date/time of Message -->
                <field/> <!-- Security -->
                <field>ORM<component>O01</component></field>
                <field/> <!-- Message Control ID -->
                <field>P</field>
                <field>2.3</field>
                <field/> <!-- Sequence Number -->
                <field/> <!-- Continuation Pointer -->
                <field/> <!-- Accept Acknowledgment Type -->
                <field/> <!-- Application Acknowledgment Type -->
                <field/> <!-- Country Code -->
                <field>8859/1</field>
            </MSH>
            <PID>
                <field/>
                <field/>
                <field><xsl:value-of select="normalize-space(attr[@tag='00100020'])"/>
                    <component/>
                    <component/>
                    <component><xsl:value-of select="normalize-space(attr[@tag='00100021'])"/></component>
                </field>
                <field/>
                <field>
                    <xsl:call-template name="pn2xpn">
                        <xsl:with-param name="pn" select="normalize-space(attr[@tag='00100010'])"/>
                    </xsl:call-template>
                 </field>
                <field/>
                <field><xsl:value-of select="normalize-space(attr[@tag='00100030'])"/></field>
                <field><xsl:value-of select="normalize-space(attr[@tag='00100040'])"/></field>
            </PID>
            <xsl:apply-templates select="attr[@tag='00400270']/item"/>
        </hl7>
    </xsl:template>
    
    <xsl:template match="item">
        <xsl:variable name="accno" select="normalize-space(attr[@tag='00080050'])"/>
         <OBC>
            <field>SC</field>
            <!-- Use Accession Number as Order Placer Number -->
            <field><xsl:value-of select="$accno"/></field>
            <!-- Use Accession Number as Order Filler Number -->
            <field><xsl:value-of select="$accno"/></field>
            <field/>
            <!-- Order Status -->
            <field>
                <xsl:call-template name="ppsstatus2orderstatus">
                    <xsl:with-param name="ppsstatus" select="normalize-space(../../attr[@tag='00400252'])"/>
                </xsl:call-template>
            </field>
        </OBC>
        <OBR>
            <field>1</field>
            <!-- Use Accession Number as Order Placer Number -->
            <field><xsl:value-of select="$accno"/></field>
            <!-- Use Accession Number as Order Filler Number -->
            <field><xsl:value-of select="$accno"/></field>
            <field>
                <xsl:call-template name="code2ce">
                    <xsl:with-param name="code" select="attr[@tag='00081032']/item"/>
                </xsl:call-template>                
            </field>
        </OBR>
        <ZDS>
            <field><xsl:value-of select="normalize-space(attr[@tag='0020000D'])"/>
                <component><xsl:value-of select="$SendingApplication"/></component>
                <component>Application</component>
                <component>DICOM</component>
            </field>
        </ZDS>
    </xsl:template>
    
    <xsl:template name="pn2xpn">
        <xsl:param name="pn"/>
        <xsl:variable name="fn" select="substring-before($pn,'^')"/>
        <xsl:choose>
            <xsl:when test="$fn">
                <xsl:value-of select="$fn"/>
                <component>
                    <xsl:variable name="wofn" select="substring-after($pn,'^')"/>
                    <xsl:variable name="vn" select="substring-before($wofn,'^')"/>
                    <xsl:choose>
                        <xsl:when test="$vn">
                            <xsl:value-of select="$vn"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="$wofn"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </component>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$pn"/>
            </xsl:otherwise>
        </xsl:choose>             
    </xsl:template>
    
    <xsl:template name="ppsstatus2orderstatus">
        <xsl:param name="ppsstatus"/>
        <xsl:choose>
            <xsl:when test="$ppsstatus = 'COMPLETED'">CM</xsl:when>
            <xsl:when test="$ppsstatus = 'DISCONTINUED'">DC</xsl:when>
            <xsl:when test="$ppsstatus = 'IN PROGRESS'">IP</xsl:when>
         </xsl:choose>             
    </xsl:template>
    
    
    <xsl:template name="code2ce">
        <xsl:param name="code"/>
        <xsl:value-of select="normalize-space($code/attr[@tag='00080100'])"/>
        <component>
            <xsl:value-of select="normalize-space($code/attr[@tag='00080104'])"/>
        </component>
        <component>
            <xsl:value-of select="normalize-space($code/attr[@tag='00080102'])"/>
        </component>
    </xsl:template>
    
</xsl:stylesheet>

<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:variable name="ssn-issuer">SSN</xsl:variable>
  <xsl:template name="attr">
    <xsl:param name="tag"/>
    <xsl:param name="vr"/>
    <xsl:param name="val"/>
    <xsl:if test="normalize-space($val)">
      <attr tag="{$tag}" vr="{$vr}">
        <xsl:call-template name="maskQuotes">
          <xsl:with-param name="str" select="$val"/>
        </xsl:call-template>
      </attr>
    </xsl:if>
  </xsl:template>
  <xsl:template name="maskQuotes">
    <xsl:param name="str"/>
    <xsl:if test="$str != '&quot;&quot;'">
      <xsl:value-of select="$str"/>
    </xsl:if>
  </xsl:template>
  <xsl:template name="cx2attrs">
    <xsl:param name="idtag"/>
    <xsl:param name="istag"/>
    <xsl:param name="cx"/>
    <attr tag="{$idtag}" vr="LO">
      <xsl:value-of select="string($cx/text())"/>
    </attr>
    <attr tag="{$istag}" vr="LO">
      <xsl:value-of select="string($cx/component[3]/text())"/>
    </attr>
  </xsl:template>
  <xsl:template name="ei2attr">
    <xsl:param name="tag"/>
    <xsl:param name="ei"/>
    <attr tag="{$tag}" vr="LO">
      <xsl:value-of select="string($ei/text())"/>
      <xsl:text>^</xsl:text>
      <xsl:value-of select="string($ei/component[1]/text())"/>
    </attr>
  </xsl:template>
  <xsl:template name="attrDA">
    <xsl:param name="tag"/>
    <xsl:param name="val"/>
    <xsl:variable name="str" select="normalize-space($val)"/>
    <xsl:if test="$str">
      <attr tag="{$tag}" vr="DA">
        <xsl:if test="$str != '&quot;&quot;'">
          <xsl:value-of select="substring($str,1,8)"/>
        </xsl:if>
      </attr>
    </xsl:if>
  </xsl:template>
  <xsl:template name="attrDATM">
    <xsl:param name="datag"/>
    <xsl:param name="tmtag"/>
    <xsl:param name="val"/>
    <xsl:variable name="str" select="normalize-space($val)"/>
    <xsl:if test="$str">
      <attr tag="{$datag}" vr="DA">
        <xsl:if test="$str != '&quot;&quot;'">
          <xsl:value-of select="substring($str,1,8)"/>
        </xsl:if>
      </attr>
      <attr tag="{$tmtag}" vr="TM">
        <xsl:if test="$str != '&quot;&quot;'">
          <xsl:variable name="tm" select="substring($str,9)"/>
          <!-- Skip Time Zone -->
          <xsl:variable name="tm_plus" select="substring-before($tm,'+')"/>
          <xsl:variable name="tm_minus" select="substring-before($tm,'-')"/>
          <xsl:choose>
            <xsl:when test="$tm_plus">
                <xsl:value-of select="$tm_plus"/>
            </xsl:when>
            <xsl:when test="$tm_minus">
                <xsl:value-of select="$tm_minus"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$tm"/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:if>
      </attr>
    </xsl:if>
  </xsl:template>
  <xsl:template name="xpn2pnAttr">
    <xsl:param name="tag"/>
    <xsl:param name="xpn"/>
    <xsl:param name="xpn25" select="$xpn/component"/>
    <xsl:call-template name="pnAttr">
      <xsl:with-param name="tag" select="$tag"/>
      <xsl:with-param name="val" select="string($xpn/text())"/>
      <xsl:with-param name="fn" select="string($xpn/text())"/>
      <xsl:with-param name="gn" select="string($xpn25[1]/text())"/>
      <xsl:with-param name="mn" select="string($xpn25[2]/text())"/>
      <xsl:with-param name="ns" select="string($xpn25[3]/text())"/>
      <xsl:with-param name="np" select="string($xpn25[4]/text())"/>
      <xsl:with-param name="deg" select="string($xpn25[5]/text())"/>
    </xsl:call-template>
  </xsl:template>
  <xsl:template name="cn2pnAttr">
    <xsl:param name="tag"/>
    <xsl:param name="cn"/>
    <xsl:param name="cn26" select="$cn/component"/>
    <xsl:call-template name="pnAttr">
      <xsl:with-param name="tag" select="$tag"/>
      <xsl:with-param name="val" select="string($cn/text())"/>
      <xsl:with-param name="fn" select="string($cn26[1]/text())"/>
      <xsl:with-param name="gn" select="string($cn26[2]/text())"/>
      <xsl:with-param name="mn" select="string($cn26[3]/text())"/>
      <xsl:with-param name="ns" select="string($cn26[4]/text())"/>
      <xsl:with-param name="np" select="string($cn26[5]/text())"/>
      <xsl:with-param name="deg" select="string($cn26[6]/text())"/>
    </xsl:call-template>
  </xsl:template>
  <xsl:template name="pnAttr">
    <xsl:param name="tag"/>
    <xsl:param name="val"/>
    <xsl:param name="fn"/>
    <xsl:param name="gn"/>
    <xsl:param name="mn"/>
    <xsl:param name="np"/>
    <xsl:param name="ns"/>
    <xsl:param name="deg"/>
    <xsl:if test="$val">
      <attr tag="{$tag}" vr="PN">
        <xsl:if test="$val != '&quot;&quot;'">
          <xsl:call-template name="maskQuotes">
            <xsl:with-param name="str" select="$fn"/>
          </xsl:call-template>
          <xsl:text>^</xsl:text>
          <xsl:call-template name="maskQuotes">
            <xsl:with-param name="str" select="$gn"/>
          </xsl:call-template>
          <xsl:text>^</xsl:text>
          <xsl:call-template name="maskQuotes">
            <xsl:with-param name="str" select="$mn"/>
          </xsl:call-template>
          <xsl:text>^</xsl:text>
          <xsl:call-template name="maskQuotes">
            <xsl:with-param name="str" select="$np"/>
          </xsl:call-template>
          <xsl:text>^</xsl:text>
          <xsl:choose>
	          <xsl:when test="$ns and $ns != '&quot;&quot;'">
	          	<xsl:value-of select="$ns"/>
	          	<xsl:if test="$deg and $deg != '&quot;&quot;'">
	          		<xsl:text> </xsl:text><xsl:value-of select="$deg"/>
	          	</xsl:if>
	          </xsl:when>
	          <xsl:otherwise>
		          <xsl:call-template name="maskQuotes">
		            <xsl:with-param name="str" select="$deg"/>
		          </xsl:call-template>
	          </xsl:otherwise>
	      </xsl:choose>
        </xsl:if>
      </attr>
    </xsl:if>
  </xsl:template>
  <xsl:template name="codeItem">
    <xsl:param name="sqtag"/>
    <xsl:param name="code"/>
    <xsl:param name="scheme"/>
    <xsl:param name="meaning"/>
    <xsl:if test="normalize-space($code)">
      <attr tag="{$sqtag}" vr="SQ">
        <item>
          <!-- Code Value -->
          <attr tag="00080100" vr="SH">
            <xsl:value-of select="$code"/>
          </attr>
          <!-- Coding Scheme Designator -->
          <attr tag="00080102" vr="SH">
            <xsl:value-of select="$scheme"/>
          </attr>
          <!-- Code Meaning -->
          <attr tag="00080104" vr="LO">
            <xsl:value-of select="$meaning"/>
          </attr>
        </item>
      </attr>
    </xsl:if>
  </xsl:template>
  <xsl:template match="PID">
    <!-- Patient Name -->
    <xsl:call-template name="xpn2pnAttr">
      <xsl:with-param name="tag" select="'00100010'"/>
      <xsl:with-param name="xpn" select="field[5]"/>
    </xsl:call-template>
    <!-- Patient ID -->
    <xsl:call-template name="cx2attrs">
      <xsl:with-param name="idtag" select="'00100020'"/>
      <xsl:with-param name="istag" select="'00100021'"/>
      <xsl:with-param name="cx" select="field[3]"/>
    </xsl:call-template>
    <!-- Patient Birth Date -->
    <xsl:call-template name="attrDA">
      <xsl:with-param name="tag" select="'00100030'"/>
      <xsl:with-param name="val" select="string(field[7]/text())"/>
    </xsl:call-template>
    <!-- Patient Sex -->
    <xsl:call-template name="attr">
      <xsl:with-param name="tag" select="'00100040'"/>
      <xsl:with-param name="vr" select="'CS'"/>
      <xsl:with-param name="val" select="string(field[8]/text())"/>
    </xsl:call-template>
    <!-- Other Patient Names -->
    <!-- TODO: extend for multiple Other Patient Names -->
    <xsl:call-template name="xpn2pnAttr">
      <xsl:with-param name="tag" select="'00101001'"/>
      <xsl:with-param name="xpn" select="field[9]"/>
    </xsl:call-template>
    <!-- Patient's Mother's Birth Name -->
    <xsl:call-template name="xpn2pnAttr">
      <xsl:with-param name="tag" select="'00101060'"/>
      <xsl:with-param name="xpn" select="field[6]"/>
    </xsl:call-template>
    <!-- Map SSN Number to Other Patient ID Sequence Item -->
    <xsl:variable name="ssn" select="field[19]"/>
    <xsl:if test="normalize-space($ssn)">
      <attr tag="00101002" vr="SQ">
        <item>
          <!-- Patient ID -->
          <attr tag="00100020" vr="LO">
            <xsl:value-of select="$ssn"/>
          </attr>
          <!-- Issuer Of Patient ID -->
          <attr tag="00100021" vr="LO">
            <xsl:value-of select="$ssn-issuer"/>
          </attr>
        </item>
      </attr>
    </xsl:if>    
  </xsl:template>
</xsl:stylesheet>

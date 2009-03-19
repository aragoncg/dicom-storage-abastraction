<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="xml" indent="yes"/>
  <xsl:include href="common.xsl"/>
  <xsl:param name="VerifyingOrganization">Verifying Organization</xsl:param>
  <xsl:template match="/hl7">
    <dataset>
      <xsl:call-template name="const-attrs"/>
      <!--SOP Instance UID-->
      <attr tag="00080018" vr="UI">
        <xsl:value-of
          select="OBX[field[3]/component='SR Instance UID']/field[5]"/>
      </attr>
      <xsl:apply-templates select="PID"/>
      <xsl:apply-templates select="OBR"/>
      <!--Current Requested Procedure Evidence Sequence-->
      <attr tag="0040A375" vr="SQ">
        <xsl:apply-templates
          select="OBX[field[3]/component='Study Instance UID']" mode="refstudy"
        />
      </attr>
      <!--Content Sequence-->
      <attr tag="0040A730" vr="SQ">
        <xsl:call-template name="const-obsctx"/>
        <xsl:apply-templates select="OBR" mode="obsctx"/>
        <xsl:apply-templates select="OBX[field[3]/component='SR Text']"
          mode="txt"/>
        <xsl:if test="OBX[field[3]/component='SOP Instance UID']">
          <item>
            <!--Relationship Type-->
            <attr tag="0040A010" vr="CS">CONTAINS</attr>
            <!--Value Type-->
            <attr tag="0040A040" vr="CS">CONTAINER</attr>
            <!--Concept Name Code Sequence-->
            <attr tag="0040A043">
              <item>
                <!--Code Value-->
                <attr tag="00080100" vr="SH">121180</attr>
                <!--Coding Scheme Designator-->
                <attr tag="00080102" vr="SH">DCM</attr>
                <!--Code Meaning-->
                <attr tag="00080104" vr="LO">Key Images</attr>
              </item>
            </attr>
            <!--Continuity Of Content-->
            <attr tag="0040A050" vr="CS">SEPARATE</attr>
            <!--Content Sequence-->
            <attr tag="0040A730" vr="SQ">
              <xsl:apply-templates
                select="OBX[field[3]/component='SOP Instance UID']" mode="img"/>
            </attr>
          </item>
        </xsl:if>
      </attr>
    </dataset>
  </xsl:template>
  <xsl:template name="const-attrs">
    <!-- Specific Character Set -->
    <attr tag="00080005" vr="CS">ISO_IR 100</attr>
    <!--SOP Class UID-->
    <attr tag="00080016" vr="UI">1.2.840.10008.5.1.4.1.1.88.11</attr>
    <!--Study Date-->
    <attr tag="00080020" vr="DA"/>
    <!--Study Time-->
    <attr tag="00080030" vr="TM"/>
    <!--Accession Number-->
    <attr tag="00080050" vr="SH"/>
    <!--Modality-->
    <attr tag="00080060" vr="CS">SR</attr>
    <!--Manufacturer-->
    <attr tag="00080070" vr="LO"/>
    <!--Referring Physician's Name-->
    <attr tag="00080090" vr="PN"/>
    <!--Referenced Performed Procedure Step Sequence-->
    <attr tag="00081111" vr="SQ"/>
    <!--Study ID-->
    <attr tag="00200010" vr="SH"/>
    <!--Series Number-->
    <attr tag="00200011" vr="IS"/>
    <!--Instance Number-->
    <attr tag="00200013" vr="IS">1</attr>
    <!--Value Type-->
    <attr tag="0040A040" vr="CS">CONTAINER</attr>
    <!--Concept Name Code Sequence-->
    <attr tag="0040A043" vr="SQ">
      <item>
        <!--Code Value-->
        <attr tag="00080100" vr="SH">11528-7</attr>
        <!--Coding Scheme Designator-->
        <attr tag="00080102" vr="SH">LN</attr>
        <!--Code Meaning-->
        <attr tag="00080104" vr="LO">Radiology Report</attr>
      </item>
    </attr>
    <!--Continuity Of Content-->
    <attr tag="0040A050" vr="CS">SEPARATE</attr>
    <!--Completion Flag-->
    <attr tag="0040A491" vr="CS">COMPLETE</attr>
    <!--Verification Flag-->
    <attr tag="0040A493" vr="CS">VERIFIED</attr>
    <!--Content Template Sequence-->
    <attr tag="0040A504" vr="SQ"/>
  </xsl:template>
  <xsl:template match="OBR">
    <!--Content Date/Time-->
    <xsl:call-template name="attrDATM">
      <xsl:with-param name="datag">00080023</xsl:with-param>
      <xsl:with-param name="tmtag">00080033</xsl:with-param>
      <xsl:with-param name="val" select="field[7]"/>
    </xsl:call-template>
    <!-- Take Study Instance UID from first referenced Image - if available -->
    <xsl:variable name="suid"
      select="normalize-space(../OBX[field[3]/component='Study Instance UID'][1]/field[5])"/>
    <!-- Study Instance UID -->
    <attr tag="0020000D" vr="UI">
      <xsl:value-of select="$suid"/>
    </attr>
    <!--Referenced Request Sequence-->
    <attr tag="0040A370" vr="SQ">
      <item>
        <!--Accession Number-->
        <attr tag="00080050" vr="SH"/>
        <!--Referenced Study Sequence-->
        <attr tag="00081110" vr="SQ"/>
        <!--Study Instance UID-->
        <xsl:value-of select="$suid"/>
        <!--Requested Procedure Description-->
        <attr tag="00321060" vr="LO">
          <xsl:value-of select="field[4]/component"/>
        </attr>
        <!--Requested Procedure Code Sequence-->
        <attr tag="00321064" vr="SQ">
          <item>
            <!--Code Value-->
            <attr tag="00080100" vr="SH">
              <xsl:value-of select="field[4]"/>
            </attr>
            <!--Coding Scheme Designator-->
            <attr tag="00080102" vr="SH">
              <xsl:value-of select="field[4]/component[2]"/>
            </attr>
            <!--Code Meaning-->
            <attr tag="00080104" vr="LO">
              <xsl:value-of select="field[4]/component"/>
            </attr>
          </item>
        </attr>
        <!--Requested Procedure ID-->
        <attr tag="00401001" vr="SH"/>
        <!--Placer Order Number / Imaging Service Request-->
        <attr tag="00402016" vr="LO">
          <xsl:value-of select="field[2]"/>
        </attr>
        <!--Filler Order Number / Imaging Service Request-->
        <attr tag="00402017" vr="LO">
          <xsl:value-of select="field[3]"/>
        </attr>
      </item>
    </attr>
    <!-- Verifying Observer Sequence -->
    <attr tag="0040A073" vr="SQ">
      <item>
        <!-- Verifying Organization -->
        <attr tag="0040A027" vr="LO">
          <xsl:value-of select="$VerifyingOrganization"/>
        </attr>
        <!-- Verification DateTime -->
        <attr tag="0040A030" vr="DT">
          <xsl:value-of select="field[7]"/>
        </attr>
        <!-- Verifying Observer Name -->
        <xsl:choose>
          <xsl:when test="field[32]/component">
            <xsl:call-template name="cn2pnAttr">
              <xsl:with-param name="tag" select="'0040A075'"/>
              <xsl:with-param name="cn" select="field[32]"/>
            </xsl:call-template>
          </xsl:when>
          <xsl:otherwise>
              <attr tag="0040A075" vr="PN">UNKOWN</attr>
          </xsl:otherwise>
        </xsl:choose>       
        <!-- Verifying Observer Identification Code Sequence -->
        <attr tag="0040A088" vr="SQ"/>
      </item>
    </attr>
  </xsl:template>
  <xsl:template name="const-obsctx">
    <item>
      <!--Relationship Type-->
      <attr tag="0040A010" vr="CS">HAS CONCEPT MOD</attr>
      <!--Value Type-->
      <attr tag="0040A040" vr="CS">CODE</attr>
      <!--Concept Name Code Sequence-->
      <attr tag="0040A043" vr="SQ">
        <item>
          <!--Code Value-->
          <attr tag="00080100" vr="SH">121049</attr>
          <!--Coding Scheme Designator-->
          <attr tag="00080102" vr="SH">DCM</attr>
          <!--Code Meaning-->
          <attr tag="00080104" vr="LO">Language of Content Item and
          Descendants</attr>
        </item>
      </attr>
      <!--Concept Code Sequence-->
      <attr tag="0040A168" vr="SQ">
        <item>
          <!--Code Value-->
          <attr tag="00080100" vr="SH">eng</attr>
          <!--Coding Scheme Designator-->
          <attr tag="00080102" vr="SH">ISO639_2</attr>
          <!--Code Meaning-->
          <attr tag="00080104" vr="LO">English</attr>
        </item>
      </attr>
    </item>
  </xsl:template>
  <xsl:template match="OBR" mode="obsctx">
    <xsl:if test="field[32]/component">
      <item>
        <!--Relationship Type-->
        <attr tag="0040A010" vr="CS">HAS OBS CONTEXT</attr>
        <!--Value Type-->
        <attr tag="0040A040" vr="CS">PNAME</attr>
        <!--Concept Name Code Sequence-->
        <attr tag="0040A043" vr="SQ">
          <item>
            <!--Code Value-->
            <attr tag="00080100" vr="SH">121008</attr>
            <!--Coding Scheme Designator-->
            <attr tag="00080102" vr="SH">DCM</attr>
            <!--Code Meaning-->
            <attr tag="00080104" vr="LO">Person Observer Name</attr>
          </item>
        </attr>
        <!--Person Name-->
        <xsl:call-template name="cn2pnAttr">
          <xsl:with-param name="tag" select="'0040A123'"/>
          <xsl:with-param name="cn" select="field[32]"/>
        </xsl:call-template>
      </item>
    </xsl:if>
    <item>
      <!--Relationship Type-->
      <attr tag="0040A010" vr="CS">HAS OBS CONTEXT</attr>
      <!--Value Type-->
      <attr tag="0040A040" vr="CS">CODE</attr>
      <!--Concept Name Code Sequence-->
      <attr tag="0040A043" vr="SQ">
        <item>
          <attr tag="00080100" vr="SH">121023</attr>
          <!--Coding Scheme Designator-->
          <attr tag="00080102" vr="SH">DCM</attr>
          <!--Code Meaning-->
          <attr tag="00080104" vr="LO">Procedure Code</attr>
        </item>
      </attr>
      <!--Concept Code Sequence-->
      <attr tag="0040A168" vr="SQ">
        <item>
          <!--Code Value-->
          <attr tag="00080100" vr="SH">
            <xsl:value-of select="field[4]"/>
          </attr>
          <!--Coding Scheme Designator-->
          <attr tag="00080102" vr="SH">
            <xsl:value-of select="field[4]/component[2]"/>
          </attr>
          <!--Code Meaning-->
          <attr tag="00080104" vr="LO">
            <xsl:value-of select="field[4]/component"/>
          </attr>
        </item>
      </attr>
    </item>
  </xsl:template>
  <xsl:template match="OBX" mode="refstudy">
    <xsl:variable name="suid">
      <xsl:value-of select="field[5]"/>
    </xsl:variable>
    <xsl:if test="not(preceding-sibling::*[field[5]=$suid])">
      <item>
        <!-- Study Instance UID -->
        <attr tag="0020000D" vr="UI">
          <xsl:value-of select="$suid"/>
        </attr>
        <!-- >Referenced Series Sequence (0008,1115) -->
        <attr tag="00081115" vr="SQ">
          <xsl:apply-templates
            select="../OBX[field[5]=$suid]/following-sibling::*[1]"
            mode="refseries"/>
        </attr>
      </item>
    </xsl:if>
  </xsl:template>
  <xsl:template match="OBX" mode="refseries">
    <xsl:variable name="suid">
      <xsl:value-of select="field[5]"/>
    </xsl:variable>
    <xsl:if test="not(preceding-sibling::*[field[5]=$suid])">
      <item>
        <!-- Series Instance UID -->
        <attr tag="0020000E" vr="UI">
          <xsl:value-of select="$suid"/>
        </attr>
        <!-- Referenced SOP Sequence -->
        <attr tag="00081199" vr="SQ">
          <xsl:apply-templates
            select="../OBX[field[5]=$suid]/following-sibling::*[1]"
            mode="refsop"/>
        </attr>
      </item>
    </xsl:if>
  </xsl:template>
  <xsl:template match="OBX" mode="refsop">
    <item>
      <!--Referenced SOP Class UID-->
      <attr tag="00081150" vr="UI">
        <xsl:value-of select="following-sibling::*[1]/field[5]"/>
      </attr>
      <!--Referenced SOP Instance UID-->
      <attr tag="00081155" vr="UI">
        <xsl:value-of select="field[5]"/>
      </attr>
    </item>
  </xsl:template>
  <xsl:template match="OBX" mode="img">
    <item>
      <!--Referenced SOP Sequence-->
      <attr tag="00081199" vr="SQ">
        <item>
          <!--Referenced SOP Class UID-->
          <attr tag="00081150" vr="UI">
            <xsl:value-of select="following-sibling::*[1]/field[5]"/>
          </attr>
          <!--Referenced SOP Instance UID-->
          <attr tag="00081155" vr="UI">
            <xsl:value-of select="field[5]"/>
          </attr>
        </item>
      </attr>
      <!--Relationship Type-->
      <attr tag="0040A010" vr="CS">CONTAINS</attr>
      <!--Value Type-->
      <attr tag="0040A040" vr="CS">IMAGE</attr>
    </item>
  </xsl:template>
  <xsl:template match="OBX" mode="txt">
    <xsl:variable name="text" select="field[5]"/>
    <xsl:choose>
      <xsl:when test="starts-with($text, 'History')">
        <xsl:call-template name="text">
          <xsl:with-param name="hcode">121060</xsl:with-param>
          <xsl:with-param name="hname">History</xsl:with-param>
          <xsl:with-param name="ecode">121060</xsl:with-param>
          <xsl:with-param name="ename">History</xsl:with-param>
          <xsl:with-param name="text" select="substring($text,9)"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:when test="starts-with($text, 'Findings')">
        <xsl:call-template name="text">
          <xsl:with-param name="hcode">121070</xsl:with-param>
          <xsl:with-param name="hname">Findings</xsl:with-param>
          <xsl:with-param name="ecode">121071</xsl:with-param>
          <xsl:with-param name="ename">Finding</xsl:with-param>
          <xsl:with-param name="text" select="substring($text,10)"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:when test="starts-with($text, 'Conclusions')">
        <xsl:call-template name="text">
          <xsl:with-param name="hcode">121076</xsl:with-param>
          <xsl:with-param name="hname">Conclusions</xsl:with-param>
          <xsl:with-param name="ecode">121077</xsl:with-param>
          <xsl:with-param name="ename">Conclusion</xsl:with-param>
          <xsl:with-param name="text" select="substring($text,13)"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="text">
          <xsl:with-param name="text" select="$text"/>
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template name="text">
    <xsl:param name="hcode">121070</xsl:param>
    <xsl:param name="hname">Findings</xsl:param>
    <xsl:param name="ecode">121071</xsl:param>
    <xsl:param name="ename">Finding</xsl:param>
    <xsl:param name="text"/>
    <item>
      <!--Relationship Type-->
      <attr tag="0040A010" vr="CS">CONTAINS</attr>
      <!--Value Type-->
      <attr tag="0040A040" vr="CS">CONTAINER</attr>
      <!--Concept Name Code Sequence-->
      <attr tag="0040A043">
        <item>
          <!--Code Value-->
          <attr tag="00080100" vr="SH">
            <xsl:value-of select="$hcode"/>
          </attr>
          <!--Coding Scheme Designator-->
          <attr tag="00080102" vr="SH">DCM</attr>
          <!--Code Meaning-->
          <attr tag="00080104" vr="LO">
            <xsl:value-of select="$hname"/>
          </attr>
        </item>
      </attr>
      <!--Continuity Of Content-->
      <attr tag="0040A050" vr="CS">SEPARATE</attr>
      <!--Content Sequence-->
      <attr tag="0040A730" vr="SQ">
        <item>
          <!--Relationship Type-->
          <attr tag="0040A010" vr="CS">CONTAINS</attr>
          <!--Value Type-->
          <attr tag="0040A040" vr="CS">TEXT</attr>
          <!--Concept Name Code Sequence-->
          <attr tag="0040A043" vr="SQ">
            <item>
              <!--Code Value-->
              <attr tag="00080100" vr="SH">
                <xsl:value-of select="$ecode"/>
              </attr>
              <!--Coding Scheme Designator-->
              <attr tag="00080102" vr="SH">DCM</attr>
              <!--Code Meaning-->
              <attr tag="00080104" vr="LO">
                <xsl:value-of select="$ename"/>
              </attr>
            </item>
          </attr>
          <!--Text Value-->
          <attr tag="0040A160" vr="UT">
            <xsl:value-of select="$text"/>
          </attr>
        </item>
      </attr>
    </item>
  </xsl:template>
</xsl:stylesheet>

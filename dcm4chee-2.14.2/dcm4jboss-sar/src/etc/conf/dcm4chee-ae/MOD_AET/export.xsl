<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="xml"/>
  <xsl:template match="/dataset">
    <exports>
      
      <!-- Export objects of all performed procedure steps to media -->
      <export code="113019" designator="99DCM4CHE" meaning="For Media Export"/>
         
      <!-- Export objects of procedure steps with given LOINC code to
      Research Collection -->
      <xsl:variable name="item" select="attr[@tag='00081032']/item"/>
      <xsl:variable name="code" select="$item/attr[@tag='00080100']"/>
      <xsl:variable name="designator" select="$item/attr[@tag='00080102']"/>
      <xsl:if test="$code='37441-3' and $designator='LN'">
        <export code="TCE007" designator="IHERADTF"
          meaning="For Research Collection Export"
          disposition="Chest High Resolution CT w/o Contrast"/>
      </xsl:if>
     
    </exports>
  </xsl:template>

</xsl:stylesheet>
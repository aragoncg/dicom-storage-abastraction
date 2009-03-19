<?xml version="1.0" encoding="UTF-8"?>
<!-- Sample configuration for grant/revoke Study Permissions on received HL7 messages -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="xml"/>
  <xsl:template match="/hl7">
    <permissions>
      <xsl:apply-templates select="MSH/field[7]" mode="MessageType"/>
    </permissions>
  </xsl:template>
  
  <!-- on Procedure Scheduled (ORM^O01) -->
  <xsl:template match="field[text()='ORM'][component='O01']" mode="MessageType">
    <!-- grant Query and Read permission on all exisiting Studies of this
    Patient to Doctor -->   
    <grant role="Doctor" action="Q,R" pid="{/hl7/PID/field[3]/text()}"
      issuer="{/hl7/PID/field[3]/component[3]}"/>
    <!-- grant Query, Read and Append permission on scheduled Study to Doctor -->   
    <grant role="Doctor" action="Q,R,A" suid="{/hl7/ZDS/field[1]/text()}"/>
  </xsl:template>
  
  <!-- on Patient Discharge (ADT^A03) -->
  <xsl:template match="field[text()='ADT'][component='A03']" mode="MessageType">
    <!-- revoke Query, Read and Append permission on all exisiting Studies of this
    Patient to Doctor -->   
    <revoke role="Doctor" action="Q,R,A" pid="{/hl7/PID/field[3]/text()}"
      issuer="{/hl7/PID/field[3]/component[3]}"/>
  </xsl:template>

  <xsl:template match="*" mode="MessageType"/>
  
</xsl:stylesheet>

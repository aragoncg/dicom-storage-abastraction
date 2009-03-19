<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="xml"/>

  <!--
   The following parameters are made available by the application:
   source-aet   - AET of the Storage SCU from which the series was received
   retrieve-aet - AET of the Query Retrieve SCP from which the series can be retrieved
   year  - The current year
   month - The current month (1=Jan, 2=Feb ..)
   date  - The current day of the month
   day   - The current day of the week (0=Sun, 1=Mon ..)
   hour  - The current hour of the day
   
   These parameters may be to define rules that depend on the source or retrieve AET
   or on the current date or time.
   
   An example of the parameters that are made available to this stylesheet is as follows:
   <xsl:param name="source-aet">DCMSND</xsl:param>
   <xsl:param name="retrieve-aet">DCM4CHEE</xsl:param>
   <xsl:param name="month">4</xsl:param>
   <xsl:param name="date">30</xsl:param> 
   <xsl:param name="day">1</xsl:param>
   <xsl:param name="hour">15</xsl:param>
  -->
  <xsl:param name="source-aet"/>
  <xsl:param name="retrieve-aet"/>
  <xsl:param name="year"/>
  <xsl:param name="month"/>
  <xsl:param name="date"/> 
  <xsl:param name="day"/>
  <xsl:param name="hour"/>

  <xsl:template match="/dataset">
    <destinations>
      <!-- Forward all Series to LONG_TERM outside business hours (7-19) after one week -->
      <destination aet="LONG_TERM" delay="1w!7-19"/>

      <!-- Forward Series with specified Referring Phyisican with low priority
        to PHYSICAN_DOE  after 3 days -->
      <xsl:if test="attr[@tag='00080090']='Doe^John'">
        <destination aet="PHYSICAN_DOE" priority="low" delay="3d"/>
      </xsl:if>
      
      <!-- Forward Magnetic Resonance Series with high priority 
        to MR_WORKSTATION immediately -->
      <xsl:if test="attr[@tag='00080060']='MR'">
        <destination aet="MR_WORKSTATION" priority="high"/>
      </xsl:if>
      
      <!-- Forward Series requested by Neuro Surgery to NEURO_SURGERY immediately -->
      <xsl:if test="attr[tag='00400275']/item/attr[@tag='00321033']='Neuro Surgery'">
        <destination aet="NEURO_SURGERY"/>
      </xsl:if>
      
    </destinations>
  </xsl:template>

</xsl:stylesheet>
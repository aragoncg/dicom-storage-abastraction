<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:internal="urn:my-internal-data" version="1.0">
  <xsl:output method="html" indent="yes" encoding="UTF-8"/>
  <xsl:variable name="page_title">Study Edit</xsl:variable>
  <xsl:include href="page.xsl"/>
  <xsl:param name="folder.edit.newStudyUID" select="'false'"/>
  <xsl:template match="model">
    <form action="studyUpdate.m" method="post" accept-charset="UTF-8" >
      <table border="1" cellspacing="0" cellpadding="0" width="100%">
        <tr>
          <td>
            <table border="0">
              <xsl:apply-templates select="patient"/>
              <xsl:apply-templates select="study"/>
            </table>
          </td>
        </tr>
      </table>
    </form>
  </xsl:template>
  <xsl:template match="patient">
    <tr>
      <td class="label" bgcolor="#eeeeee">Patient ID:</td>
      <td>
        <xsl:value-of select="patientID"/>
      </td>
    </tr>
    <tr>
      <td class="label" bgcolor="#eeeeee">Issuer of Patient ID:</td>
      <td>
        <xsl:value-of select="issuerOfPatientID"/>
      </td>
    </tr>
    <tr>
      <td class="label" bgcolor="#eeeeee">Patient Name:</td>
      <td>
        <xsl:value-of select="patientName"/>
      </td>
    </tr>
  </xsl:template>
  <xsl:template match="study">
    <tr>
      <td class="label" bgcolor="#eeeeee">Study Instance UID:</td>
      <td>
   	     <xsl:value-of select="studyIUID"/>
      </td>
    </tr>
    <tr>
      <td class="label" bgcolor="#eeeeee">Study ID:</td>
      <td>
        <xsl:value-of select="s"/>
      </td>
    </tr>
    <tr>
      <td class="label" bgcolor="#eeeeee">Accession Number:</td>
      <td>
        <xsl:value-of select="accessionNumber"/>
      </td>
    </tr>
    <tr>
      <td class="label" bgcolor="#eeeeee">Placer Order Number:</td>
      <td>
        <xsl:value-of select="placerOrderNumber"/>
      </td>
    </tr>
    <tr>
      <td class="label" bgcolor="#eeeeee">Filler Order Number:</td>
      <td>
        <xsl:value-of select="fillerOrderNumber"/>
      </td>
    </tr>
    <tr>
      <td class="label" bgcolor="#eeeeee">Referring Physician:</td>
      <td>
        <xsl:value-of select="referringPhysician"/>
      </td>
    </tr>
    <tr>
      <td class="label" bgcolor="#eeeeee">Study Description:</td>
      <td>
        <xsl:value-of select="studyDescription"/>
      </td>
    </tr>
    <tr>
      <td class="label" bgcolor="#eeeeee">Date/Time [yyyy/mm/dd hh:mm]:</td>
      <td>
        <xsl:value-of select="studyDateTime"/>
      </td>
    </tr>
    <tr>
      <td class="label" bgcolor="#eeeeee">Modalities in Study:</td>
      <td>
        <xsl:value-of select="modalitiesInStudy"/>
      </td>
    </tr>
    <tr>
      <td class="label" bgcolor="#eeeeee">Number of Series:</td>
      <td>
        <xsl:value-of select="numberOfSeries"/>
      </td>
    </tr>
    <tr>
      <td class="label" bgcolor="#eeeeee">Number of Instances:</td>
      <td>
        <xsl:value-of select="numberOfInstances"/>
      </td>
    </tr>
    <tr>
      <td class="label" bgcolor="#eeeeee">Availability:</td>
      <td>
        <xsl:value-of select="availability"/>
      </td>
    </tr>
    <tr>
      <td class="label" bgcolor="#eeeeee">Retrieve AETs:</td>
      <td>
        <xsl:value-of select="retrieveAETs"/>
      </td>
    </tr>
    <tr>
      <td class="label" bgcolor="#eeeeee">Study Status Id:</td>
      <td>
        <xsl:value-of select="studyStatusId"/>
      </td>
    </tr>
    <tr>
      <td align="left">
        <input type="submit" name="cancel" value="Cancel"/>
      </td>
    </tr>
  </xsl:template>
</xsl:stylesheet>

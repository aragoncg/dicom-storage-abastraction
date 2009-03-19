<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:internal="urn:my-internal-data" version="1.0">
  <xsl:output method="html" indent="yes" encoding="UTF-8"/>
  <xsl:variable name="page_title">Study Edit</xsl:variable>
  <xsl:include href="page.xsl"/>
  <xsl:param name="folder.edit.newStudyUID" select="'false'"/>
  <xsl:template match="model">
    <form action="studyUpdate.m" method="post" accept-charset="UTF-8" >
      <input name="patPk" type="hidden" value="{patPk}"/>
      <input name="studyPk" type="hidden" value="{studyPk}"/>
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
	    <xsl:choose>
			<xsl:when test="/model/studyPk = -1 and $folder.edit.newStudyUID = 'true'">
		        <input size="64" name="studyIUID" type="text" value="{studyIUID}"/>
			</xsl:when>
			<xsl:otherwise>
	    	    <xsl:value-of select="studyIUID"/>
			</xsl:otherwise>
	    </xsl:choose>
      </td>
    </tr>
    <tr>
      <td class="label" bgcolor="#eeeeee">Study ID:</td>
      <td>
        <input size="16" name="studyID" type="text" value="{studyID}"/>
      </td>
    </tr>
    <tr>
      <td class="label" bgcolor="#eeeeee">Accession Number:</td>
      <td>
        <input size="16" name="accessionNumber" type="text" value="{accessionNumber}"/>
      </td>
    </tr>
    <tr>
      <td class="label" bgcolor="#eeeeee">Placer Order Number:</td>
      <td>
        <input size="64" name="placerOrderNumber" type="text" value="{placerOrderNumber}"/>
      </td>
    </tr>
    <tr>
      <td class="label" bgcolor="#eeeeee">Filler Order Number:</td>
      <td>
        <input size="64" name="fillerOrderNumber" type="text" value="{fillerOrderNumber}"/>
      </td>
    </tr>
    <tr>
      <td class="label" bgcolor="#eeeeee">Referring Physician:</td>
      <td>
        <input size="64" name="referringPhysician" type="text" value="{referringPhysician}"/>
      </td>
    </tr>
    <tr>
      <td class="label" bgcolor="#eeeeee">Study Description:</td>
      <td>
        <input size="64" name="studyDescription" type="text" value="{studyDescription}"/>
      </td>
    </tr>
    <tr>
      <td class="label" bgcolor="#eeeeee">Date/Time [yyyy/mm/dd hh:mm]:</td>
      <td>
        <input size="16" name="studyDateTime" type="text" value="{studyDateTime}"/>
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
	    <xsl:choose>
		 	<xsl:when test="../studyPk = -1">
				<input type="submit" name="submit" value="Add Study" />									
			</xsl:when>
			<xsl:otherwise>
				<input type="submit" name="submit" value="Update" />
			</xsl:otherwise>
		</xsl:choose>
        <input type="submit" name="cancel" value="Cancel"/>
      </td>
    </tr>
  </xsl:template>
</xsl:stylesheet>

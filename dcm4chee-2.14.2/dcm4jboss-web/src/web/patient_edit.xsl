<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:internal="urn:my-internal-data">

   <xsl:output method="html" indent="yes" encoding="UTF-8" />

   <xsl:variable name="page_title">Patient Edit</xsl:variable>
   <xsl:include href="page.xsl" />

   <xsl:template match="model">
   		<xsl:apply-templates select="patient"/>
   </xsl:template>
   <xsl:template match="patient">
      <form NAME="PatientForm" action="patientUpdate.m" method="post" accept-charset="UTF-8" >
         <input name="pk" type="hidden" value="{pk}" />

		   <table border="1" cellspacing="0" cellpadding="0" width="100%"><tr><td>
           <table border="0">
            <tr>
               <td class="label" bgcolor="#eeeeee">Patient ID:</td>
               <td>							 
							   <xsl:choose>
								 	<xsl:when test="pk = -1">
                  <input size="64" name="patientID" type="text" value="{patientID}" />
									</xsl:when>
									<xsl:otherwise>
									  <xsl:value-of select="patientID"/>
									</xsl:otherwise>
								 </xsl:choose>
							 </td>
            </tr>
            <tr>
               <td class="label" bgcolor="#eeeeee">Issuer of Patient ID:</td>
               <td>							 
							   <xsl:choose>
								 	<xsl:when test="pk = -1">
                  <input size="64" name="issuerOfPatientID" type="text" value="{issuerOfPatientID}" />
									</xsl:when>
									<xsl:otherwise>
									  <xsl:value-of select="issuerOfPatientID"/>
									</xsl:otherwise>
								 </xsl:choose>
							 </td>
            </tr>
            <tr>
               <td class="label" bgcolor="#eeeeee">Patient Name:</td>
               <td>
                  <input size="64" name="patientName" type="text" value="{patientName}" />
               </td>
            </tr>

            <tr>
               <td class="label" bgcolor="#eeeeee">Sex:</td>
               <td>
                  <select name="patientSex" size="1">
										<option value="">
										<xsl:if test="patientSex = ''">
											<xsl:attribute name="selected"/>
										</xsl:if>
										</option>
										<option value="F">
										<xsl:if test="patientSex = 'F'">
											<xsl:attribute name="selected"/>
										</xsl:if>
										Female
										</option>
										<option value="M">
										<xsl:if test="patientSex = 'M'">
											<xsl:attribute name="selected"/>
										</xsl:if>
										Male
										</option>
										<option value="O">
										<xsl:if test="patientSex = 'O'">
											<xsl:attribute name="selected"/>
										</xsl:if>
										Other
										</option>
									</select>
               </td>
            </tr>

            <tr>
               <td class="label" bgcolor="#eeeeee">Birth Date [yyyy/mm/dd]:</td>
               <td>
                  <input size="16" name="patientBirthDate" type="text" value="{patientBirthDate}" />
               </td>
            </tr>

            <tr>
               <td align="left">
							   <xsl:choose>
								 	<xsl:when test="pk = -1">
										<input type="submit" name="submit" value="Add Patient" 
											onclick="return checkPatientFields(this.form)"/>									
									</xsl:when>
									<xsl:otherwise>
										<input type="submit" name="submit" value="Update" />
									</xsl:otherwise>
								 </xsl:choose>
                  <input type="submit" name="cancel" value="Cancel" />
               </td>
            </tr>
         </table>
         </td></tr></table>
      </form>
   </xsl:template>
</xsl:stylesheet>


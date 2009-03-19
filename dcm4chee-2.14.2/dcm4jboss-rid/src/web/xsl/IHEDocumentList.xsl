<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
 version="1.0">
 <xsl:output method="xml" encoding="UTF-8" indent="no"
  doctype-public="-//W3C//DTD XHTML 1.0 Strict//EN"
  doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd" />

 <xsl:include href="common.xsl" />
 <xsl:template match="/">
  <html>
   <head>
    <title>
     <xsl:value-of
      select="IHEDocumentList/recordTarget/patient/patientPatient/name/family" />
     ,
     <xsl:value-of
      select="IHEDocumentList/recordTarget/patient/patientPatient/name/given" />
    </title>
   </head>
   <body>
    <h2>
     <xsl:value-of select="IHEDocumentList/code/@displayName" />
     for
     <xsl:value-of
      select="IHEDocumentList/recordTarget/patient/patientPatient/name/family" />
     ,
     <xsl:value-of
      select="IHEDocumentList/recordTarget/patient/patientPatient/name/given" />
    </h2>
    <p>
     <object>
      <b>Gender:</b>
      <xsl:value-of
       select="IHEDocumentList/recordTarget/patient/patientPatient/administrativeGenderCode/@code" />
      <br />
      <b>DOB:</b>
      <xsl:call-template name="formatDate">
       <xsl:with-param name="date"
        select="IHEDocumentList/recordTarget/patient/patientPatient/birthTime/@value" />
      </xsl:call-template>
      <br />
      <b>List Created:</b>
      <xsl:call-template name="formatDateWithTime">
       <xsl:with-param name="dateTime"
        select="IHEDocumentList/activityTime/@value" />
      </xsl:call-template>
      <br />
      <br />
      <table border="2">
       <tr>
        <th>Type</th>
        <th>Status</th>
        <th>Date</th>
        <th>Document</th>
       </tr>
       <xsl:for-each select="/IHEDocumentList/component/documentInformation">
        <tr>
         <td>
          <xsl:value-of select="code/@displayName" />
         </td>
         <td>
          <xsl:value-of select="statusCode/@code" />
         </td>
         <td>
          <xsl:call-template name="formatDateWithTime">
           <xsl:with-param name="dateTime" select="effectiveTime/@value" />
          </xsl:call-template>
         </td>
         <td>
          <a>
           <xsl:attribute name="href"> <xsl:value-of
             select="text/reference/@value" />
                    			</xsl:attribute>
           Get Document
          </a>
         </td>
        </tr>
       </xsl:for-each>
      </table>
     </object>
    </p>
   </body>
  </html>
 </xsl:template>
</xsl:stylesheet>


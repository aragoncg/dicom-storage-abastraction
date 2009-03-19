<?xml version="1.0" encoding="UTF-8"?>
<!-- $Id$ -->
<xsl:param name="trash.remove" select="'false'" />
<xsl:template match="model">
 <form action="trashfolder.m" method="post" name="myForm"
  accept-charset="UTF-8">
  <table class="folder_header" border="0" cellspacing="0"
   cellpadding="0" width="100%">
   <td class="folder_header" valign="top">
    <table class="folder_header" border="0" height="30" cellspacing="0"
     cellpadding="0" width="100%">
     <td class="folder_header" width="5">
      <input type="checkbox" name="showWithoutStudies" value="true"
       title="&ShowPatientsWithoutStudies;">
       <xsl:if test="/model/showWithoutStudies = 'true'">
        <xsl:attribute name="checked" />
       </xsl:if>
      </input>
     </td>
     <td class="folder_header" width="5" title="&ShowPatientsWithoutStudies;">
       <xsl:text>&woStudies;</xsl:text>
     </td>
     <td class="folder_header" align="center">
      <xsl:choose>
        <xsl:when test="total &lt; 1">&NoMatchingStudiesFound;</xsl:when>
        <xsl:otherwise>&DisplayingStudies;
         <b>
         <xsl:value-of select="offset + 1" />
         </b>
          &to;
          <b>
         <xsl:choose>
          <xsl:when test="offset + limit &lt; total">
           <xsl:value-of select="offset + limit" />
          </xsl:when>
          <xsl:otherwise>
           <xsl:value-of select="total" />
          </xsl:otherwise>
         </xsl:choose>
          </b>
          &of;
          <b>
         <xsl:value-of select="total" />
          </b>
          &matchingStudies;.
        </xsl:otherwise>
      </xsl:choose>
     </td>

     <td class="folder_header" width="150"></td>
     <td class="folder_header" width="40">
      <input type="image" value="Search" name="filter"
        src="images/search.gif" border="0" title="&NewSearch;" />
     </td>
     <td class="folder_header" width="40">
      <input type="image" value="Prev" name="prev" src="images/prev.gif"
        alt="prev" border="0" title="&PreviousSearchResults;">
       <xsl:if test="offset = 0">
        <xsl:attribute name="disabled">disabled</xsl:attribute>
       </xsl:if>
      </input>
     </td>
     <td class="folder_header" width="40">
      <input type="image" value="Next" name="next" src="images/next.gif"
        alt="next" border="0" title="&NextSearchResults;">
       <xsl:if test="offset + limit &gt;= total">
        <xsl:attribute name="disabled">disabled</xsl:attribute>
       </xsl:if>
      </input>
     </td>
     <td class="folder_header" width="40">&#160;</td>
     <td class="folder_header" width="40">
      <input type="image" value="Undel" name="undel"
       src="images/undel.gif" alt="undelete" border="0"
        title="&RecoverSelectedEntities;"
        onclick="return confirm('&RecoverSelectedEntities;?')">
       <xsl:if test="total &lt;= 0">
        <xsl:attribute name="disabled">disabled</xsl:attribute>
       </xsl:if>
      </input>
     </td>
     <xsl:if test="$trash.remove='true'">
      <td class="folder_header" width="40">
       <input type="image" value="Del" name="del"
        src="images/loeschen.gif" alt="delete" border="0"
         title="&DeleteSelectedEntitiesIrrevocable;"
         onclick="return confirm('&DeleteSelectedEntitiesIrrevocable;?')">
        <xsl:if test="total &lt;= 0">
         <xsl:attribute name="disabled">disabled</xsl:attribute>
        </xsl:if>
       </input>
      </td>
      <td class="folder_header" width="40">
       <input type="image" value="EmptyTrash" name="emptyTrash"
        src="images/deltrash.gif" alt="emptyTrash" border="0"
         title="&EmptyTrash;"
         onclick="return confirm('&EmptyTrash;?')">
         <xsl:if test="total &lt;= 0">
         <xsl:attribute name="disabled">disabled</xsl:attribute>
        </xsl:if>
       </input>
      </td>
     </xsl:if>
    </table>
    <table class="folder_search" border="0" width="100%" cellpadding="0"
     cellspacing="0">
     <tr>
      <td class="folder_search">&PatientName;:</td>
      <td>
       <input size="10" name="patientName" type="text"
        value="{patientName}" />
      </td>
      <td class="folder_search">&PatientID;:</td>
      <td>
       <input size="10" name="patientID" type="text"
        value="{patientID}" />
      </td>
      <td class="label">&StudyIUID;:</td>
      <td>
       <input size="45" name="studyUID" type="text" value="{studyUID}" />
      </td>
      <td class="label">&AccessionNo;:</td>
      <td>
       <input size="10" name="accessionNumber" type="text"
        value="{accessionNumber}" />
      </td>
     </tr>
    </table>
    <xsl:call-template name="overview" />
    <table border="0" cellpadding="0" cellspacing="0" width="100%">
     <tr>
      <td>
       <table border="0" cellpadding="0" cellspacing="0" width="100%">
        <tbody valign="top">
         <xsl:apply-templates select="patients/item" />
        </tbody>
       </table>
      </td>
     </tr>
    </table>
   </td>
  </table>
 </form>
</xsl:template>

<xsl:template name="overview">
 <table class="folder_overview" border="0" cellpadding="0"
  cellspacing="0" width="100%">
  <table class="folder_overview" border="0" cellpadding="0"
   cellspacing="0" width="100%">
   <colgroup>
    <col width="5%" />
    <col width="22%" />
    <col width="10%" />
    <col width="12%" />
    <col width="47%" />
    <col width="4%" />
   </colgroup>
   <tr>
    <td class="patient_mark">
     <font size="1">&Patient;</font>
    </td>
    <td>
     <font size="1">&Name;:</font>
    </td>
    <td>
     <font size="1">&PatientID;:</font>
    </td>
    <td>
      <font size="1">&BirthDate;:</font>
    </td>
    <td>
     <font size="1">&Sex;:</font>
    </td>
    <td></td>
   </tr>
  </table>

  <table class="folder_overview" border="0" cellspacing="0"
   cellpadding="0" width="100%">
   <colgroup>
    <col width="5%" /><!-- margin -->
    <col width="11%" /><!-- Date/time -->
    <col width="22%" /><!-- StudyID -->
    <col width="26%" /><!-- Study Instance UID -->
    <col width="9%" /><!-- Acc No --><!-- 73 -->
    <col width="13%" /><!-- Ref. Physician -->
    <col width="8%" /><!-- Study Status ID -->
    <col width="2%" /><!-- add -->
    <col width="2%" /><!-- edit -->
    <col width="2%" /><!-- sticky -->
   </colgroup>
   <tr>
    <td class="study_mark">
      <font size="1">&Study;</font>
    </td>
    <td>
      <font size="1">&Date;/&Time;:</font>
    </td>
    <td>
      <font size="1">&StudyIDMedia;:</font>
    </td>
    <td>
      <font size="1">&StudyIUID;:</font>
    </td>
    <td>
      <font size="1">&AccNo;:</font>
    </td>
    <td>
      <font size="1">&RefPhysician;</font>
    </td>
    <td>
      <font size="1">&Status;:</font>
    </td>
    <td>&#160;</td>
    <td>&#160;</td>
    <td>&#160;</td>
   </tr>
  </table>

  <table class="folder_overview" border="0" cellspacing="0"
   cellpadding="0" width="100%">
   <colgroup>
    <col width="5%" /><!-- margin -->
    <col width="12%" /><!-- date/time -->
    <col width="12%" /><!-- Series No -->
    <col width="10%" /><!-- Modality -->
    <col width="35%" /><!-- Series Instance UID. -->
    <col width="10%" /><!-- Vendor/Model -->

    <col width="12%" /><!-- PPS Status -->
    <col width="2%" /><!-- edit -->
    <col width="2%" /><!-- sticky -->
   </colgroup>
   <tr>
    <td class="series_mark">
      <font size="1">&Series;</font>
    </td>
    <td>
      <font size="1">&Date;/&Time;:</font>
    </td>
    <td>
      <font size="1">&SeriesNoMedia;:</font>
    </td>
    <td>
      <font size="1">&Modality;:</font>
    </td>
    <td>
      <font size="1">&SeriesIUID;:</font>
    </td>
    <td>
      <font size="1">&Vendor;/&Model;:</font>
    </td>
    <td>
     <font size="1">&PPSStatus;:</font>
    </td>
    <td>&#160;</td>
    <td>&#160;</td>
    <td align="right" valign="bottom">
      <img src="images/plus.gif" alt="+" title="&SelectAllStudies;"
      onclick="selectAll( document.myForm,'stickyStudy', true)" />
      <img src="images/minus.gif" alt="-" title="&DeselectAll;"
      onclick="selectAll( document.myForm,'sticky', false)" />
    </td>
   </tr>
  </table>
 </table>
</xsl:template>


<xsl:template
 match="item[@type='org.dcm4chex.archive.web.maverick.model.PatientModel']">
 <tr>
  <table class="patient_line" width="100%" cellpadding="0"
   cellspacing="0" border="0">
   <colgroup>
    <col width="1%" />
    <col width="26%" />
    <col width="10%" />
    <col width="12%" />
    <col width="45%" />
    <col width="2%" />
    <col width="2%" />
    <col width="2%" />
   </colgroup>
   <xsl:variable name="rowspan"
    select="1+count(descendant::studies/item)" />
   <td class="patient_mark" align="right" rowspan="{$rowspan}">
    <xsl:choose>
     <xsl:when test="$rowspan=1">
       <a title="&ShowStudies;"
       href="expandTrashPatient.m?patPk={pk}&amp;expand=true">
       <img src="images/plus.gif" border="0" alt="+" />
      </a>
     </xsl:when>
     <xsl:otherwise>
       <a title="&HideStudies;"
       href="expandTrashPatient.m?patPk={pk}&amp;expand=false">
       <img src="images/minus.gif" border="0" alt="-" />
      </a>
     </xsl:otherwise>
    </xsl:choose>
   </td>
    <td title="&PatientName;">
    <strong>
     <xsl:value-of select="patientName" />
    </strong>
    </td>
    <td title="&PatientID;">
    <strong>
     <xsl:value-of select="patientID" />
    </strong>
    </td>
    <td title="&BirthDate;">
    <strong>
     <xsl:value-of select="patientBirthDate" />
    </strong>
    </td>
    <td title="&Sex;">
      <strong>
     <xsl:value-of select="patientSex" />
    </strong>
   </td>
   <td>&#160;</td>
   <td class="patient_mark" align="right">
    <a href="trashfolder.m?undel=patient&amp;patPk={pk}"
      onclick="return confirm('&RecoverPatient;?')">
      <img src="images/undel.gif" alt="icon" border="0"
      title="&RecoverPatient;" />
    </a>
   </td>
   <td class="patient_mark" align="right">
    <input type="checkbox" name="stickyPat" value="{pk}">
     <xsl:if test="/model/stickyPatients/item = pk">
      <xsl:attribute name="checked" />
     </xsl:if>
    </input>
   </td>
  </table>
 </tr>
 <xsl:apply-templates select="studies/item">
  <xsl:sort data-type="text" order="ascending" select="studyDateTime" />
 </xsl:apply-templates>
</xsl:template>

<xsl:template
 match="item[@type='org.dcm4chex.archive.web.maverick.model.StudyModel']">
 <tr>
  <table class="study_line" width="100%" cellpadding="0" cellspacing="0"
   border="0">
   <xsl:variable name="rowspan"
    select="1+count(descendant::series/item)" />
   <colgroup>
    <col width="2%" /><!-- margin -->
    <col width="14%" /><!-- Date/time -->
    <col width="22%" /><!-- StudyID -->
    <col width="26%" /><!-- Study Instance UID -->
    <col width="9%" /><!-- Acc No -->
    <col width="13%" /><!-- Ref. Physician -->
    <col width="8%" /><!-- Study Status ID -->
    <col width="2%" /><!-- add -->
    <col width="2%" /><!-- edit -->
    <col width="2%" /><!-- sticky -->
   </colgroup>
   <td class="study_mark" align="right" rowspan="{$rowspan}">
    <xsl:choose>
     <xsl:when test="$rowspan=1">
       <a title="&ShowSeries;"
       href="expandTrashStudy.m?patPk={../../pk}&amp;studyPk={pk}&amp;expand=true">
       <img src="images/plus.gif" border="0" alt="+" />
      </a>
     </xsl:when>
     <xsl:otherwise>
       <a title="&HideSeries;"
       href="expandTrashStudy.m?patPk={../../pk}&amp;studyPk={pk}&amp;expand=false">
       <img src="images/minus.gif" border="0" alt="-" />
      </a>
     </xsl:otherwise>
    </xsl:choose>
   </td>
    <td title="&StudyDateTime;">
    <xsl:value-of select="studyDateTime" />
    </td>
    <td title="&StudyIDMedia;">
      <xsl:value-of select="studyID" />
    <xsl:if test="filesetId != '_NA_'">
     @<xsl:value-of select="filesetId" />
    </xsl:if>
    </td>
    <td title="&StudyIUID;">
    <xsl:value-of select="studyIUID" />
    </td>
    <td title="&AccessionNo;">
    <xsl:value-of select="accessionNumber" />
    </td>
    <td title="&RefPhysican;">
      <xsl:value-of select="referringPhysician" />
    </td>
    <td title="&StudyStatusID;" align="center">
    <xsl:choose>
     <xsl:when test="studyStatusImage!=''">
      <img src="{studyStatusImage}" border="0" alt="{studyStatusId}" />
     </xsl:when>
     <xsl:when test="studyStatusId!=''">
      <xsl:value-of select="studyStatusId" />
     </xsl:when>
     <xsl:otherwise>&#160;</xsl:otherwise>
    </xsl:choose>
   </td>
   <td>&#160;</td>
   <td class="study_mark" align="right">
    <a href="trashfolder.m?undel=study&amp;studyPk={pk}"
     onclick="return confirm('Undelete this study ?')">
     <img src="images/undel.gif" alt="icon" border="0"
      title="&RecoverStudy;" />
    </a>
   </td>
   <td class="study_mark" align="right">
    <input type="checkbox" name="stickyStudy" value="{pk}">
     <xsl:if test="/model/stickyStudies/item = pk">
      <xsl:attribute name="checked" />
     </xsl:if>
    </input>
   </td>
  </table>
 </tr>
 <xsl:apply-templates select="series/item">
  <xsl:sort data-type="number" order="ascending" select="seriesNumber" />
 </xsl:apply-templates>
</xsl:template>

<xsl:template
 match="item[@type='org.dcm4chex.archive.web.maverick.model.SeriesModel']">
 <tr>
  <table class="series_line" width="100%" cellpadding="0"
   cellspacing="0" border="0">
   <colgroup>
    <col width="3%" /><!-- left margin -->
    <col width="2%" /><!-- spacer -->
    <col width="14%" /><!-- Date/Time -->
    <col width="12%" /><!-- Series No -->
    <col width="10%" /><!-- Modality -->
    <col width="35%" /><!-- Series Instance UID. -->
    <col width="10%" /><!-- Vendor/Model -->
    <col width="12%" /><!-- PPS Status -->
    <col width="2%" /><!-- edit -->
    <col width="2%" /><!-- sticky -->
   </colgroup>
   <xsl:variable name="rowspan"
    select="1+count(descendant::instances/item)" />
   <td class="series_mark" align="right" rowspan="{$rowspan}">
    <xsl:choose>
     <xsl:when test="$rowspan=1">
       <a title="&ShowInstances;"
       href="expandTrashSeries.m?patPk={../../../../pk}&amp;studyPk={../../pk}&amp;seriesPk={pk}&amp;expand=true">
       <img src="images/plus.gif" border="0" alt="+" />
      </a>
     </xsl:when>
     <xsl:otherwise>
       <a title="&HideInstances;"
       href="expandTrashSeries.m?patPk={../../../../pk}&amp;studyPk={../../pk}&amp;seriesPk={pk}&amp;expand=false">
       <img src="images/minus.gif" border="0" alt="-" />
      </a>
     </xsl:otherwise>
    </xsl:choose>
   </td>
   <td>&#160;</td>
    <td title="&SeriesDateTime;">
    <xsl:value-of select="seriesDateTime" />
    </td>
    <td title="&SeriesNoMedia;">
      <xsl:value-of select="seriesNumber" />
    <xsl:if test="filesetId != '_NA_'">
     @<xsl:value-of select="filesetId" />
    </xsl:if>
    </td>
    <td title="&Modality;">
    <xsl:value-of select="modality" />
    </td>
    <td title="&SeriesIUID;">
      <xsl:value-of select="seriesIUID" />
    </td>
    <td title="&Vendor;/&Model;">
      <xsl:value-of select="manufacturer" />
    <xsl:text>\</xsl:text>
    <xsl:value-of select="manufacturerModelName" />
   </td>
   <td>
    <xsl:choose>
     <xsl:when
      test="PPSStatus='DISCONTINUED' or incorrectWLEntry='true'">
      <xsl:attribute name="style">color: red</xsl:attribute>
      <xsl:attribute name="title">&PPSStatus;:
       <xsl:value-of select="DRCodeMeaning" />
      </xsl:attribute>
      <xsl:text>DISCONTINUED</xsl:text>
     </xsl:when>
     <xsl:when test="PPSStatus!=''">
      <xsl:attribute name="style">color: black</xsl:attribute>
       <xsl:attribute name="title">&PPSStatus;</xsl:attribute>
      <xsl:value-of select="PPSStatus" />
     </xsl:when>
    </xsl:choose>
   </td>
   <td>&#160;</td>
   <td class="series_mark" align="right">
    <a href="trashfolder.m?undel=series&amp;seriesPk={pk}"
      onclick="return confirm('&RecoverSeries;?')">
      <img src="images/undel.gif" alt="icon" border="0"
      title="&RecoverSeries;" />
    </a>
   </td>
   <td class="series_mark" align="right">
    <input type="checkbox" name="stickySeries" value="{pk}">
     <xsl:if test="/model/stickySeries/item = pk">
      <xsl:attribute name="checked" />
     </xsl:if>
    </input>
   </td>
  </table>
 </tr>
 <xsl:apply-templates select="instances/item">
  <xsl:sort data-type="number" order="ascending"
   select="instanceNumber" />
 </xsl:apply-templates>
</xsl:template>

<xsl:template
 match="item[@type='org.dcm4chex.archive.web.maverick.model.ImageModel']">
 <tr>
  <table class="instance_line" width="100%" cellpadding="0"
   cellspacing="0" border="0">
   <colgroup>
    <col width="4%" />
    <col width="12%" />
    <col width="3%" />
    <col width="6%" />
    <col width="25%" />
    <col width="5%" />
    <col width="10%" />
    <col width="31%" />
    <col width="2%" />
    <col width="2%" />
   </colgroup>
   <xsl:variable name="rowspan"
    select="1+count(descendant::files/item)" />
   <td align="right" rowspan="{$rowspan}">
    <xsl:choose>
     <xsl:when test="$rowspan=1">
       <a title="&ShowFiles;"
       href="expandTrashInstance.m?expand=true&amp;patPk={../../../../../../pk}&amp;studyPk={../../../../pk}&amp;seriesPk={../../pk}&amp;instancePk={pk}">
       <img src="images/plus.gif" border="0" alt="+" />
      </a>
     </xsl:when>
     <xsl:otherwise>
       <a title="&HideFiles;"
         href="expandTrashInstance.m?expand=false&amp;patPk={../../../../../../pk}&amp;studyPk={../../../../pk}&amp;seriesPk={../../pk}&amp;instancePk={pk}">
       <img src="images/minus.gif" border="0" alt="-" />
      </a>
     </xsl:otherwise>
    </xsl:choose>
   </td>

    <td title="&ContentDateTime;">
    <xsl:value-of select="contentDateTime" />
    </td>
    <td title="&InstanceNumber;">
    <xsl:value-of select="instanceNumber" />
    </td>
    <td title="&ImageType;">
    <xsl:value-of select="imageType" />
    </td>
    <td title="&PixelMatrix;">
    <xsl:value-of select="photometricInterpretation" />
    <xsl:text> </xsl:text>
    <xsl:value-of select="rows" />
    <xsl:text>x</xsl:text>
    <xsl:value-of select="columns" />
    <xsl:text>x</xsl:text>
    <xsl:value-of select="numberOfFrames" />
    <xsl:text> </xsl:text>
    <xsl:value-of select="bitsAllocated" />
    <xsl:text>&#160;bits</xsl:text>
    </td>
    <td title="&NumberOfFiles;">
    <xsl:value-of select="numberOfFiles" />
    </td>
    <td title="&RetrieveAET;">
    <xsl:value-of select="retrieveAETs" />
    </td>
    <td title="&SopIUID;">
    <xsl:value-of select="sopIUID" />
   </td>
   <td class="instance_mark" align="right">
    <a href="trashfolder.m?undel=instance&amp;instancePk={pk}"
      onclick="return confirm('&RecoverInstance; ?')">
     <img src="images/undel.gif" alt="icon" border="0"
      title="&RecoverInstance;" />
    </a>
   </td>
   <td class="instance_mark" align="right">
    <input type="checkbox" name="stickyInst" value="{pk}">
     <xsl:if test="/model/stickyInstances/item = pk">
      <xsl:attribute name="checked" />
     </xsl:if>
    </input>
   </td>
  </table>
 </tr>
 <xsl:apply-templates select="files/item">
  <xsl:sort data-type="number" order="descending" select="pk" />
 </xsl:apply-templates>
</xsl:template>

<xsl:template
 match="item[@type='org.dcm4chex.archive.web.maverick.model.PresentationStateModel']">
 <tr>
  <table class="instance_line" width="100%" cellpadding="0"
   cellspacing="0" border="0">
   <colgroup>
    <col width="4%" />
    <col width="15%" />
    <col width="6%" />
    <col width="20%" />
    <col width="5%" />
    <col width="5%" />
    <col width="5%" />
    <col width="12%" />
    <col width="24%" />
    <col width="2%" />
    <col width="2%" />
   </colgroup>
   <xsl:variable name="rowspan"
    select="1+count(descendant::files/item)" />
   <td align="right" rowspan="{$rowspan}">
    <xsl:choose>
     <xsl:when test="$rowspan=1">
      <a title="&ShowFiles;"
       href="expandTrashInstance.m?expand=true&amp;patPk={../../../../../../pk}&amp;studyPk={../../../../pk}&amp;seriesPk={../../pk}&amp;instancePk={pk}">
       <img src="images/plus.gif" border="0" alt="+" />
      </a>
     </xsl:when>
     <xsl:otherwise>
      <a title="&HideFiles;"
       href="expandTrashInstance.m?expand=false&amp;patPk={../../../../../../pk}&amp;studyPk={../../../../pk}&amp;seriesPk={../../pk}&amp;instancePk={pk}">
       <img src="images/minus.gif" border="0" alt="-" />
      </a>
     </xsl:otherwise>
    </xsl:choose>
   </td>
   <td title="Creation Datetime">
    <xsl:value-of select="presentationCreationDateTime" />
   </td>
   <td title="&InstanceNumber;">
    <xsl:value-of select="instanceNumber" />
   </td>
   <td title="Presentation Description">
    <xsl:value-of select="presentationDescription" />
   </td>
   <td title="Presentation Label">
    <xsl:value-of select="presentationLabel" />
   </td>
   <td title="Number of Referenced Images">
    <xsl:text>-&gt;</xsl:text>
    <xsl:value-of select="numberOfReferencedImages" />
   </td>
   <td title="&NumberOfFiles;">
    <xsl:value-of select="numberOfFiles" />
   </td>
   <td title="&RetrieveAET;">
    <xsl:value-of select="retrieveAETs" />
   </td>
   <td title="&SopIUID;">
    <xsl:value-of select="sopIUID" />
   </td>
   <td class="instance_mark" align="right">
    <a
     href="trashfolder.m?undel=instance&amp;patPk={../../../../../../pk}&amp;studyPk={../../../../pk}&amp;seriesPk={../../pk}&amp;instancePk={pk}"
     onclick="return confirm('&RecoverInstance;?')">
     <img src="images/undel.gif" alt="icon" border="0"
      title="&RecoverInstance;" />
    </a>
   </td>
   <td class="instance_mark" align="right">
    <input type="checkbox" name="stickyInst" value="{pk}">
     <xsl:if test="/model/stickyInstances/item = pk">
      <xsl:attribute name="checked" />
     </xsl:if>
    </input>
   </td>
  </table>
 </tr>
 <xsl:apply-templates select="files/item">
  <xsl:sort data-type="number" order="descending" select="pk" />
 </xsl:apply-templates>
</xsl:template>

<xsl:template
 match="item[@type='org.dcm4chex.archive.web.maverick.model.StructuredReportModel']">
 <tr>
  <table class="instance_line" width="100%" cellpadding="1"
   cellspacing="0" border="0">
   <colgroup>
    <col width="4%" />
    <col width="15%" />
    <col width="6%" />
    <col width="15%" />
    <col width="15%" />
    <col width="5%" />
    <col width="13%" />
    <col width="23" />
    <col width="2%" />
    <col width="2%" />
   </colgroup>

   <xsl:variable name="rowspan"
    select="1+count(descendant::files/item)" />
   <td align="right" rowspan="{$rowspan}">
    <xsl:choose>
     <xsl:when test="$rowspan=1">
      <a title="&ShowFiles;"
       href="expandTrashInstance.m?expand=true&amp;patPk={../../../../../../pk}&amp;studyPk={../../../../pk}&amp;seriesPk={../../pk}&amp;instancePk={pk}">
       <img src="images/plus.gif" border="0" alt="+" />
      </a>
     </xsl:when>
     <xsl:otherwise>
      <a title="&HideFiles;"
       href="expandTrashInstance.m?expand=false&amp;patPk={../../../../../../pk}&amp;studyPk={../../../../pk}&amp;seriesPk={../../pk}&amp;instancePk={pk}">
       <img src="images/minus.gif" border="0" alt="-" />
      </a>
     </xsl:otherwise>
    </xsl:choose>
   </td>
   <td title="&ContentDateTime;">
    <xsl:value-of select="contentDateTime" />
   </td>
   <td title="&InstanceNumber;">
    <xsl:value-of select="instanceNumber" />
   </td>
   <td title="&DocumentTitle;">
    <xsl:value-of select="documentTitle" />
   </td>
   <td title="&DocumentStatus;">
    <xsl:value-of select="completionFlag" />
    <xsl:text>/</xsl:text>
    <xsl:value-of select="verificationFlag" />
   </td>
   <td title="&NumberOfFiles;">
    <xsl:value-of select="numberOfFiles" />
   </td>
   <td title="&RetrieveAET;">
    <xsl:value-of select="retrieveAETs" />
   </td>
   <td title="&SopIUID;">
    <xsl:value-of select="sopIUID" />
   </td>
   <td class="instance_mark" align="right">
    <a
     href="trashfolder.m?undel=instance&amp;patPk={../../../../../../pk}&amp;studyPk={../../../../pk}&amp;seriesPk={../../pk}&amp;instancePk={pk}"
     onclick="return confirm('&RecoverInstance;?')">
     <img src="images/undel.gif" alt="icon" border="0"
      title="&RecoverInstance;" />
    </a>
   </td>
   <td class="instance_mark" align="right">
    <input type="checkbox" name="stickyInst" value="{pk}">
     <xsl:if test="/model/stickyInstances/item = pk">
      <xsl:attribute name="checked" />
     </xsl:if>
    </input>
   </td>
  </table>
 </tr>
 <xsl:apply-templates select="files/item">
  <xsl:sort data-type="number" order="descending" select="pk" />
 </xsl:apply-templates>
</xsl:template>

<xsl:template
 match="item[@type='org.dcm4chex.archive.web.maverick.model.WaveformModel']">
 <tr>
  <table class="instance_line" width="100%" cellpadding="0"
   cellspacing="0" border="0">
   <colgroup>
    <col width="4%" />
    <col width="10%" />
    <col width="3%" />
    <col width="21%" />
    <col width="25%" />
    <col width="10%" />
    <col width="23%" />
    <col width="2%" />
    <col width="2%" />
   </colgroup>
   <xsl:variable name="rowspan"
    select="1+count(descendant::files/item)" />
   <td align="right" rowspan="{$rowspan}">
    <xsl:choose>
     <xsl:when test="$rowspan=1">
      <a title="&ShowFiles;"
       href="expandTrashInstance.m?expand=true&amp;patPk={../../../../../../pk}&amp;studyPk={../../../../pk}&amp;seriesPk={../../pk}&amp;instancePk={pk}">
       <img src="images/plus.gif" border="0" alt="+" />
      </a>
     </xsl:when>
     <xsl:otherwise>
      <a title="&HideFiles;"
       href="expandTrashInstance.m?expand=false&amp;patPk={../../../../../../pk}&amp;studyPk={../../../../pk}&amp;seriesPk={../../pk}&amp;instancePk={pk}">
       <img src="images/minus.gif" border="0" alt="-" />
      </a>
     </xsl:otherwise>
    </xsl:choose>
   </td>
   <td title="&ContentDateTime;">
    <xsl:value-of select="contentDateTime" />
   </td>
   <td title="&InstanceNumber;">
    <xsl:value-of select="instanceNumber" />
   </td>
   <td title="&WaveformType;">
    <xsl:value-of select="waveformType" />
   </td>
   <td>&#160;&#160;</td>
   <td title="&RetrieveAET;">
    <xsl:value-of select="retrieveAETs" />
   </td>
   <td title="&SopIUID;">
    <xsl:value-of select="sopIUID" />
   </td>
   <td class="instance_mark" align="right">
    <a
     href="trashfolder.m?undel=instance&amp;patPk={../../../../../../pk}&amp;studyPk={../../../../pk}&amp;seriesPk={../../pk}&amp;instancePk={pk}"
     onclick="return confirm('&RecoverInstance;?')">
     <img src="images/undel.gif" alt="icon" border="0"
      title="&RecoverInstance;" />
    </a>
   </td>
   <td class="instance_mark" align="right">
    <input type="checkbox" name="stickyInst" value="{pk}">
     <xsl:if test="/model/stickyInstances/item = pk">
      <xsl:attribute name="checked" />
     </xsl:if>
    </input>
   </td>
  </table>
 </tr>
 <xsl:apply-templates select="files/item">
  <xsl:sort data-type="number" order="descending" select="pk" />
 </xsl:apply-templates>
</xsl:template>

<xsl:template
 match="item[@type='org.dcm4chex.archive.ejb.interfaces.FileDTO']">
 <xsl:variable name="line_name">
  <xsl:choose>
   <xsl:when test="fileStatus &lt; 0">error_line</xsl:when>
   <xsl:otherwise>file_line</xsl:otherwise>
  </xsl:choose>
 </xsl:variable>
 <tr>
  <table class="{$line_name}" width="100%" cellpadding="0"
   cellspacing="0" border="0">
   <colgroup>
    <col width="5%" />
    <col width="10%" />
    <col width="10%" />
    <col width="10%" />
    <col width="10%" />
    <col width="35%" />
    <col width="20%" />
   </colgroup>
   <td>&#160;</td>
   <td title="&TSUID;">
    <xsl:value-of select="fileTsuid" />
   </td>
   <td title="&RetrieveAET;">
    <xsl:value-of select="retrieveAET" />
   </td>
   <td title="&Status;">
    <xsl:choose>
     <xsl:when test="fileStatus=0">&OK;</xsl:when>
      <xsl:when test="fileStatus=1">&toArchive;</xsl:when>
     <xsl:when test="fileStatus=2">&archived;</xsl:when>
      <xsl:when test="fileStatus=-1">&compressionFailed;</xsl:when>
      <xsl:when test="fileStatus=-2">&compressionVerificationFailed;</xsl:when>
     <xsl:when test="fileStatus=-3">&MD5CheckFailed;</xsl:when>
     <xsl:when test="fileStatus=-3">&HSMQueryFailed;</xsl:when>
     <xsl:otherwise>
      <xsl:value-of select="fileStatus" />
     </xsl:otherwise>
    </xsl:choose>
   </td>
   <td title="&FileSize;">
    <xsl:value-of select="fileSize" />
    <xsl:text>&#160;bytes</xsl:text>
   </td>
   <td title="&FilePath;">
    <xsl:value-of select="directoryPath" />
    <xsl:text>/</xsl:text>
    <xsl:value-of select="filePath" />
   </td>
   <td title="&MD5Sum;">
     <xsl:value-of select="md5String" />
   </td>
  </table>
 </tr>
</xsl:template>

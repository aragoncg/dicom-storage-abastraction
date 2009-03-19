<?xml version="1.0" encoding="UTF-8"?>
<!-- $Id$ -->
<!--
	Enable/disable the patient folder operations to match the project requirements
	TODO: Remove project specific hardcoded values
-->
<xsl:param name="folder.export_tf" select="'false'" />
<xsl:param name="folder.export_xds" select="'false'" />
<xsl:param name="folder.xds_consumer" select="'false'" />
<xsl:param name="folder.send" select="'false'" />
<xsl:param name="folder.delete" select="'false'" />
<xsl:param name="folder.edit" select="'false'" />
<xsl:param name="folder.move" select="'false'" />
<xsl:param name="folder.add_worklist" select="'false'" />
<xsl:param name="folder.mergepat" select="'false'" />
<xsl:param name="folder.study_permission" select="'false'" />
<xsl:param name="folder.query_has_issuer" select="'false'" />

<xsl:template match="model">
	<form action="foldersubmit.m" method="post" name="myForm"
		accept-charset="UTF-8">
		<input type="hidden" name="form" value="true" />
		<table class="folder_header" border="0" cellspacing="0"
			cellpadding="0" width="100%">
			<td class="folder_header" valign="top">
				<table class="folder_header" border="0" height="30"
					cellspacing="0" cellpadding="0" width="100%">
				  <tr>	
					<td class="folder_header">
						<div title="&ShowPatientsWithoutStudies;">
							<input type="checkbox"
								name="showWithoutStudies" value="true">
								<xsl:if
									test="showWithoutStudies = 'true'">
									<xsl:attribute name="checked" />
								</xsl:if>
							</input>
							<xsl:text>&woStudies;
							</xsl:text>
						</div>
						<div
							title="&ListStudiesOfOnePatientBeginningWithTheMostRecentStudy;">
							<input type="checkbox"
								name="latestStudiesFirst" value="true">
								<xsl:if
									test="latestStudiesFirst = 'true'">
									<xsl:attribute name="checked" />
								</xsl:if>
							</input>
							<xsl:text>&latestStudiesFirst;
							</xsl:text>
						</div>
					</td>
					<td class="folder_header" align="center">
						<xsl:choose>
							<xsl:when test="total &lt; 1">&NoMatchingStudiesFound;
							</xsl:when>
							<xsl:otherwise>&DisplayingStudies;
								<b>
									<xsl:value-of select="offset + 1" />
								</b>
								&to;
								<b>
									<xsl:choose>
										<xsl:when
											test="offset + limit &lt; total">
											<xsl:value-of
												select="offset + limit" />
										</xsl:when>
										<xsl:otherwise>
											<xsl:value-of
												select="total" />
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
						<input type="image" value="Prev" name="prev"
							src="images/prev.gif" alt="prev" border="0"
							title="&PreviousSearchResults;">
							<xsl:if test="offset = 0">
								<xsl:attribute name="disabled">
									disabled
								</xsl:attribute>
							</xsl:if>
						</input>
					</td>
					<td class="folder_header" width="40">
						<input type="image" value="Next" name="next"
							src="images/next.gif" alt="next" border="0"
							title="&NextSearchResults;">
							<xsl:if test="offset + limit &gt;= total">
								<xsl:attribute name="disabled">
									disabled
								</xsl:attribute>
							</xsl:if>
						</input>
					</td>
					<xsl:if test="$folder.edit='true'">
						<td class="folder_header" width="40">
							<a href="patientEdit.m?pk=-1">
								<img src="images/addpat.gif"
									alt="Add Patient" border="0" title="&AddNewPatient;" />
							</a>
						</td>
					</xsl:if>
					<xsl:if test="$folder.mergepat='true'">
						<td class="folder_header" width="40">
							<input type="image" value="Merge"
								name="merge" src="images/merge.gif" alt="merge" border="0"
								title="&MergeSelectedPatients;"
								onclick="return validateChecks(this.form.stickyPat, 'Patient', 2)">
								<xsl:if test="total &lt;= 0">
									<xsl:attribute name="disabled">
										disabled
									</xsl:attribute>
								</xsl:if>
							</input>
						</td>
					</xsl:if>
					<xsl:if test="$folder.move='true'">
						<td class="folder_header" width="40">
							<input type="image" value="Move" name="move"
								src="images/move.gif" alt="move" border="0"
								onclick="return confirm('&MoveSelectedEntities;?')"
								title="&MoveSelectedEntities;">
								<xsl:if test="total &lt;= 0">
									<xsl:attribute name="disabled">
										disabled
									</xsl:attribute>
								</xsl:if>
							</input>
						</td>
					</xsl:if>
					<xsl:if test="$folder.export_tf='true'">
						<td class="folder_header" width="40">
							<input type="image" value="Export"
								name="exportTF" src="images/export_tf.gif" alt="TF Export"
								border="0"
								title="&ExportSelectedEntitiesToTeachingFileSystem;">
								<xsl:if test="total &lt;= 0">
									<xsl:attribute name="disabled">
										disabled
									</xsl:attribute>
								</xsl:if>
							</input>
						</td>
					</xsl:if>
					<xsl:if test="$folder.export_xds='true'">
						<td class="folder_header" width="40">
							<input type="image" value="xdsi"
								name="exportXDSI" src="images/export_xdsi.gif" alt="XDSI Export"
								border="0" title="&ExportSelectedEntitiesToXDSRepository;">
								<xsl:if test="total &lt;= 0">
									<xsl:attribute name="disabled">
										disabled
									</xsl:attribute>
								</xsl:if>
							</input>
						</td>
					</xsl:if>
					<xsl:if test="$folder.delete='true'">
						<td class="folder_header" width="40">
							<input type="image" value="Del" name="del"
								src="images/trash.gif" alt="delete" border="0"
								title="&DeleteSelectedEntities;"
								onclick="return confirm('&DeleteSelectedEntities;?')">
								<xsl:if test="total &lt;= 0">
									<xsl:attribute name="disabled">
										disabled
									</xsl:attribute>
								</xsl:if>
							</input>
						</td>
					</xsl:if>
					<xsl:if test="$folder.send='true'">
						<td class="folder_header" width="40">
							<input type="image" value="Send" name="send"
								src="images/send.gif" alt="send" border="0"
								title="&SendSelectedEntitiesToSpecifiedDestination;"
								onclick="return confirm('&confirmSendSelectedEntitiesTo1;' 
         + document.myForm.destination.options[document.myForm.destination.selectedIndex ].text
         + '&confirmSendSelectedEntitiesTo2;')">
								<xsl:if test="total &lt;= 0">
									<xsl:attribute name="disabled">
										disabled
									</xsl:attribute>
								</xsl:if>
							</input>
						</td>
					</xsl:if>
					<td class="folder_header" width="50">
						<select size="1" name="destination"
							title="&SendDestination;">
							<xsl:for-each select="aets/item">
								<xsl:sort data-type="text"
									order="ascending" select="title" />
								<option>
									<xsl:attribute name="value">
										<xsl:value-of select="title" />
									</xsl:attribute>
									<xsl:if
										test="/model/destination = title">
										<xsl:attribute
											name="selected">
											true
										</xsl:attribute>
									</xsl:if>
									<xsl:value-of select="title" />
									<xsl:if
										test="not(description='')">
										<xsl:text>(</xsl:text>
										<xsl:value-of
											select="description" />
										<xsl:text>)</xsl:text>
									</xsl:if>
								</option>
							</xsl:for-each>
						</select>
					</td>
					<td class="folder_header" width="5">
						<input type="checkbox" name="filterAET"
							value="true" title="&ShowOnlyStudiesFromSelectedSourceAET;">
							<xsl:if test="filterAET = 'true'">
								<xsl:attribute name="checked" />
							</xsl:if>
						</input>
					</td>
					<td class="folder_header" width="5"
						title="&ShowOnlyStudiesFromSelectedSourceAET;">
						&AETFilter;
					</td>
				   </tr>
				   <tr>
				      <td colspan="5">
                        <xsl:if
                            test="$folder.query_has_issuer='true'">
                            <div title="&QueryHasIssuer;">
                                <xsl:text>&QueryHasIssuer_text1;</xsl:text>                            
                            <input type="checkbox" name="hideHasNoIssuerOfPID" value="true"
                                onclick="return toggle(this.form.hideHasNoIssuerOfPID, this.form.hideHasIssuerOfPID)">
                                <xsl:if
                                    test="hideHasNoIssuerOfPID = 'true'">
                                    <xsl:attribute name="checked" />
                                </xsl:if>
                            </input>
                            <xsl:text>&QueryHasIssuer_text2;</xsl:text>
                            <input type="checkbox" name="hideHasIssuerOfPID" value="true"
                                onclick="return toggle(this.form.hideHasIssuerOfPID, this.form.hideHasNoIssuerOfPID)">
                                <xsl:if
                                    test="hideHasIssuerOfPID = 'true'">
                                    <xsl:attribute name="checked" />
                                </xsl:if>
                            </input>
                            <xsl:text>&QueryHasIssuer_text3;</xsl:text>
                            </div>
                        </xsl:if>
				      </td>
				   </tr>
				   <tr><td>&nbsp;</td></tr>
				</table>
				<table class="folder_search" border="0" width="100%"
					cellpadding="0" cellspacing="0">
					<tr>
						<td class="label">&PatientName;:
						</td>
						<td>
							<input size="10" name="patientName"
								type="text" value="{patientName}" />
						</td>
						<td class="label">&PatientID;:
						</td>
						<td>
							<input size="10" name="patientID"
								type="text" title="&formatPatientID;" value="{patientID}" />
                        </td>
						<td></td>
						<xsl:choose>
							<xsl:when test="showStudyIUID='true'">
								<td class="label">&StudyIUID;:
								</td>
								<td>
									<input size="45" name="studyUID"
										type="text" value="{studyUID}" />
								</td>
							</xsl:when>
							<xsl:when test="showSeriesIUID='true'">
								<td class="label">&SeriesIUID;:
								</td>
								<td>
									<input size="45" name="seriesUID"
										type="text" value="{seriesUID}" />
								</td>
							</xsl:when>
							<xsl:otherwise>
								<td class="label">&StudyID;:
								</td>
								<td>
									<input size="10" name="studyID"
										type="text" value="{studyID}" />
								</td>
								<td class="label">&StudyDate;
								</td>
								<td>
									<input size="10"
										name="studyDateRange" type="text" value="{studyDateRange}"
										title="&formatStudyDate;" />
									<input name="studyUID" type="hidden"
										value="" />
								</td>
							</xsl:otherwise>
						</xsl:choose>

						<td class="label">&AccessionNo;:
						</td>
						<td>
							<input size="10" name="accessionNumber"
								type="text" value="{accessionNumber}" />
						</td>
						<td class="label">&Modality;:
						</td>
						<td>
							<xsl:call-template name="modalityList">
								<xsl:with-param name="name">
									modality
								</xsl:with-param>
								<xsl:with-param name="title">&Modality;
								</xsl:with-param>
								<xsl:with-param name="selected"
									select="modality" />
							</xsl:call-template>
						</td>
					</tr>
				</table>
				<xsl:call-template name="overview" />
				<table border="0" cellpadding="0" cellspacing="0"
					width="100%">
					<tr>
						<td>
							<table border="0" cellpadding="0"
								cellspacing="0" width="100%">
								<tbody valign="top">
									<xsl:apply-templates
										select="patients/item" />
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
				<col width="22%" /><!-- pat name -->
				<col width="10%" /><!-- pat id -->
				<col width="12%" /><!-- pat birthdate -->
				<col width="5%" /><!--  patient sex  -->
				<col width="38%" />
				<col width="8%" /><!-- xds, add, inspect, edit, sticky -->
			</colgroup>
			<tr>
				<td class="patient_mark">
					<font size="1">&Patient;
					</font>
				</td>
				<td>
					<font size="1">&Name;:
					</font>
				</td>
				<td>
					<font size="1">&PatientID;:
					</font>
				</td>
				<td>
					<font size="1">&BirthDate;:
					</font>
				</td>
				<td>
					<font size="1">&Sex;:
					</font>
				</td>
				<td></td>
			</tr>
		</table>

		<table class="folder_overview" border="0" cellspacing="0"
			cellpadding="0" width="100%">
			<colgroup>
				<col width="5%" /><!-- margin -->
				<col width="11%" /><!-- Date/time -->
				<col width="12%" /><!-- StudyID -->
				<col width="10%" /><!-- Modalities -->
				<col width="26%" /><!-- Study Desc -->
				<col width="9%" /><!-- Acc No --><!-- 73 -->
				<col width="11%" /><!-- Ref. Physician -->
				<col width="4%" /><!-- Study Status ID -->
				<col width="2%" /><!-- No. of Series -->
				<col width="2%" /><!-- No. of Instances -->
				<col width="8%" /><!-- Webviewer, add, inspect, edit, sticky -->
			</colgroup>
			<tr>
				<td class="study_mark">
					<font size="1">&Study;
					</font>
				</td>
				<td>
					<font size="1">&Date;/&Time;:
					</font>
				</td>
				<td>
					<font size="1">&StudyIDMedia;:
					</font>
				</td>
				<td>
					<font size="1">&Modality;:
					</font>
				</td>
				<td>
					<font size="1">
						<xsl:choose>
							<xsl:when test="showStudyIUID='false'">
								<b>&StudyDescription;
								</b>
								/
								<a title="&ShowStudyIUID;"
									href="foldersubmit.m?showStudyIUID=true&amp;studyID=">
									&StudyIUID;
								</a>
							</xsl:when>
							<xsl:otherwise>
								<a title="&ShowStudyDescription;"
									href="foldersubmit.m?showStudyIUID=false&amp;studyUID=">
									&StudyDescription;
								</a>
								/
								<b>&StudyIUID;
								</b>
							</xsl:otherwise>
						</xsl:choose>
						:
					</font>
				</td>
				<td>
					<font size="1">&AccNo;:
					</font>
				</td>
				<td>
					<font size="1">&RefPhysician;:
					</font>
				</td>
				<td>
					<font size="1">&Status;:
					</font>
				</td>
				<td align="right">
					<font size="1">&NoS;:
					</font>
				</td>
				<td align="right">
					<font size="1">&NoI;:
					</font>
				</td>
				<td>&#160;</td>
			</tr>
		</table>

		<table class="folder_overview" border="0" cellspacing="0"
			cellpadding="0" width="100%">
			<colgroup>
				<col width="5%" /><!-- left margin -->
				<col width="12%" /><!-- Date/Time -->
				<col width="12%" /><!-- Series No -->
				<col width="10%" /><!-- Modality -->
				<col width="35%" /><!-- Series Desc. -->
				<col width="10%" /><!-- Vendor/Model -->
				<col width="6%" /><!-- PPS Status -->
				<col width="2%" /><!-- NOI -->
				<col width="8%" /><!-- web viewer, edit, inspect, sticky -->
			</colgroup>
			<tr>
				<td class="series_mark">
					<font size="1">&Series;
					</font>
				</td>
				<td>
					<font size="1">&Date;/&Time;:
					</font>
				</td>
				<td>
					<font size="1">&SeriesNoMedia;:
					</font>
				</td>
				<td>
					<font size="1">&Modality;:
					</font>
				</td>
				<td>
					<font size="1">
						<xsl:choose>
							<xsl:when test="showSeriesIUID='false'">
								<b>&SeriesDescription;/&BodyPart;
								</b>
								/
								<a title="&ShowSeriesIUID;"
									href="foldersubmit.m?showSeriesIUID=true">
									&SeriesIUID;
								</a>
							</xsl:when>
							<xsl:otherwise>
								<a title="&ShowSeriesDescription;"
									href="foldersubmit.m?showSeriesIUID=false">
									&SeriesDescription;/&BodyPart;
								</a>
								/
								<b>&SeriesIUID;
								</b>
							</xsl:otherwise>
						</xsl:choose>
						:
					</font>
				</td>
				<td>
					<font size="1">&Vendor;/&Model;:
					</font>
				</td>
				<td>
					<font size="1">&PPSStatus;:
					</font>
				</td>
				<td align="right">
					<font size="1">&NoI;:
					</font>
				</td>
				<td align="right">
					<img src="images/plus.gif" alt="+"
						title="&SelectAllStudies;"
						onclick="selectAll( document.myForm,'stickyStudy', true)" />
					<img src="images/minus.gif" alt="-"
						title="&DeselectAll;"
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
				<col width="26%" /><!-- pat name -->
				<col width="10%" /><!-- pat id -->
				<col width="12%" /><!-- pat birthdate -->
				<col width="5%" /><!--  patient sex  -->
				<col width="38%" />
				<col width="8%" /><!-- xds, add, inspect, edit, sticky -->
			</colgroup>
			<xsl:variable name="rowspan"
				select="1+count(descendant::studies/item)" />
			<td class="patient_mark" align="right"
				rowspan="{$rowspan}">
				<xsl:choose>
					<xsl:when test="showStudies='false'">
						<a title="&ShowStudies;"
							href="expandPat.m?patPk={pk}&amp;expand=true">
							<img src="images/plus.gif" border="0"
								alt="+" />
						</a>
					</xsl:when>
					<xsl:otherwise>
						<a title="&HideStudies;"
							href="expandPat.m?patPk={pk}&amp;expand=false">
							<img src="images/minus.gif" border="0"
								alt="-" />
						</a>
					</xsl:otherwise>
				</xsl:choose>
			</td>
			<td title="&PatientName;">
				<strong>
					<xsl:value-of select="patientName" />
				</strong>
				&#160;
			</td>
			<td title="&PatientID;">
				<strong>
					<xsl:value-of select="patientID" />
					<xsl:if test="/model/showIssuerOfPID = 'true' and issuerOfPatientID != ''">
					   <xsl:text>^^^</xsl:text><xsl:value-of select="issuerOfPatientID" />
					</xsl:if>
				</strong>
				&#160;
			</td>
			<td title="&BirthDate;">
				<strong>
					<xsl:value-of select="patientBirthDate" />
				</strong>
				&#160;
			</td>
			<td title="&Sex;">
				<strong>
					<xsl:value-of select="patientSex" />
				</strong>
				&#160;
			</td>
			<td>&#160;</td>
			<td class="patient_mark" align="right">
				<xsl:if
					test="$folder.xds_consumer='true' and /model/XDSConsumer='true'">
					<xsl:text>&XDS;
					</xsl:text>
					<xsl:choose>
						<xsl:when test="showXDS='false'">
							<a title="&ShowXDSDocuments;"
								href="expandXDS.m?patPk={pk}&amp;expand=true">
								<img src="images/plus.gif" border="0"
									alt="+" />
							</a>
						</xsl:when>
						<xsl:otherwise>
							<a title="&HideXDSDocuments;"
								href="expandXDS.m?patPk={pk}&amp;expand=false">
								<img src="images/minus.gif" border="0"
									alt="-" />
							</a>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:if>
				<xsl:if test="$folder.edit='true'">
					<a href="studyEdit.m?patPk={pk}&amp;studyPk=-1">
						<img src="images/add.gif" alt="icon" border="0"
							title="&AddNewStudy;" />
					</a>
					<a href="patientEdit.m?pk={pk}">
						<img src="images/edit.gif" alt="icon" border="0"
							title="&EditPatientAttributes;" />
					</a>
					<xsl:if test="$folder.study_permission='true'">
						<a href="studyPermission.m?patPk={pk}">
							<img src="images/permission.gif" alt="icon"
								border="0" title="&ShowStudyPermissionsForPatient;" />
						</a>
					</xsl:if>
				</xsl:if>
                <a href="inspectDicomHeader.m?patPk={pk}"
                    target="dbAttrs">
                    <img src="images/dbattrs.gif" alt="icon"
                        border="0" title="&ShowPatientAttributesInDB;" />
                </a>
				<input type="checkbox" name="stickyPat" value="{pk}">
					<xsl:if test="/model/stickyPatients/item = pk">
						<xsl:attribute name="checked" />
					</xsl:if>
				</input>
			</td>
		</table>
	</tr>
	<xsl:if test="showXDS='true'">
		<xsl:call-template name="xds_documents" />
	</xsl:if>
	<xsl:variable name="studyOrder">
		<xsl:choose>
			<xsl:when test="/model/latestStudiesFirst = 'true'">descending</xsl:when>
			<xsl:otherwise>ascending</xsl:otherwise>
		</xsl:choose>
	</xsl:variable>
	<xsl:apply-templates select="studies/item">
		<xsl:sort data-type="text" order="{$studyOrder}"
			select="studyDateTime" />
	</xsl:apply-templates>
</xsl:template>

<xsl:template
	match="item[@type='org.dcm4chex.archive.web.maverick.model.StudyModel']">
	<tr>
		<table class="study_line" width="100%" cellpadding="0"
			cellspacing="0" border="0">
			<xsl:variable name="rowspan"
				select="1+count(descendant::series/item)" />
			<colgroup>
				<col width="2%" /><!-- margin -->
				<col width="14%" /><!-- Date/time -->
				<col width="12%" /><!-- StudyID -->
				<col width="10%" /><!-- Modalities -->
				<col width="26%" /><!-- Study Desc -->
				<col width="9%" /><!-- Acc No --><!-- 73 -->
				<col width="11%" /><!-- Ref. Physician -->
				<col width="4%" /><!-- Study Status ID -->
				<col width="2%" /><!-- No. of Series -->
				<col width="2%" /><!-- No. of Instances -->
				<col width="8%" /><!-- Webviewer, add, inspect, edit, sticky -->
			</colgroup>
			<td class="study_mark" align="right" rowspan="{$rowspan}">
				<xsl:choose>
					<xsl:when test="$rowspan=1">
						<a title="&ShowSeries;"
							href="expandStudy.m?patPk={../../pk}&amp;studyPk={pk}&amp;expand=true">
							<img src="images/plus.gif" border="0"
								alt="+" />
						</a>
					</xsl:when>
					<xsl:otherwise>
						<a title="&HideSeries;"
							href="expandStudy.m?patPk={../../pk}&amp;studyPk={pk}&amp;expand=false">
							<img src="images/minus.gif" border="0"
								alt="-" />
						</a>
					</xsl:otherwise>
				</xsl:choose>
			</td>
			<td title="&StudyDateTime;">
				<xsl:value-of select="studyDateTime" />
				&#160;
			</td>
			<td title="&StudyIDMedia;">
				<xsl:value-of select="studyID" />
				<xsl:if test="filesetId != '_NA_'">
					@
					<xsl:value-of select="filesetId" />
				</xsl:if>
				&#160;
			</td>
			<td title="&Modality;">
				<xsl:value-of select="modalitiesInStudy" />
				&#160;
			</td>
			<xsl:choose>
				<xsl:when test="/model/showStudyIUID='false'">
					<td title="&StudyDescription;">
						<xsl:value-of select="studyDescription" />
						&#160;
					</td>
				</xsl:when>
				<xsl:otherwise>
					<td title="&StudyIUID;">
						<xsl:value-of select="studyIUID" />
						&#160;
					</td>
				</xsl:otherwise>
			</xsl:choose>
			<td title="&AccessionNo;">
				<xsl:value-of select="accessionNumber" />
				&#160;
			</td>
			<td title="&RefPhysican;">
				<xsl:value-of select="referringPhysician" />
				&#160;
			</td>
			<td title="&StudyStatusID;" align="center">
				<xsl:choose>
					<xsl:when test="studyStatusImage!=''">
						<img src="{studyStatusImage}" border="0"
							alt="icon" />
					</xsl:when>
					<xsl:when test="studyStatusId!=''">
						<xsl:value-of select="studyStatusId" />
						&#160;
					</xsl:when>
					<xsl:otherwise>&#160;</xsl:otherwise>
				</xsl:choose>
			</td>
			<td title="&NumberOfSeries;" align="center">
				<xsl:value-of select="numberOfSeries" />
				&#160;
			</td>
			<td title="&NumberOfInstances;" align="center">
				<xsl:value-of select="numberOfInstances" />
				&#160;
			</td>
			<td class="study_mark" align="right">
				<xsl:if test="/model/webViewer='true'">
					<xsl:choose>
						<xsl:when test="modalitiesInStudy='SR'"><!-- no webviewer action for SR! -->
						</xsl:when>
						<xsl:when test="modalitiesInStudy='KO'"><!-- no webviewer action if study contains only KO ! -->
						</xsl:when>
						<xsl:otherwise>
							<a
								href="/dcm4chee-webview/webviewer.jsp?studyUID={studyIUID}">
								<xsl:attribute name="onclick">
									<xsl:text>
										return openWin('
									</xsl:text>
									<xsl:value-of
										select="/model/webViewerWindowName" />
									<xsl:text>
										','/dcm4chee-webview/webviewer.jsp?studyUID=
									</xsl:text>
									<xsl:value-of select="studyIUID" />
									<xsl:text>')</xsl:text>
								</xsl:attribute>
								<img src="images/webview.gif" alt="icon"
									border="0" title="&ViewStudyInWebviewer;" />
							</a>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:if>
				<xsl:if test="$folder.edit='true'">
					<xsl:if test="$folder.add_worklist='true'">
						<a href="addWorklist.m?studyPk={pk}">
							<img src="images/worklist.gif" alt="icon"
								border="0" title="&AddWorklistItem;" />
						</a>
					</xsl:if>
					<a
						href="seriesEdit.m?patPk={../../pk}&amp;studyPk={pk}&amp;seriesPk=-1">
						<img src="images/add.gif" alt="icon" border="0"
							title="&AddNewSeries;" />
					</a>
					<a
						href="studyEdit.m?patPk={../../pk}&amp;studyPk={pk}">
						<img src="images/edit.gif" alt="icon" border="0"
							title="&EditStudyAttributes;" />
					</a>
					<xsl:if test="$folder.study_permission='true'">
						<a
							href="studyPermission.m?studyIUID={studyIUID}&amp;patPk={../../pk}">
							<img src="images/permission.gif" alt="icon"
								border="0" title="Show Study Permissions" />
						</a>
					</xsl:if>
				</xsl:if>
                <a href="inspectDicomHeader.m?studyPk={pk}"
                    target="studyAtrrs">
                    <img src="images/dbattrs.gif" alt="icon"
                        border="0" title="&ShowStudyAttributesInDB;" />
                </a>
				<input type="checkbox" name="stickyStudy"
					value="{pk}">
					<xsl:if test="/model/stickyStudies/item = pk">
						<xsl:attribute name="checked" />
					</xsl:if>
				</input>
			</td>
		</table>
	</tr>
	<xsl:apply-templates select="series/item">
		<xsl:sort data-type="number" order="ascending"
			select="seriesNumber" />
	</xsl:apply-templates>
</xsl:template>

<xsl:template
	match="item[@type='org.dcm4chex.archive.web.maverick.model.SeriesModel']">
	<tr>
		<table class="series_line" width="100%" cellpadding="0"
			cellspacing="0" border="0">
			<colgroup>
				<col width="3%" /><!-- left margin -->
				<col width="14%" /><!-- Date/Time -->
				<col width="12%" /><!-- Series No -->
				<col width="10%" /><!-- Modality -->
				<col width="35%" /><!-- Series Desc. -->
				<col width="10%" /><!-- Vendor/Model -->
				<col width="6%" /><!-- PPS Status -->
				<col width="2%" /><!-- NOI -->
				<col width="8%" /><!-- web viewer, edit, inspect, sticky -->
			</colgroup>
			<xsl:variable name="rowspan"
				select="1+count(descendant::instances/item)" />
			<td class="series_mark" align="right"
				rowspan="{$rowspan}">
				<xsl:choose>
					<xsl:when test="$rowspan=1">
						<a title="&ShowInstances;"
							href="expandSeries.m?patPk={../../../../pk}&amp;studyPk={../../pk}&amp;seriesPk={pk}&amp;expand=true">
							<img src="images/plus.gif" border="0"
								alt="+" />
						</a>
					</xsl:when>
					<xsl:otherwise>
						<a title="&HideInstances;"
							href="expandSeries.m?patPk={../../../../pk}&amp;studyPk={../../pk}&amp;seriesPk={pk}&amp;expand=false">
							<img src="images/minus.gif" border="0"
								alt="-" />
						</a>
					</xsl:otherwise>
				</xsl:choose>
			</td>
			<td title="&SeriesDateTime;">
				<xsl:value-of select="seriesDateTime" />
				&#160;
			</td>
			<td title="&SeriesNoMedia;">
				<xsl:value-of select="seriesNumber" />
				<xsl:if test="filesetId != '_NA_'">
					@
					<xsl:value-of select="filesetId" />
				</xsl:if>
				&#160;
			</td>
			<td title="&Modality;">
				<xsl:value-of select="modality" />
				&#160;
			</td>
			<xsl:choose>
				<xsl:when test="/model/showSeriesIUID='false'">
					<td title="&SeriesDescription;/&BodyPart;">
						<xsl:value-of select="seriesDescription" />
						/
						<xsl:value-of select="bodyPartExamined" />
						&#160;
					</td>
				</xsl:when>
				<xsl:otherwise>
					<td title="&SeriesIUID;">
						<xsl:value-of select="seriesIUID" />
						&#160;
					</td>
				</xsl:otherwise>
			</xsl:choose>
			<td title="&Vendor; / &Model;">
				<xsl:value-of select="manufacturer" />
				/
				<xsl:value-of select="manufacturerModelName" />
				&#160;
			</td>
			<td title="&PPSStatus;">
				<xsl:choose>
					<xsl:when test="PPSStatus='DISCONTINUED'">
						<xsl:attribute name="style">
							color: red
						</xsl:attribute>
					</xsl:when>
					<xsl:when test="PPSStatus!=''">
						<xsl:attribute name="style">
							color: black
						</xsl:attribute>
					</xsl:when>
				</xsl:choose>
				<xsl:value-of select="PPSStatus" />
				&#160;
			</td>
			<td title="&NumberOfInstances;" align="center">
				<xsl:value-of select="numberOfInstances" />
				&#160;
			</td>
			<td class="series_mark" align="right">
				<xsl:if test="/model/webViewer='true'">
					<xsl:choose>
						<xsl:when
							test="modality != 'SR' and modality != 'PR' and modality != 'KO' and modality != 'AU' ">
							<a
								href="/dcm4chee-webview/webviewer.jsp?seriesUID={seriesIUID}">
								<xsl:attribute name="onclick">
									<xsl:text>
										return openWin('
									</xsl:text>
									<xsl:value-of
										select="/model/webViewerWindowName" />
									<xsl:text>
										','/dcm4chee-webview/webviewer.jsp?seriesUID=
									</xsl:text>
									<xsl:value-of select="seriesIUID" />
									')
								</xsl:attribute>
								<img src="images/webview.gif" alt="icon"
									border="0" title="&ViewSeriesInWebviewer;" />
							</a>
						</xsl:when>
						<xsl:when test="modality = 'KO'">
							<a
								href="/dcm4chee-webview/webviewer.jsp?seriesUID={seriesIUID}">
								<xsl:attribute name="onclick">
									<xsl:text>
										return openWin('
									</xsl:text>
									<xsl:value-of
										select="/model/webViewerWindowName" />
									<xsl:text>
										','/dcm4chee-webview/webviewer.jsp?seriesUID=
									</xsl:text>
									<xsl:value-of select="seriesIUID" />
									<xsl:text>')</xsl:text>
								</xsl:attribute>
								<img src="images/webview_ko.gif"
									alt="icon" border="0" title="&ViewKeyObjectsInWebviewer;" />
							</a>
						</xsl:when>
					</xsl:choose>
				</xsl:if>
				<xsl:if test="$folder.edit='true'">
					<a
						href="seriesEdit.m?patPk={../../../../pk}&amp;studyPk={../../pk}&amp;seriesPk={pk}">
						<img src="images/edit.gif" alt="icon" border="0"
							title="&EditSeriesAttributes;" />
					</a>
				</xsl:if>
                <a href="inspectDicomHeader.m?seriesPk={pk}"
                    target="dbAttrs">
                    <img src="images/dbattrs.gif" alt="icon"
                        border="0" title="&ShowSeriesAttributesInDB;" />
                </a>
				<input type="checkbox" name="stickySeries"
					value="{pk}">
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
							href="expandInstance.m?expand=true&amp;patPk={../../../../../../pk}&amp;studyPk={../../../../pk}&amp;seriesPk={../../pk}&amp;instancePk={pk}">
							<img src="images/plus.gif" border="0"
								alt="+" />
						</a>
					</xsl:when>
					<xsl:otherwise>
						<a title="&HideFiles;"
							href="expandInstance.m?expand=false&amp;patPk={../../../../../../pk}&amp;studyPk={../../../../pk}&amp;seriesPk={../../pk}&amp;instancePk={pk}">
							<img src="images/minus.gif" border="0"
								alt="-" />
						</a>
					</xsl:otherwise>
				</xsl:choose>
			</td>

			<td title="&ContentDateTime;">
				<xsl:value-of select="contentDateTime" />
				&#160;
			</td>
			<td title="&InstanceNumber;">
				<xsl:value-of select="instanceNumber" />
				&#160;
			</td>
			<td title="&ImageType;">
				<xsl:value-of select="imageType" />
				&#160;
			</td>
			<td title="&PixelMatrix;">
				<xsl:value-of select="photometricInterpretation" />
				&#160;
				<xsl:text></xsl:text>
				<xsl:value-of select="rows" />
				<xsl:text>x</xsl:text>
				<xsl:value-of select="columns" />
				<xsl:text>x</xsl:text>
				<xsl:value-of select="numberOfFrames" />
				<xsl:text></xsl:text>
				<xsl:value-of select="bitsAllocated" />
				<xsl:text>&#160;bits</xsl:text>
			</td>
			<td title="&NumberOfFiles;">
				<xsl:value-of select="numberOfFiles" />
				&#160;
			</td>
			<td title="&RetrieveAET;">
				<xsl:value-of select="retrieveAETs" />
				&#160;
			</td>
			<td title="&SopIUID;">
				<xsl:value-of select="sopIUID" />
				&#160;
			</td>
			<td class="instance_mark" align="right">
				<xsl:choose>
					<xsl:when
						test="availability='ONLINE' or availability='NEARLINE'">
						<a
							href="{/model/wadoBaseURL}wado?requestType=WADO&amp;studyUID={../../../../studyIUID}&amp;seriesUID={../../seriesIUID}&amp;objectUID={sopIUID}"
							target="imageview">
							<img src="images/image.gif" alt="icon"
								border="0" title="&ViewImage;" />
						</a>
						<a href="inspectDicomHeader.m?instancePk={pk}"
							target="dbAttrs">
							<img src="images/dbattrs.gif" alt="icon"
								border="0" title="&ShowInstanceAttributesInDB;" />
						</a>
						<a
							href="{/model/wadoBaseURL}wado?requestType=WADO&amp;contentType=application/dicom%2Bxml&amp;studyUID={../../../../studyIUID}&amp;seriesUID={../../seriesIUID}&amp;objectUID={sopIUID}"
							target="_blank">
							<img src="images/attrs.gif" alt="icon"
								border="0" title="&ShowDICOMAttributes;" />
						</a>
						<a
							href="{/model/wadoBaseURL}wado?requestType=WADO&amp;contentType=application/dicom&amp;studyUID={../../../../studyIUID}&amp;seriesUID={../../seriesIUID}&amp;objectUID={sopIUID}"
							target="_blank">
							<img src="images/save.gif" alt="icon"
								border="0" title="&DownloadDICOMObject;" />
						</a>
					</xsl:when>
					<xsl:otherwise>
						<img src="images/invalid.gif" alt="icon"
							border="0" title="&notOnline;" />
					</xsl:otherwise>
				</xsl:choose>
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
				<col width="13%" />
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
							href="expandInstance.m?expand=true&amp;patPk={../../../../../../pk}&amp;studyPk={../../../../pk}&amp;seriesPk={../../pk}&amp;instancePk={pk}">
							<img src="images/plus.gif" border="0"
								alt="+" />
						</a>
					</xsl:when>
					<xsl:otherwise>
						<a title="&HideFiles;"
							href="expandInstance.m?expand=false&amp;patPk={../../../../../../pk}&amp;studyPk={../../../../pk}&amp;seriesPk={../../pk}&amp;instancePk={pk}">
							<img src="images/minus.gif" border="0"
								alt="-" />
						</a>
					</xsl:otherwise>
				</xsl:choose>
			</td>
			<td title="&CreationDateTime;">
				<xsl:value-of select="presentationCreationDateTime" />
				&#160;
			</td>
			<td title="&InstanceNumber;">
				<xsl:value-of select="instanceNumber" />
				&#160;
			</td>
			<td title="&PresentationDescription;">
				<xsl:value-of select="presentationDescription" />
				&#160;
			</td>
			<td title="&PresentationLabel;">
				<xsl:value-of select="presentationLabel" />
				&#160;
			</td>
			<td title="&NumberOfReferencedImages;">
				<xsl:text>-&gt;</xsl:text>
				<xsl:value-of select="numberOfReferencedImages" />
				&#160;
			</td>
			<td title="&NumberOfFiles;">
				<xsl:value-of select="numberOfFiles" />
				&#160;
			</td>
			<td title="&RetrieveAET;">
				<xsl:value-of select="retrieveAETs" />
				&#160;
			</td>
			<td title="&SopIUID;">
				<xsl:value-of select="sopIUID" />
				&#160;
			</td>
			<td class="instance_mark" align="right">
				<xsl:choose>
					<xsl:when test="availability='ONLINE'">
						<xsl:if
							test="/model/webViewer='true' and ../../modality='PR'">
							<a
								href="/dcm4chee-webview/webviewer.jsp?prUID={sopIUID}">
								<xsl:attribute name="onclick">
									<xsl:text>
										return openWin('
									</xsl:text>
									<xsl:value-of
										select="/model/webViewerWindowName" />
									<xsl:text>
										','/dcm4chee-webview/webviewer.jsp?prUID=
									</xsl:text>
									<xsl:value-of select="sopIUID" />
									<xsl:text>')</xsl:text>
								</xsl:attribute>
								<img src="images/webview_pr.gif"
									alt="icon" border="0"
									title="&ViewImagesWithAppliedPresentationStateInWebviewer;" />
							</a>
						</xsl:if>
						<a href="inspectDicomHeader.m?instancePk={pk}"
							target="dbAttrs">
							<img src="images/dbattrs.gif" alt="icon"
								border="0" title="&ShowInstanceAttributesInDB;" />
						</a>
						<a
							href="{/model/wadoBaseURL}wado?requestType=WADO&amp;contentType=application/dicom%2Bxml&amp;studyUID={../../../../studyIUID}&amp;seriesUID={../../seriesIUID}&amp;objectUID={sopIUID}"
							target="_blank">
							<img src="images/attrs.gif" alt="icon"
								border="0" title="&ShowDICOMAttributes;" />
						</a>
						<a
							href="{/model/wadoBaseURL}wado?requestType=WADO&amp;contentType=application/dicom&amp;studyUID={../../../../studyIUID}&amp;seriesUID={../../seriesIUID}&amp;objectUID={sopIUID}"
							target="_blank">
							<img src="images/save.gif" alt="icon"
								border="0" title="&DownloadDICOMObject;" />
						</a>
					</xsl:when>
					<xsl:otherwise>
						<img src="images/invalid.gif" alt="icon"
							border="0" title="&notOnline;" />
					</xsl:otherwise>
				</xsl:choose>
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
				<col width="16%" />
				<col width="18" />
				<col width="2%" />
				<col width="2%" />
				<col width="2%" />
			</colgroup>

			<xsl:variable name="rowspan"
				select="1+count(descendant::files/item)" />
			<td align="right" rowspan="{$rowspan}">
				<xsl:choose>
					<xsl:when test="$rowspan=1">
						<a title="&ShowFiles;"
							href="expandInstance.m?expand=true&amp;patPk={../../../../../../pk}&amp;studyPk={../../../../pk}&amp;seriesPk={../../pk}&amp;instancePk={pk}">
							<img src="images/plus.gif" border="0"
								alt="+" />
						</a>
					</xsl:when>
					<xsl:otherwise>
						<a title="&HideFiles;"
							href="expandInstance.m?expand=false&amp;patPk={../../../../../../pk}&amp;studyPk={../../../../pk}&amp;seriesPk={../../pk}&amp;instancePk={pk}">
							<img src="images/minus.gif" border="0"
								alt="-" />
						</a>
					</xsl:otherwise>
				</xsl:choose>
			</td>
			<td title="&ContentDateTime;">
				<xsl:value-of select="contentDateTime" />
				&#160;
			</td>
			<td title="&InstanceNumber;">
				<xsl:value-of select="instanceNumber" />
				&#160;
			</td>
			<td title="&DocumentTitle;">
				<xsl:value-of select="documentTitle" />
				&#160;
			</td>
			<td title="&DocumentStatus;">
				<xsl:value-of select="completionFlag" />
				<xsl:text>/</xsl:text>
				<xsl:value-of select="verificationFlag" />
				&#160;
			</td>
			<td title="&NumberOfFiles;">
				<xsl:value-of select="numberOfFiles" />
				&#160;
			</td>
			<td title="&RetrieveAET;">
				<xsl:value-of select="retrieveAETs" />
				&#160;
			</td>
			<td title="&SopIUID;">
				<xsl:value-of select="sopIUID" />
				&#160;
			</td>
			<xsl:choose>
				<xsl:when test="availability='ONLINE'">
					<td class="instance_mark" align="right">
						<xsl:choose>
							<xsl:when
								test="/model/webViewer='true' and sopCUID='1.2.840.10008.5.1.4.1.1.88.59'">
								<a
									href="/dcm4chee-webview/webviewer.jsp?instanceUID={sopIUID}">
									<xsl:attribute name="onclick">
										return openWin('
										<xsl:value-of
											select="/model/webViewerWindowName" />
										','/dcm4chee-webview/webviewer.jsp?instanceUID=
										<xsl:value-of select="sopIUID" />
										')
									</xsl:attribute>
									<img src="images/webview_ko.gif"
										alt="icon" border="0" title="&ViewKeyObjectsInWebviewer;" />
								</a>
							</xsl:when>
							<xsl:otherwise>&#160;</xsl:otherwise>
						</xsl:choose>
					</td>
					<td class="instance_mark" align="right">
						<a
							href="{/model/wadoBaseURL}rid/IHERetrieveDocument?requestType=DOCUMENT&amp;documentUID={sopIUID}&amp;preferredContentType=application/pdf"
							target="SRview">
							<img src="images/sr_pdf.gif" alt="icon"
								border="0" title="&ViewReportAsPDF;" />
						</a>
						<a
							href="{/model/wadoBaseURL}wado?requestType=WADO&amp;studyUID=0&amp;seriesUID=0&amp;objectUID={sopIUID}&amp;contentType=text/html"
							target="SRview">
							<img src="images/sr.gif" alt="icon"
								border="0" title="&ViewReportAsHTML;" />
						</a>
						<a href="xdsiExport.m?docUID={sopIUID}">
							<img src="images/xds.gif" alt="icon"
								border="0" title="&ExportPDFtoXDSRepository;" />
						</a>
						<a href="inspectDicomHeader.m?instancePk={pk}"
							target="dbAttrs">
							<img src="images/dbattrs.gif" alt="icon"
								border="0" title="&ShowInstanceAttributesInDB;" />
						</a>
						<a
							href="{/model/wadoBaseURL}wado?requestType=WADO&amp;contentType=application/dicom%2Bxml&amp;studyUID={../../../../studyIUID}&amp;seriesUID={../../seriesIUID}&amp;objectUID={sopIUID}"
							target="_blank">
							<img src="images/attrs.gif" alt="icon"
								border="0" title="&ShowDICOMAttributes;" />
						</a>
						<a
							href="{/model/wadoBaseURL}wado?requestType=WADO&amp;contentType=application/dicom&amp;studyUID={../../../../studyIUID}&amp;seriesUID={../../seriesIUID}&amp;objectUID={sopIUID}"
							target="_blank">
							<img src="images/save.gif" alt="icon"
								border="0" title="&DownloadDICOMObject;" />
						</a>
					</td>
				</xsl:when>
				<xsl:otherwise>
					<td class="instance_mark" align="right">&#160;</td>
					<td class="instance_mark" align="right">
						<img src="images/invalid.gif" alt="icon"
							border="0" title="&notOnline;" />
					</td>
				</xsl:otherwise>
			</xsl:choose>
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
				<col width="5%" />
				<col width="10%" />
				<col width="20%" />
				<col width="2%" />
			</colgroup>
			<xsl:variable name="rowspan"
				select="1+count(descendant::files/item)" />
			<td align="right" rowspan="{$rowspan}">
				<xsl:choose>
					<xsl:when test="$rowspan=1">
						<a title="&ShowFiles;"
							href="expandInstance.m?expand=true&amp;patPk={../../../../../../pk}&amp;studyPk={../../../../pk}&amp;seriesPk={../../pk}&amp;instancePk={pk}">
							<img src="images/plus.gif" border="0"
								alt="+" />
						</a>
					</xsl:when>
					<xsl:otherwise>
						<a title="&HideFiles;"
							href="expandInstance.m?expand=false&amp;patPk={../../../../../../pk}&amp;studyPk={../../../../pk}&amp;seriesPk={../../pk}&amp;instancePk={pk}">
							<img src="images/minus.gif" border="0"
								alt="-" />
						</a>
					</xsl:otherwise>
				</xsl:choose>
			</td>
			<td title="&ContentDateTime;">
				<xsl:value-of select="contentDateTime" />
				&#160;
			</td>
			<td title="&InstanceNumber;">
				<xsl:value-of select="instanceNumber" />
				&#160;
			</td>
			<td title="&WaveformType;">
				<xsl:value-of select="waveformType" />
				&#160;
			</td>
			<td>&#160;</td>
			<td>&#160;</td>
			<td title="&RetrieveAET;">
				<xsl:value-of select="retrieveAETs" />
				&#160;
			</td>
			<td title="&SopIUID;">
				<xsl:value-of select="sopIUID" />
				&#160;
			</td>
			<td class="instance_mark" align="right">
				<xsl:choose>
					<xsl:when test="availability='ONLINE'">
						<a
							href="{/model/wadoBaseURL}rid/IHERetrieveDocument?requestType=DOCUMENT&amp;documentUID={sopIUID}&amp;preferredContentType=application/pdf"
							target="waveformview">
							<img src="images/waveform.gif" alt="icon"
								border="0" title="&ViewWaveform;" />
						</a>
						<a href="inspectDicomHeader.m?instancePk={pk}"
							target="dbAttrs">
							<img src="images/dbattrs.gif" alt="icon"
								border="0" title="&ShowInstanceAttributesInDB;" />
						</a>
						<a
							href="{/model/wadoBaseURL}wado?requestType=WADO&amp;contentType=application/dicom%2Bxml&amp;studyUID={../../../../studyIUID}&amp;seriesUID={../../seriesIUID}&amp;objectUID={sopIUID}"
							target="_blank">
							<img src="images/attrs.gif" alt="icon"
								border="0" title="&ShowDICOMAttributes;" />
						</a>
						<a
							href="{/model/wadoBaseURL}wado?requestType=WADO&amp;contentType=application/dicom&amp;studyUID={../../../../studyIUID}&amp;seriesUID={../../seriesIUID}&amp;objectUID={sopIUID}"
							target="_blank">
							<img src="images/save.gif" alt="icon"
								border="0" title="&DownloadDICOMObject;" />
						</a>
						<a href="xdsiExport.m?docUID={sopIUID}">
							<img src="images/xds.gif" alt="icon"
								border="0" title="&ExportPDFtoXDSRepository;" />
						</a>
					</xsl:when>
					<xsl:otherwise>
						<img src="images/invalid.gif" alt="icon"
							border="0" title="&notOnline;" />
					</xsl:otherwise>
				</xsl:choose>
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
	match="item[@type='org.dcm4chex.archive.web.maverick.model.EncapsulatedModel']">
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
				<col width="16%" />
				<col width="18" />
				<col width="4%" />
				<col width="2%" />
			</colgroup>

			<xsl:variable name="rowspan"
				select="1+count(descendant::files/item)" />
			<td align="right" rowspan="{$rowspan}">
				<xsl:choose>
					<xsl:when test="$rowspan=1">
						<a title="&ShowFiles;"
							href="expandInstance.m?expand=true&amp;patPk={../../../../../../pk}&amp;studyPk={../../../../pk}&amp;seriesPk={../../pk}&amp;instancePk={pk}">
							<img src="images/plus.gif" border="0"
								alt="+" />
						</a>
					</xsl:when>
					<xsl:otherwise>
						<a title="&HideFiles;"
							href="expandInstance.m?expand=false&amp;patPk={../../../../../../pk}&amp;studyPk={../../../../pk}&amp;seriesPk={../../pk}&amp;instancePk={pk}">
							<img src="images/minus.gif" border="0"
								alt="-" />
						</a>
					</xsl:otherwise>
				</xsl:choose>
			</td>
			<td title="&ContentDateTime;">
				<xsl:value-of select="contentDateTime" />
				&#160;
			</td>
			<td title="&InstanceNumber;">
				<xsl:value-of select="instanceNumber" />
				&#160;
			</td>
			<td title="&DocumentTitle;">
				<xsl:value-of select="documentTitle" />
				&#160;
			</td>
			<td title="&MIMEType;">
				<xsl:value-of select="mimeType" />
				&#160;
			</td>
			<td title="&NumberOfFiles;">
				<xsl:value-of select="numberOfFiles" />
				&#160;
			</td>
			<td title="&RetrieveAET;">
				<xsl:value-of select="retrieveAETs" />
				&#160;
			</td>
			<td title="&SopIUID;">
				<xsl:value-of select="sopIUID" />
				&#160;
			</td>
			<xsl:choose>
				<xsl:when test="availability='ONLINE'">
					<td class="instance_mark" align="right">
						<a
							href="{/model/wadoBaseURL}rid/IHERetrieveDocument?requestType=DOCUMENT&amp;documentUID={sopIUID}&amp;preferredContentType={mimeType}"
							target="SRview">
							<img src="images/sr_pdf.gif" alt="icon"
								border="0" title="&ViewDocument;" />
						</a>
						<a href="inspectDicomHeader.m?instancePk={pk}"
							target="dbAttrs">
							<img src="images/dbattrs.gif" alt="icon"
								border="0" title="&ShowInstanceAttributesInDB;" />
						</a>
						<a
							href="{/model/wadoBaseURL}wado?requestType=WADO&amp;contentType=application/dicom%2Bxml&amp;studyUID={../../../../studyIUID}&amp;seriesUID={../../seriesIUID}&amp;objectUID={sopIUID}"
							target="_blank">
							<img src="images/attrs.gif" alt="icon"
								border="0" title="&ShowDICOMAttributes;" />
						</a>
						<a
							href="{/model/wadoBaseURL}wado?requestType=WADO&amp;contentType=application/dicom&amp;studyUID={../../../../studyIUID}&amp;seriesUID={../../seriesIUID}&amp;objectUID={sopIUID}"
							target="_blank">
							<img src="images/save.gif" alt="icon"
								border="0" title="&DownloadDICOMObject;" />
						</a>
					</td>
				</xsl:when>
				<xsl:otherwise>
					<td class="instance_mark" align="right">
						<img src="images/invalid.gif" alt="icon"
							border="0" title="&notOnline;" />
					</td>
				</xsl:otherwise>
			</xsl:choose>
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
				&#160;
			</td>
			<td title="&RetrieveAET;">
				<xsl:value-of select="retrieveAET" />
				&#160;
			</td>
			<td title="&Status;">
				<xsl:choose>
					<xsl:when test="fileStatus=0">&OK;
					</xsl:when>
					<xsl:when test="fileStatus=1">&toArchive;
					</xsl:when>
					<xsl:when test="fileStatus=2">&archived;
					</xsl:when>
					<xsl:when test="fileStatus=-1">&compressionFailed;
					</xsl:when>
					<xsl:when test="fileStatus=-2">&compressionVerificationFailed;
					</xsl:when>
					<xsl:when test="fileStatus=-3">&MD5CheckFailed;
					</xsl:when>
					<xsl:when test="fileStatus=-4">&HSMQueryFailed;
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="fileStatus" />
					</xsl:otherwise>
				</xsl:choose>
				&#160;
			</td>
			<td title="&FileSize;">
				<xsl:value-of select="fileSize" />
				<xsl:text>&#160;bytes&#160;</xsl:text>
			</td>
			<td title="&FilePath;">
				<xsl:value-of select="directoryPath" />
				<xsl:text>/</xsl:text>
				<xsl:value-of select="filePath" />
				&#160;
			</td>
			<td title="&MD5Sum;">
				<xsl:value-of select="md5String" />
				&#160;
			</td>
			<td>
				<xsl:if
					test="position()=1 and ../../availability='ONLINE'">
					<a
						href="{/model/wadoBaseURL}wado?requestType=WADO&amp;contentType=application/dicom%2Bxml&amp;useOrig=true&amp;studyUID=1&amp;seriesUID=1&amp;objectUID={../../sopIUID}"
						target="_blank">
						<img src="images/attrs.gif" alt="icon"
							border="0" title="&ShowOriginalReceivedDICOMAttributes;" />
					</a>
					<a
						href="{/model/wadoBaseURL}wado?requestType=WADO&amp;contentType=application/dicom&amp;useOrig=true&amp;studyUID={../../../../../../studyIUID}&amp;seriesUID={../../../../seriesIUID}&amp;objectUID={../../sopIUID}"
						target="_blank">
						<img src="images/save.gif" alt="icon" border="0"
							title="&DownloadOriginalReceivedDICOMObject;" />
					</a>
				</xsl:if>
			</td>
		</table>
	</tr>
</xsl:template>


<xsl:template name="xds_documents">
	<tr>
		<table class="xds_docs" width="100%">
			<colgroup>
				<col width="2%" />
				<col width="30%" />
				<col width="10%" />
				<col width="10%" />
				<col width="13%" />
				<col width="40%" />
				<col width="5%" />
			</colgroup>
			<tr>
				<td class="xds_docs_nav" title="&XDSDocuments;"
					colspan="7">&XDSDocuments;:&#160;&#160;
					<a
						href="xdsQuery.m?queryType=findDocuments&amp;patPk={pk}">
						<img src="images/search.gif" alt="icon"
							border="0" title="&QueryForXDSDocuments;" />
					</a>
					<a
						href="xdsQuery.m?queryType=clearDocumentList&amp;patPk={pk}">
						<img src="images/loeschen.gif" alt="icon"
							border="0" title="&ClearListOfXDSDocuments;" />
					</a>
				</td>
			</tr>
			<tr>
				<td class="xds_doc_header">&#160;</td>
				<td class="xds_doc_header">&DocumentTitle;
				</td>
				<td class="xds_doc_header">&CreationDateTime;
				</td>
				<td class="xds_doc_header">&DocumentStatus;
				</td>
				<td class="xds_doc_header">&MIMEType;
				</td>
				<td class="xds_doc_header">&DocumentID;
				</td>
				<td class="xds_doc_header">&#160;</td>
			</tr>
			<xsl:apply-templates select="XDSDocuments/item" mode="xds" />
		</table>
	</tr>
</xsl:template>

<xsl:template match="item[@type='java.lang.String']" mode="xds">
	<tr>
		<td title="&XDSDocument;">
			<xsl:value-of select="." />
		</td>
	</tr>
</xsl:template>
<xsl:template
	match="item[@type='org.dcm4chex.archive.web.maverick.xdsi.XDSDocumentObject']"
	mode="xds">
	<tr>
		<td title="">&#160;</td>
		<td title="&DocumentTitle;">
			<xsl:value-of select="name" />
			&#160;
		</td>
		<td title="&CreationDateTime;">
			<xsl:value-of select="creationTime" />
			&#160;
		</td>
		<td title="&DocumentStatus;">
			<xsl:value-of select="statusAsString" />
			&#160;
		</td>
		<td title="&MIMEType;">
			<xsl:value-of select="mimeType" />
			&#160;
		</td>
		<td title="&DocumentID;">
			<xsl:value-of select="id" />
			&#160;
		</td>
		<td>
			<xsl:choose>
				<xsl:when test="mimeType='application/dicom'">
					<a
						href="showManifest.m?url={URL}&amp;documentID={id}"
						target="xdsManifest">
						<img src="images/image.gif" alt="icon"
							border="0" title="&OpenXDSIManifest;" />
					</a>
					<xsl:if test="/model/webViewer='true'">
						<a
							href="/dcm4chee-webview/webviewer.jsp?manifestURL={URL}">
							<xsl:attribute name="onclick">
								<xsl:text>return openWin('</xsl:text>
								<xsl:value-of
									select="/model/webViewerWindowName" />
								<xsl:text>
									','/dcm4chee-webview/webviewer.jsp?manifestURL=
								</xsl:text>
								<xsl:value-of select="URL" />
								<xsl:text>')</xsl:text>
							</xsl:attribute>
							<img src="images/webview.gif" alt="icon"
								border="0" title="&ViewManifestInWebviewer;" />
						</a>
					</xsl:if>
				</xsl:when>
				<xsl:when test="mimeType='application/pdf'">
					<a href="{URI}" target="xdsdoc">
						<img src="images/sr_pdf.gif" alt="icon"
							border="0" title="&OpenPDFDocument;" />
					</a>
				</xsl:when>
				<xsl:otherwise>
					<a href="{URI}" target="xdsdoc">
						<img src="images/sr.gif" alt="icon" border="0"
							title="&OpenXDSDocument;" />
					</a>
				</xsl:otherwise>
			</xsl:choose>
		</td>
	</tr>
</xsl:template>

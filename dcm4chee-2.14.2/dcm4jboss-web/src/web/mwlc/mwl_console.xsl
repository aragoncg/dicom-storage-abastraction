<xsl:stylesheet version="1.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="html" indent="yes" encoding="UTF-8"/>
<xsl:variable name="page_title">Modality Worklist Console</xsl:variable>
<xsl:include href="../page.xsl"/>
<xsl:include href="../modality_sel.xsl"/>
<xsl:template match="model">
<!-- Filter -->
	<form action="mwl_console.m" method="post" name="myForm" accept-charset="UTF-8">
		<table border="0" cellspacing="0" cellpadding="0" width="100%" bgcolor="eeeeee">
			<td valign="top">
				<table border="0" height="30" cellspacing="0" cellpadding="0" width="100%">
					<td bgcolor="eeeeee" align="center">
						<xsl:if test="total &gt; 0">
							<b>Modality Worklist:</b> Displaying procedure step 
							<b>
								<xsl:value-of select="offset + 1"/>
							</b>
								to
							<b>
								<xsl:choose>
									<xsl:when test="offset + limit &lt; total">
										<xsl:value-of select="offset + limit"/>
									</xsl:when>
									<xsl:otherwise>
										<xsl:value-of select="total"/>												
									</xsl:otherwise>
								</xsl:choose>
							</b>
								of
							<b>
								<xsl:value-of select="total"/>
							</b>matching procedure steps.
						</xsl:if>
						<xsl:if test="total = 0">
							<b>Modality Worklist:</b> No matching procedure steps found!
						</xsl:if>
 					</td>
					<xsl:if test="/model/linkMode > 0">
						<td title="MPPS Link mode" class="mppsLinkMode">
							LINK MPPS (id:<xsl:value-of select="mppsIUID"/>) to a MWL entry!&#160;&#160;
							<a href="mwl_console.m?action=cancelLink&amp;mppsIUID={mppsIUID}">
								<img src="images/cancel.gif" alt="cancel" border="0" title="Cancel LINK mode!"/>		
							</a>
						</td>	
					</xsl:if>

					<td width="150" bgcolor="eeeeee">
					</td>
					<td width="40" bgcolor="eeeeee">
						<input type="image" value="Search" name="filter" src="images/search.gif" border="0"
						 	title="New Search"/>
					</td>
					<td width="40" bgcolor="eeeeee">
						<xsl:if test="offset &gt; 0">
							<a href="mwl_console.m?nav=prev">
								<img src="images/prev.gif" alt="prev" border="0" title="Previous Search Results"/>		
							</a>
						</xsl:if>
					</td>
					<td width="40" bgcolor="eeeeee">
						<xsl:if test="offset + limit &lt; total">
							<a href="mwl_console.m?nav=next">
								<img src="images/next.gif" alt="next" border="0" title="Next Search Results"/>		
							</a>
						</xsl:if>
					</td>
					<xsl:if test="/model/linkMode = 1">
						<td width="40" bgcolor="eeeeee">
							<input type="image" value="doLink" name="doLink" src="images/link.gif" border="0"
							 	title="link selected MWL to mpps"
							 	onclick="return validateChecks(this.form.stickyPat, 'Modality Worklist', 1)"/>
						</td>
					</xsl:if>
					<td bgcolor="eeeeee">&#160;</td>
					<xsl:if test="/model/local = 'true'">
						<td width="40" bgcolor="eeeeee">
							<input type="image" value="del" name="del" src="images/loeschen.gif" border="0"
							 	title="Delete selected Modality Worklist Entries"
							 	onclick="return confirm('Delete selected Modality Worklist Entries?')"/>
						</td>
					</xsl:if>
				</table>
				<table border="0" cellpadding="0" cellspacing="0" bgcolor="eeeeee">
					<tr valign="top">
						<td bgcolor="eeeeee">&#160;&#160;</td>
						<td bgcolor="eeeeee" class="label">Patient:</td>
						<td bgcolor="eeeeee">
							<input size="10" name="patientName" type="text" value="{filter/patientName}"
								title="Patient name"/>
						</td>
						<td bgcolor="eeeeee">&#160;&#160;</td>
						<td bgcolor="eeeeee" colspan="2" title="Query Start date. format:yyyy/mm/dd">Start&#160;Date: </td>
						<td bgcolor="eeeeee">
							<input size="20" name="startDate" type="text" value="{filter/startDate}"
								title="Query Start date. format:yyyy/mm/dd"/>
						</td>
						<td bgcolor="eeeeee">&#160;&#160;</td>
						<td bgcolor="eeeeee" >Modality: </td>
						<td bgcolor="eeeeee">
							<xsl:call-template name="modalityList">
							    <xsl:with-param name="name">modality</xsl:with-param>
							    <xsl:with-param name="title">Modality</xsl:with-param>
							    <xsl:with-param name="selected" select="filter/modality"/>
							</xsl:call-template>
						</td>
						<td bgcolor="eeeeee">&#160;&#160;</td>
						<td bgcolor="eeeeee" nowrap="nowrap" >Station AET: </td>
						<xsl:if test="filter/onlyGroups!='true'">
							<td bgcolor="eeeeee">
								<input size="10" name="stationAET" type="text" value="{filter/stationAET}"
									title="Station AET"/>
							</td>
						</xsl:if>
						<xsl:if test="count(filter/stationAetGroupNames/item) > 0">
							<xsl:variable name="grpSelectSize">
								<xsl:choose>
									<xsl:when test="count(filter/stationAetGroupNames/item) > 3">
										<xsl:value-of select="3" />
									</xsl:when>
									<xsl:otherwise>
										<xsl:value-of select="count(filter/stationAetGroupNames/item)" />
									</xsl:otherwise>
								</xsl:choose>
							</xsl:variable>
						    <td>
								<select multiple="multiple" name="selectedStationAetGroups" title="Station AET Groups:">
									<xsl:attribute name="size"><xsl:value-of select="$grpSelectSize"/></xsl:attribute>
									<xsl:for-each select="filter/stationAetGroupNames/item">
										<xsl:sort data-type="text" order="ascending" select="@key"/>
											<option>
												<xsl:attribute name="value"><xsl:value-of select="@key"/></xsl:attribute>
												<xsl:if test=".='true'">
													<xsl:attribute name="selected"/>
												</xsl:if> 
												<xsl:value-of select="@key"/>
											</option>
									</xsl:for-each>
								</select>
						    </td>
						</xsl:if>
						<td bgcolor="eeeeee">&#160;&#160;</td>
						<td bgcolor="eeeeee" nowrap="nowrap" >Accession No.: </td>
						<td bgcolor="eeeeee">
							<input size="10" name="accessionNumber" type="text" value="{filter/accessionNumber}"
								title="Accession number"/>
						</td>
						<td width="100%" bgcolor="eeeeee">&#160;</td>
						
					</tr>
				</table>
			</td>
		</table>
		<xsl:call-template name="tableheader"/>
		<xsl:apply-templates select="mwlEntries/item">
			<xsl:sort data-type="number" order="ascending" select="spsID"/>
		</xsl:apply-templates>

</form>
</xsl:template>

<xsl:template name="tableheader">
		
<!-- Header of working list entries -->
<table width="100%" border="0" cellspacing="0" cellpadding="0">

	<table border="0" cellpadding="0" cellspacing="0" width="100%">
		<colgroup>
			<col width="10%"/>
			<col width="7%"/>
			<col width="10%"/>
			<col width="9%"/>
			<col width="20%"/>
			<col width="11%"/>
			<col width="14%"/>
			<col width="15%"/>
			<col width="12%"/>
		</colgroup>
		<tr >
			<td bgcolor="eeeeee" style="height:7px" colspan="11"></td> <!-- spacer -->
		</tr>
		<tr>
			<th title="Scheduled Procedure Step ID" align="left">ID</th>
			<th title="SPS Status" align="left">Status</th>
			<th title="Requested Procedure Step ID" align="left">Req. Proc. ID</th>
			<th title="Accession Number" align="left">Acc. No.</th>
			<th title="Requested Procedure Description" align="left">Proc. Desc.</th>
			<th title="Scheduled Step Start Date" align="left">Start Date</th>
			<th title="Sched. Station: (&lt;Name&gt;-&lt;AET&gt;[&lt;Mod.&gt;]" align="left">Station</th>
			<th title="Patient: Name and ID" align="left">Patient</th>
			<xsl:choose>
				<xsl:when test="local = 'true'">
					<th nowrap="nowrap">Function</th>	
				</xsl:when>
				<xsl:otherwise>
					<th >&#160;&#160;</th>
				</xsl:otherwise>
			</xsl:choose>
		</tr>
	</table>
	
	<table border="0" cellpadding="0" cellspacing="0" width="100%">
		<colgroup>
			<col width="1%"/>
			<col width="15%"/>
			<col width="16%"/>
			<col width="27%"/>
			<col width="15%"/>
			<col width="8%"/>
			<col width="10%"/>
			<col width="4%"/>
			<col width="12%"/>
		</colgroup>
		<tr>
			<th align="left">&#160;&#160;</th> <!-- intend -->
			<th title="Study Instance UID" align="left">StudyIUID</th>
			<th title="Filler and Placer Order Number" align="left">Filler/Placer Order</th>
			<th title="Scheduled Procedure Step Description (protocol)" align="left">SPS Desc.</th>
			<th title="Name of the patient's referring physician" align="left">Ref. Physician</th>
			<th title="Admission No.: Identification number of the visit" align="left">Adm. ID</th>
			<th title="Patients Birthdate" align="left">Birthdate</th>
			<th title="Sex of the patient" align="left">Sex</th>
			<th align="left">&#160;&#160;</th> <!-- function -->
		</tr>
		<tr >
			<td bgcolor="eeeeee" style="height:5px" colspan="11"></td> <!-- spacer -->
		</tr>
	</table>

</table>

</xsl:template>

<!-- List of working list entries ( scheduled procedur steps ) -->

<xsl:template match="item[@type='org.dcm4chex.archive.web.maverick.mwl.model.MWLEntry']">

<table width="100%" border="0" cellspacing="0" cellpadding="0">
	
	<table border="0" cellpadding="0" cellspacing="0" width="100%">
		<colgroup>
			<col width="10%"/>
			<col width="7%"/>
			<col width="10%"/>
			<col width="9%"/>
			<col width="20%"/>
			<col width="11%"/>
			<col width="14%"/>
			<col width="15%"/>
			<col width="6%"/>
			<col width="3%"/>
			<col width="3%"/>
		</colgroup>
		<tr>
	        <td align="left" title="SPS ID" >
				<xsl:value-of select="spsID"/>
	        </td>
	        <td align="left" title="SPS Status" >
				<xsl:value-of select="spsStatus"/>
	        </td>
	        <td align="left" title="Req. Procedure ID" >
				<xsl:value-of select="reqProcedureID"/>
		 	</td>
	        <td align="left" title="Accession No." >
				<xsl:value-of select="accessionNumber"/>
		 	</td>
	        <td align="left" title="Proc. Desc.">
				<xsl:value-of select="reqProcedureDescription"/>
		 	</td>
	        <td align="left" title="Start Date" >
				<xsl:value-of select="spsStartDateTime"/>
	        </td>
	        <td align="left" title="Station" >
				<xsl:if test="string-length(stationName) > 0">
					<xsl:value-of select="stationName"/> -
				</xsl:if>
				<xsl:value-of select="stationAET"/>[<xsl:value-of select="modality"/>]
	        </td>
	        <td align="left" title="Patient" >
				<a href="foldersubmit.m?patientID={patientID}&amp;accessionNumber=&amp;patientName=&amp;studyID=&amp;studyDateRange=&amp;modality=&amp;filter.x=1&amp;trashFolder=false">
					<xsl:value-of select="patientName"/> [<xsl:value-of select="patientID"/>]
				</a>
			</td>
			<td title="Function" align="center" valign="bottom">
				<xsl:if test="/model/linkMode > 0">
					&#160;
					<a href="mwl_console.m?action=doLink&amp;spsID={rqSpsID}&amp;mppsIUID={/model/mppsIUID}">
						<xsl:attribute name="onclick">return confirm('Link this worklist entry <xsl:value-of select="spsID"/> with MPPS <xsl:value-of select="/model/mppsIUID"/> ?')</xsl:attribute>
						<img src="images/link.gif" alt="link" border="0" title="Link this worklist entry with a MPPS !"/>		
					</a>
				</xsl:if>
			</td>	
			<td title="Function">
				<a href="mwl_console.m?action=inspect&amp;spsID={rqSpsID}" target="_blank">
					<img src="images/attrs.gif" alt="attrs" border="0" title="Show MWL item attributes"/>		
				</a>
			</td>
			<td title="Function" align="center" valign="bottom">
				&#160;
				<xsl:if test="/model/linkMode > 0 or /model/local = 'true'">
					<input type="checkbox" name="sticky" value="{rqSpsID}" />
				</xsl:if>
			</td>	
		</tr>
	</table>

	<table border="0" cellpadding="0" cellspacing="0" width="100%">
		<colgroup>
			<col width="1%"/>
			<col width="15%"/>
			<col width="16%"/>
			<col width="27%"/>
			<col width="15%"/>
			<col width="8%"/>
			<col width="10%"/>
			<col width="4%"/>
			<col width="12%"/>
		</colgroup>
		<tr>
			<td>&#160;&#160;</td><!-- intend -->
	        <td title="StudyIUID:{studyUID}">
				<a href="foldersubmit.m?studyUID={studyUID}&amp;filter.x=1">
				    <xsl:variable name="uid"><xsl:value-of select="studyUID"/></xsl:variable>
				    <xsl:variable name="uid_len"><xsl:value-of select="string-length($uid)"/></xsl:variable>
					<xsl:choose>
						<xsl:when test="$uid_len &lt; 28">
							<xsl:value-of select="studyUID"/>
						</xsl:when>
						<xsl:otherwise>
							...<xsl:value-of select="substring($uid,$uid_len - 20)"/>
						</xsl:otherwise>
					</xsl:choose>
				</a>
		 	</td>
	        <td title="Filler/Placer Order">
				<xsl:value-of select="fillerOrderNumber"/>/<xsl:value-of select="placerOrderNumber"/>
		 	</td>
	        <td title="SPS Desc.">
				<xsl:value-of select="SPSDescription"/>
		 	</td>
	        <td title="Ref. Physician">
				<xsl:value-of select="referringPhysicianName"/>
		 	</td>
	        <td title="Admission ID">
				<xsl:value-of select="admissionID"/>
		 	</td>
	        <td title="Birthday">
				<xsl:value-of select="patientBirthDate"/>
		 	</td>
	        <td title="Birthday">
				<xsl:value-of select="patientSex"/>
		 	</td>
			<td>&#160;&#160;</td><!-- function -->
		</tr>
		<tr >
			<td bgcolor="eeeeee" style="height:5px" colspan="11"></td> <!-- spacer -->
		</tr>
	</table>

</table>
</xsl:template>
	   
</xsl:stylesheet>


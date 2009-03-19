<xsl:stylesheet version="1.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="html" indent="yes" encoding="UTF-8"/>
<xsl:variable name="page_title">Modality Performed Procedure Step - MPPS Console</xsl:variable>
<xsl:include href="../page.xsl"/>
<xsl:include href="../modality_sel.xsl"/>
<xsl:template match="model">
<!-- Filter -->
	<form action="mpps_console.m" method="post" name="myForm" accept-charset="UTF-8">
		<table border="0" cellspacing="0" cellpadding="0" width="100%" bgcolor="eeeeee">
			<td valign="top">
				<table border="0" height="30" cellspacing="0" cellpadding="0" width="100%">
					<td bgcolor="eeeeee" align="center">
						<xsl:if test="total &gt; 0">
							<b>MPPS Worklist:</b> Displaying procedure step 
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
							<b>MPPS Worklist:</b> No matching procedure steps found!
						</xsl:if>
 					</td>

					<td width="150" bgcolor="eeeeee">
					</td>
					<td width="40" bgcolor="eeeeee">
						<input type="image" value="Search" name="filter" src="images/search.gif" border="0"
						 	title="New Search"/>
					</td>
					<td width="40" bgcolor="eeeeee">
						<xsl:if test="offset &gt; 0">
							<input type="image" value="Search" name="prev" src="images/prev.gif" border="0"
						 		title="Previous Search Results"/>
						</xsl:if>
					</td>
					<td width="40" bgcolor="eeeeee">
						<xsl:if test="offset + limit &lt; total">
							<input type="image" value="Search" name="next" src="images/next.gif" border="0"
						 		title="Next Search Results"/>
						</xsl:if>
					</td>
					<xsl:if test="/model/filter/emptyAccNo='true'">
						<td width="40" bgcolor="eeeeee">	
							<input type="image" name="link" value="link" src="images/link.gif" 
									border="0" title="Link one ore more MPPS to a MWL entry"
									onclick="return validateChecks(this.form.mppsIUID, 'MPPS', 1)">
							<xsl:if test="total &lt;= 0">
								<xsl:attribute name="disabled">disabled</xsl:attribute>
							</xsl:if>
							</input>
						</td>
					</xsl:if>
				</table>
				<table border="0" cellpadding="0" cellspacing="0" bgcolor="eeeeee">
					<tr valign="top">
						<td bgcolor="eeeeee">&#160;&#160;</td>
						<td bgcolor="eeeeee" class="label">Patient:</td>
						<td bgcolor="eeeeee">
							<input size="10" name="patientName" type="text" value="{filter/patientName}"/>
						</td>
						<td bgcolor="eeeeee">&#160;&#160;</td>
						<td bgcolor="eeeeee" colspan="2" title="Query Start date. format:yyyy/mm/dd">Start&#160;Date: </td>
						<td bgcolor="eeeeee">
							<input size="20" name="startDate" 
									title="Query Start date. format:yyyy/mm/dd" 
									type="text" value="{filter/startDate}"/>
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
						<td bgcolor="eeeeee" nowrap="nowrap" >Acc. No.: (</td>
						<td bgcolor="eeeeee">
							<input type="checkbox" name="emptyAccNo" value="true">
								<xsl:if test="/model/filter/emptyAccNo = 'true'">
									<xsl:attribute name="checked"/>
								</xsl:if>
							</input>
						</td>
						<td bgcolor="eeeeee" nowrap="nowrap" >unscheduled) </td>
						<td bgcolor="eeeeee">
							<input size="10" name="accessionNumber" type="text" value="{filter/accessionNumber}"/>
						</td>
						<td bgcolor="eeeeee" nowrap="nowrap" >Status: </td>
						<td bgcolor="eeeeee">
							<select name="status" >
								<option value="">
									<xsl:if test="filter/status=''"><xsl:attribute name="selected" /></xsl:if>
									[ANY]
								</option>
								<option value="0">
									<xsl:if test="filter/status=0"><xsl:attribute name="selected" /></xsl:if>
									IN PROGRESS
								</option>
								<option value="1">
									<xsl:if test="filter/status=1"><xsl:attribute name="selected" /></xsl:if>
									COMPLETED
								</option>
								<option value="2">
									<xsl:if test="filter/status=2"><xsl:attribute name="selected" /></xsl:if>
									DISCONTINUED
								</option>
							</select>
					 						</td>
						<td width="100%" bgcolor="eeeeee">&#160;</td>
						
					</tr>
				</table>
			</td>
		</table>
		<xsl:call-template name="tableheader"/>
		<xsl:apply-templates select="mppsEntries/item">
		  <xsl:sort data-type="text" order="descending" select="ppsStartDateTime" />
		</xsl:apply-templates>
	</form>
</xsl:template>

<xsl:template name="tableheader">
		
<!-- Header of working list entries -->
<table width="100%" border="0" cellspacing="0" cellpadding="0">

	<table border="0" cellpadding="0" cellspacing="0" width="100%">
		<colgroup>
			<col width="17%"/>
			<col width="11%"/>
			<col width="30%"/>
			<col width="20%"/>
			<col width="5%"/>
			<col width="12%"/>
			<col width="5%"/>
		</colgroup>
		<tr >
			<td bgcolor="eeeeee" style="height:7px" colspan="7"></td> <!-- spacer -->
		</tr>
		<tr bgcolor="eeeeee">
			<th title="PatientName: " align="left">Patient</th>
			<th title="Performed Step Start Date" align="left">Start Date</th>
			<th title="Performed Procedure Description" align="left">Proc. Desc.</th>
			<th title="Perf. Station: (&lt;Name&gt;-&lt;AET&gt;[&lt;Mod.&gt;]" align="left">Station</th>
			<th title="Number of Instances: " align="left">NoI</th>
			<th title="Procedure Step Status" align="left">Status</th>
			<th nowrap="nowrap">Function</th>
		</tr>
	</table>
	<table border="0" cellpadding="0" cellspacing="0" width="100%">
		<colgroup>
			<col width="5%"/>
			<col width="35%"/>
			<col width="35%"/>
			<col width="13%"/>
			<col width="12%"/>
		</colgroup>
		<tr bgcolor="eeeeee">
			<th >&#160;</th>
			<th title="Scheduled Procedure Step ID" align="left">Scheduled Procedure Step ID</th>
			<th title="Study Instance UID" align="left">Study Instance UID</th>
			<th title="Accession Number" align="left">Accession No.</th>
			<th >&#160;</th>
		</tr>
	</table>
	
</table>

</xsl:template>

<!-- List of MPPS entries ( Modality performed procedure steps ) -->

<xsl:template match="item[@type='org.dcm4chex.archive.web.maverick.mpps.model.MPPSEntry']">

<table width="100%" border="0" cellspacing="0" cellpadding="0">
	
	<table border="0" cellpadding="0" cellspacing="0" width="100%">
		<colgroup>
			<col width="17%"/>
			<col width="11%"/>
			<col width="30%"/>
			<col width="20%"/>
			<col width="5%"/>
			<col width="12%"/>
			<col width="3%"/>
			<col width="2%"/>
		</colgroup>
		<tr >
			<td bgcolor="999999" style="height:2px" colspan="9"></td> <!-- spacer -->
		</tr>
		<tr>
	        <td align="left" title="Patient" >
				<a href="foldersubmit.m?destination=LOCAL&amp;patientID={patientID}&amp;accessionNumber=&amp;patientName=&amp;studyID=&amp;studyDateRange=&amp;modality=&amp;filter.x=1&amp;trashFolder=false">
					<xsl:value-of select="patientName"/>
				</a>
			</td>
	        <td align="left" title="Start Date" >
				<xsl:value-of select="ppsStartDateTime"/>
	        </td>
	        <td align="left" title="PPS Desc.">
				<xsl:value-of select="PPSDescription"/>
		 	</td>
	        <td align="left" title="Station" >
				<xsl:if test="string-length(stationName) > 0">
					<xsl:value-of select="stationName"/> -
				</xsl:if>
				<xsl:value-of select="stationAET"/>[<xsl:value-of select="modality"/>]
	        </td>
	        <td align="left" title="Number of Instances" >
				<xsl:value-of select="numberOfInstances"/>
	        </td>
	        <td align="left" title="PPSStatus" >
				<xsl:value-of select="PPSStatus"/>
		 	</td>
			<xsl:choose> 
				<xsl:when test="accNumbers=''">
					<td title="Function">
						<a href="mpps_console.m?link.x=1&amp;mppsIUID={mppsIUID}&amp;">
							<img src="images/link.gif" alt="link" border="0" title="Link this MPPS entry with a MWL entry"/>		
						</a>
					</td>	
				</xsl:when>
				<xsl:otherwise>
					<td title="Function">
						<a href="mpps_console.m?action=unlink&amp;patientName={patientName}&amp;mppsIUID={mppsIUID}&amp;">
							<img src="images/unlink.gif" alt="unlink" border="0" title="Unlink this MPPS entry from MWL entries"/>		
						</a>
					</td>	
				</xsl:otherwise>
			</xsl:choose>
			<td title="Function">
				<a href="mpps_console.m?action=inspect&amp;mppsIUID={mppsIUID}" target="_blank">
					<img src="images/attrs.gif" alt="attrs" border="0" title="Show MPPS attributes"/>		
				</a>
			</td>
			<td title="Function">
				<input type="checkbox" name="mppsIUID" value="{mppsIUID}">
					<xsl:if test="/model/mppsIUIDs/item = mppsIUID">
						<xsl:attribute name="checked"/>
					</xsl:if>
				</input>
			</td>	
		</tr>
	</table>

</table>
	<xsl:apply-templates select="scheduledStepAttrs/item"/>
</xsl:template>
<!-- List of ScheduleStepAttribute sequence entries (per MPPS entry) -->

<xsl:template match="item[@type='org.dcm4chex.archive.web.maverick.mpps.model.MPPSEntry$SSAttr']">

<table width="100%" border="0" cellspacing="0" cellpadding="0">
	
	<table border="0" cellpadding="0" cellspacing="0" width="100%">
		<colgroup>
			<col width="5%"/>
			<col width="35%"/>
			<col width="35%"/>
			<col width="25%"/>
		</colgroup>
		<tr>
			<td >&#160;&#160;</td>
	        <td  align="left" title="Schedule Procedure Step ID" >
	        	<xsl:value-of select="spsID"/>
			</td>
	        <td  title="StudyIUID">
				<a href="foldersubmit.m?destination=LOCAL&amp;studyUID={studyUID}&amp;accessionNumber=&amp;patientName=&amp;patientID=&amp;studyDateRange=&amp;modality=&amp;filter.x=1&amp;trashFolder=false">
					<xsl:value-of select="studyUID"/>
				</a>
		 	</td>
	        <td  align="left" title="Accession Number" >
				<a href="foldersubmit.m?destination=LOCAL&amp;accessionNumber={accessionNumber}&amp;patientName=&amp;patientID=&amp;studyID=&amp;studyDateRange=&amp;modality=&amp;filter.x=1&amp;trashFolder=false">
					<xsl:value-of select="accessionNumber"/>
				</a>
		 	</td>
		</tr>
		<tr >
			<td style="height:2px" colspan="4"></td> <!-- spacer -->
		</tr>
	</table>

</table>
</xsl:template>
	   
</xsl:stylesheet>


<xsl:stylesheet version="1.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="html" indent="yes" encoding="UTF-8"/>
<xsl:variable name="page_title">General Purpose Worklist Console</xsl:variable>
<xsl:include href="../page.xsl"/>
<xsl:template match="model">
<!-- Filter -->
	<form action="gpwl_console.m" method="post" name="myForm" >
		<table border="0" cellspacing="0" cellpadding="0" width="100%" bgcolor="eeeeee">
			<td valign="top">
				<table border="0" height="30" cellspacing="0" cellpadding="0" width="100%">
					<td bgcolor="eeeeee" align="center">
						<xsl:if test="total &gt; 0">
							<b>General Purpose Worklist:</b> Displaying procedure step 
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
							<b>General Purpose Worklist:</b> No matching procedure steps found!
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
							<a href="gpwl_console.m?nav=prev">
								<img src="images/prev.gif" alt="prev" border="0" title="Previous Search Results"/>		
							</a>
						</xsl:if>
					</td>
					<td width="40" bgcolor="eeeeee">
						<xsl:if test="offset + limit &lt; total">
							<a href="gpwl_console.m?nav=next">
								<img src="images/next.gif" alt="next" border="0" title="Next Search Results"/>		
							</a>
						</xsl:if>
					</td>
					<td bgcolor="eeeeee">&#160;</td>
					<xsl:if test="/model/local = 'true'">
						<td width="40" bgcolor="eeeeee">
							<input type="image" value="del" name="del" src="images/loeschen.gif" border="0"
							 	title="Delete selected General Purpose Worklist Entries"
							 	onclick="return confirm('Delete selected General Purpose Worklist Entries?')"/>
						</td>
					</xsl:if>
				</table>
				<table border="0" cellpadding="0" cellspacing="0" bgcolor="eeeeee">
					<tr>
						<td bgcolor="eeeeee">&#160;&#160;</td>
						<td bgcolor="eeeeee" class="label">Patient:</td>
						<td bgcolor="eeeeee">
							<input size="10" name="patientName" type="text" value="{filter/patientName}"
								title="Patient name"/>
						</td>
						<td bgcolor="eeeeee">&#160;&#160;</td>
						<td bgcolor="eeeeee" colspan="2" title="Query Start date. format:yyyy/mm/dd">Start&#160;Date: </td>
						<td bgcolor="eeeeee">
							<input size="20" name="SPSStartDate" type="text" value="{filter/SPSStartDate}"
								title="Query Start date. format:yyyy/mm/dd"/>
						</td>
						<td bgcolor="eeeeee">&#160;&#160;</td>
						<td bgcolor="eeeeee" nowrap="nowrap" >Workitem: </td>
						<td bgcolor="eeeeee">
							<input size="15" name="workitemCode" type="text" value="{filter/workitemCode}"
								title="Workitem Code"/>
						</td>
						<td bgcolor="eeeeee">&#160;&#160;</td>
						<td bgcolor="eeeeee" nowrap="nowrap" >Accession No.: </td>
						<td bgcolor="eeeeee">
							<input size="10" name="accessionNumber" type="text" value="{filter/accessionNumber}"
								title="Accession number"/>
						</td>
						<td bgcolor="eeeeee">&#160;&#160;</td>
						<td bgcolor="eeeeee" >Status: </td>
						<td bgcolor="eeeeee">
							<select name="status" >
								<option value="">
									<xsl:if test="filter/status=''"><xsl:attribute name="selected" /></xsl:if>
									[ANY]
								</option>
								<option value="SCHEDULED">
									<xsl:if test="filter/status='SCHEDULED'"><xsl:attribute name="selected" /></xsl:if>
									SCHEDULED
								</option>
								<option value="IN PROGRESS">
									<xsl:if test="filter/status='IN PROGRESS'"><xsl:attribute name="selected" /></xsl:if>
									IN PROGRESS
								</option>
								<option value="SUSPENDED">
									<xsl:if test="filter/status='SUSPENDED'"><xsl:attribute name="selected" /></xsl:if>
									SUSPENDED
								</option>
								<option value="COMPLETED">
									<xsl:if test="filter/status='COMPLETED'"><xsl:attribute name="selected" /></xsl:if>
									COMPLETED
								</option>
								<option value="DISCONTINUED">
									<xsl:if test="filter/status='DISCONTINUED'"><xsl:attribute name="selected" /></xsl:if>
									DISCONTINUED
								</option>
							</select>
						</td>
						<td bgcolor="eeeeee">&#160;&#160;</td>
						<td bgcolor="eeeeee" nowrap="nowrap" >priority: </td>
						<td bgcolor="eeeeee">
							<select name="priority" >
								<option value="">
									<xsl:if test="filter/priority=''"><xsl:attribute name="selected" /></xsl:if>
									[ANY]
								</option>
								<option value="LOW">
									<xsl:if test="filter/priority='LOW'"><xsl:attribute name="selected" /></xsl:if>
									LOW
								</option>
								<option value="MEDIUM">
									<xsl:if test="filter/priority='MEDIUM'"><xsl:attribute name="selected" /></xsl:if>
									MEDIUM
								</option>
								<option value="HIGH">
									<xsl:if test="filter/priority='HIGH'"><xsl:attribute name="selected" /></xsl:if>
									HIGH
								</option>
							</select>
						</td>
						<td width="100%" bgcolor="eeeeee">&#160;</td>
						
					</tr>
				</table>
			</td>
		</table>
		<xsl:call-template name="tableheader"/>
		<xsl:apply-templates select="GPWLEntries/item">
			<xsl:sort data-type="number" order="ascending" select="spsID"/>
		</xsl:apply-templates>

</form>
</xsl:template>

<xsl:template name="tableheader">
		
<!-- Header of working list entries -->
<table width="100%" border="0" cellspacing="0" cellpadding="0">

	<table border="0" cellpadding="0" cellspacing="0" width="100%">
		<colgroup>
			<col width="27%"/>
			<col width="10%"/>
			<col width="10%"/>
			<col width="7%"/>
			<col width="7%"/>
			<col width="7%"/>
			<col width="12%"/>
			<col width="18%"/>
			<col width="2%"/>
		</colgroup>
		<tr >
			<td bgcolor="eeeeee" style="height:7px" colspan="9"></td> <!-- spacer -->
		</tr>
		<tr>
			<th title="General Purpose Scheduled Procedure Instance UID" align="left">GPSP IUID</th>
			<th title="Accession Number" align="left">Acc. No.</th>
			<th title="Scheduled Procedure Step ID" align="left">SPS ID</th>
			<th title="GPSPS Status" align="left">Status</th>
			<th title="GPSPS Priority" align="left">Priority</th>
			<th title="Input Availability Flag" align="left">Input Avail.</th>
			<th title="SPS Start Date" align="left">Start Date</th>
			<th title="Patient: Name and ID" align="left">Patient</th>
			<xsl:choose>
				<xsl:when test="local = 'true'">
					<th nowrap="nowrap">Function</th>	
				</xsl:when>
				<xsl:otherwise>
					<th>&#160;&#160;</th>
				</xsl:otherwise>
			</xsl:choose>
		</tr>
	</table>
	
	<table border="0" cellpadding="0" cellspacing="0" width="100%">
		<colgroup>
			<col width="1%"/>
			<col width="15%"/>
			<col width="16%"/>
			<col width="15%"/>
			<col width="22%"/>
			<col width="13%"/>
			<col width="10%"/>
			<col width="4%"/>
			<col width="12%"/>
		</colgroup>
		<tr>
			<th align="left">&#160;&#160;</th> <!-- intend -->
			<th title="Workitem Code" align="left">Work Item</th>
			<th title="Station Names" align="left">Station Name</th>
			<th title="Station Class" align="left">Station Class</th>
			<th title="Human Performers" align="left">Human Performers</th>
			<th title="Expected Completion Date" align="left">Completion Date</th>
			<th title="Patients Birthdate" align="left">Birthdate</th>
			<th title="Sex of the patient" align="left">Sex</th>
			<th align="left">&#160;&#160;</th> <!-- function -->
		</tr>
		<tr >
			<td bgcolor="eeeeee" style="height:5px" colspan="9"></td> <!-- spacer -->
		</tr>
	</table>

</table>

</xsl:template>

<!-- List of working list entries ( scheduled procedur steps ) -->

<xsl:template match="item[@type='org.dcm4chex.archive.web.maverick.gpwl.model.GPWLEntry']">

<table width="100%" border="0" cellspacing="0" cellpadding="0">
	
	<table border="0" cellpadding="0" cellspacing="0" width="100%">
		<colgroup>
			<col width="27%"/>
			<col width="10%"/>
			<col width="10%"/>
			<col width="7%"/>
			<col width="7%"/>
			<col width="7%"/>
			<col width="12%"/>
			<col width="16%"/>
			<col width="2%"/>
			<col width="2%"/>
		</colgroup>
		<tr>
	        <td align="left" title="IUID" >
				<xsl:value-of select="IUID"/>
	        </td>
	        <td align="left" title="Accession No." >
				&#160;<xsl:value-of select="accessionNumber"/>
		 	</td>
	        <td align="left" title="SPS ID" >
				&#160;<xsl:value-of select="spsID"/>
	        </td>
	        <td align="left" title="GPSPSStatus" >
				&#160;<xsl:value-of select="GPSPSStatus"/>
		 	</td>
	        <td align="left" title="GPSPSPriority">
				&#160;<xsl:value-of select="GPSPSPriority"/>
		 	</td>
	        <td align="left" title="InputAvailabilityFlag">
				&#160;<xsl:value-of select="inputAvailabilityFlag"/>
		 	</td>
	        <td align="left" title="Start Date" >
				&#160;<xsl:value-of select="spsStartDateTime"/>
	        </td>
	        <td align="left" title="Patient" >
				<a href="foldersubmit.m?destination=LOCAL&amp;patientID={patientID}&amp;accessionNumber=&amp;patientName=&amp;studyID=&amp;studyDateRange=&amp;modality=&amp;filter.x=1&amp;trashFolder=false">
					&#160;<xsl:value-of select="patientName"/> [<xsl:value-of select="patientID"/>]
				</a>
			</td>
			<td title="Function">
				<a href="gpwl_console.m?action=inspect&amp;gpwlIUID={IUID}" target="_blank">
					<img src="images/attrs.gif" alt="attrs" border="0" title="Show GP-SPS attributes"/>		
				</a>
			</td>	
			<td title="Function" align="center" valign="bottom">
				<input type="checkbox" name="sticky" value="{IUID}" />
			</td>
		</tr>
	</table>

	<table border="0" cellpadding="0" cellspacing="0" width="100%">
		<colgroup>
			<col width="1%"/>
			<col width="15%"/>
			<col width="16%"/>
			<col width="15%"/>
			<col width="22%"/>
			<col width="13%"/>
			<col width="10%"/>
			<col width="4%"/>
			<col width="12%"/>
		</colgroup>
		<tr>
			<td>&#160;&#160;</td><!-- intend -->
	        <td title="Workitem code">
				<xsl:value-of select="workItemCode"/>
		 	</td>
	        <td title="Station Name.">
				&#160;<xsl:value-of select="stationName"/>
		 	</td>
	        <td title="Station Class">
				&#160;<xsl:value-of select="stationClass"/>
		 	</td>
	        <td title="Human Performers">
				&#160;<xsl:value-of select="humanPerformers"/>
		 	</td>
	        <td title="Expected Completion Date">
				&#160;<xsl:value-of select="expectedCompletionDate"/>
		 	</td>
	        <td title="Patient Birthday">
				&#160;<xsl:value-of select="birthdate"/>
		 	</td>
	        <td title="Patient sex">
				&#160;<xsl:value-of select="patientSex"/>
		 	</td>
			<td>&#160;&#160;</td><!-- function -->
		</tr>
		<tr >
			<td bgcolor="eeeeee" style="height:5px" colspan="9"></td> <!-- spacer -->
		</tr>
	</table>
	
	


</table>
</xsl:template>
	   
</xsl:stylesheet>


<xsl:stylesheet version="1.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="html" indent="yes" encoding="UTF-8"/>
<xsl:variable name="page_title">Media Creation Managment Console</xsl:variable>
<xsl:include href="../page.xsl"/>
<xsl:template match="model">
<!-- Filter -->
	<form action="mcm_console.m" method="post" name="myForm" accept-charset="UTF-8">
		<table border="0" cellspacing="0" cellpadding="0" width="100%" bgcolor="eeeeee">
			<td valign="top">
				<table border="0" height="30" cellspacing="0" cellpadding="0" width="100%">
					<td bgcolor="eeeeee" align="center">
						<xsl:if test="total &gt; 0">
							Displaying media
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
							</b>matching media.
						</xsl:if>
						<xsl:if test="total = 0">
							No matching media found!
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
							<a href="mcm_console.m?nav=prev">
								<img src="images/prev.gif" alt="prev" border="0" title="Previous Search Results"/>		
							</a>
						</xsl:if>
					</td>
					<td width="40" bgcolor="eeeeee">
						<xsl:if test="offset + limit &lt; total">
							<a href="mcm_console.m?nav=next">
								<img src="images/next.gif" alt="next" border="0" title="Next Search Results"/>		
							</a>
						</xsl:if>
					</td>
				</table>
				<table border="0" width="100%" cellpadding="0" cellspacing="0" bgcolor="eeeeee">
					<tr>
						<td width="20" bgcolor="eeeeee">&#160;</td>
						<td width="130" bgcolor="eeeeee" class="label">Media Status:</td>
						<td width="20" bgcolor="eeeeee">&#160;</td>
						<td width="130" bgcolor="eeeeee" colspan="2">Date: </td>
						<td width="20" bgcolor="eeeeee">&#160;&#160;&#160;</td>
						<td width="20" bgcolor="eeeeee">from: </td>
						<td width="100%" bgcolor="eeeeee">&#160;</td>
					</tr><tr>
						<td width="20" bgcolor="eeeeee" rowspan="3"></td>
						<xsl:variable name="stati" select="filter/selectedStatiAsString"/>
						<td width="130" bgcolor="eeeeee" rowspan="3">
							<select multiple="multiple" size="4" name="mediaStatus" title="Media Status:">
								<!-- additional 'all media types' option -->
								<option value="-all-" >
									<xsl:if test="$stati = '-all-'">
										<xsl:attribute name="selected"/>
									</xsl:if>
									all
								</option>
								<xsl:for-each select="filter/mediaStatusList/item">
									<xsl:sort data-type="number" order="ascending" select="order"/>
										<option>
											<xsl:attribute name="value"><xsl:value-of select="status"/></xsl:attribute>
											<xsl:if test="contains( $stati, concat( ' ',status ) )">
												<xsl:attribute name="selected"/>
											</xsl:if>
											<xsl:value-of select="description"/>
										</option>
								</xsl:for-each>
							</select>
						</td>
						<td width="20" bgcolor="eeeeee" rowspan="3">&#160;</td>
						<td bgcolor="eeeeee">
							<input type="radio" name="createOrUpdateDate" value="all" >
								<xsl:if test="filter/createOrUpdateDate = 'all'">
	                  				<xsl:attribute name="checked">true</xsl:attribute>
								</xsl:if>
							</input>
						</td>
						<td bgcolor="eeeeee">all</td>
						<td width="20" bgcolor="eeeeee" rowspan="3">&#160;</td>
						<td width="90" bgcolor="eeeeee">
							<input size="10" name="startDate" type="text" value="{filter/startDate}"/>
			      		</td>
			      		<xsl:choose>
							<xsl:when test="/model/mcmNotAvail = 'true'">
								<td width="60%" bgcolor="eeeeee" rowspan="3" align="center">
									<table border="1" cellpadding="4" cellspacing="4" bgcolor="eeeeee">
										<tr>										
											<td nowrap="" valign="middle" align="center" bgcolor="ee8888">
												<font color="000000">&#160;Offline Storage Service not available!&#160;<br/>
												<input type="checkbox" value="true" name="checkMCM" border="0"
												 	title="Enable availability check of offline storage.">
													<xsl:if test="/model/checkAvail = 'true'">
							                  				<xsl:attribute name="checked">true</xsl:attribute>
													</xsl:if>Retry to connect to Offline Storage
												</input></font>
											</td>
										</tr>
									</table>
								</td>
								<td width="100%" bgcolor="eeeeee" rowspan="3"/>
							</xsl:when>
							<xsl:otherwise>
								<td width="60%" bgcolor="eeeeee"></td>
								<td width="100%" bgcolor="eeeeee" rowspan="3" />
							</xsl:otherwise>
						</xsl:choose>
					</tr><tr>
						<td bgcolor="eeeeee">
							<input type="radio" name="createOrUpdateDate" value="create" >
								<xsl:if test="filter/createOrUpdateDate = 'create'">
	                  				<xsl:attribute name="checked">true</xsl:attribute>
								</xsl:if>
							</input>
						</td>
						<td nowrap="" bgcolor="eeeeee">Initialized date</td>
						<td bgcolor="eeeeee">To: </td>
						<td width="100%" bgcolor="eeeeee"/>
					</tr><tr>
						<td bgcolor="eeeeee">
							<input type="radio" name="createOrUpdateDate" value="update" bgcolor="eeeeee">
								<xsl:if test="filter/createOrUpdateDate = 'update'">
	                  				<xsl:attribute name="checked">true</xsl:attribute>
								</xsl:if>
							</input>
						</td>
						<td nowrap="" bgcolor="eeeeee">Modified date</td>
						<td width="90" bgcolor="eeeeee">
							<input size="10" name="endDate" type="text" value="{filter/endDate}"/>
						</td>
						<td width="100%" bgcolor="eeeeee" />
					</tr>
			      	<tr height="10">
						<td width="10" bgcolor="eeeeee" />
						<td width="130" bgcolor="eeeeee" />
						<td width="20" bgcolor="eeeeee" />
						<td width="130" bgcolor="eeeeee" />
			      		<td width="90" bgcolor="eeeeee" />
						<td width="10" bgcolor="eeeeee" />
			      		<td width="90" bgcolor="eeeeee" /> 
						<td width="60%" bgcolor="eeeeee" />
						<td width="100%" bgcolor="eeeeee" />
			      	</tr>
				</table>
			</td>
		</table>
<!--	</form> -->
<!-- List of media -->
		<table width="70%" border="0" bordercolor="#ffffff" cellspacing="5" cellpadding="0">
		<tr>	<center>
			<td>
				<tr>
					<td width="20%"><h2>Fileset ID</h2></td>
					<td width="15%"><h2>Initialized</h2></td>
					<td width="15%"><h2>Modified</h2></td>
					<td width="15%"><h2>Usage</h2></td>
					<td width="15%"><h2>Instance status</h2></td>
					<td width="10%"><h2>Media status</h2></td>	
					<td width="10" ><h2>Action</h2></td>	
				</tr>
					<xsl:apply-templates select="mediaList/item">
						<xsl:sort data-type="number" order="descending" select="mediaPk"/>
					</xsl:apply-templates>
			</td>	</center>
		</tr>
		</table>
</form>

</xsl:template>

	<xsl:template match="item[@type='org.dcm4chex.archive.web.maverick.mcmc.model.MediaData']">
		<tr>
	        <td title="Media Fileset ID" >
				<xsl:value-of select="filesetID"/>
			</td>
	        <td title="Creation date" >
				<xsl:value-of select="createdTime"/>
	        </td>
	        <td title="Date of last update" >
				<xsl:value-of select="updatedTime"/>
	        </td>
	        <td title="Media usage: {mediaUsage} Bytes" >
				<xsl:value-of select="mediaUsageWithUnit"/>
	        </td>
	        <td title="Instances online">
				<xsl:choose>
					<xsl:when test="instancesAvailable = 'true'">
						<img src="images/avail.gif" alt="Instances online" border="0" title="All instances online!"/>		
	 				</xsl:when>
					<xsl:otherwise>
						<img src="images/not_avail.gif" alt="Missing instances!" border="0" title="One or more instances are not online!"/>		
					</xsl:otherwise>
				</xsl:choose>
	        </td>
	        <td title="Status info: {mediaStatusString}">
				<xsl:choose>
					<xsl:when test="mediaStatus = 0">
						<img src="images/cdrom-open.gif" alt="Media status: open" border="0" title="Status: open - info: {mediaStatusInfo}"/>		
					</xsl:when>
					<xsl:when test="mediaStatus = 1">
						<img src="images/cdrom-queued.gif" alt="Media status: queued" border="0" title="Status: queued - info: {mediaStatusInfo}"/>		
					</xsl:when>
					<xsl:when test="mediaStatus = 2">
						<img src="images/cdrom-transfering.gif" alt="Media status: transfering" border="0" title="Status: creating - info: {mediaStatusInfo}"/>		
					</xsl:when>
					<xsl:when test="mediaStatus = 3">
						<img src="images/cdrom-creating.gif" alt="Media status: creating" border="0" title="Status: completed - info: {mediaStatusInfo}"/>		
					</xsl:when>
					<xsl:when test="mediaStatus = 4">
						<img src="images/cdrom-completed.gif" alt="Media status: completed" border="0" title="Status: completed - info: {mediaStatusInfo}"/>		
					</xsl:when>
					<xsl:when test="mediaStatus = 999">
						<img src="images/cdrom-failed.gif" alt="Media status: failed" border="0" title="Status: failed - info: {mediaStatusInfo}"/>		
					</xsl:when>
					<xsl:otherwise>
						unknown	(<xsl:value-of select="mediaStatus"/>)											
					</xsl:otherwise>
				</xsl:choose>
				
	        </td>
	        <td title="Action">
	        	<table>
		        	<td width="20">
		        		<xsl:choose>
							<xsl:when test="instancesAvailable = 'false'"><!-- Dont allow burn if not online! -->
								&#160;
			 				</xsl:when>
							<xsl:when test="mediaStatus = /model/statiForQueue and /model/mcmNotAvail = 'false'">
								<a href="mcm_console.m?action=queue&amp;mediaPk={mediaPk}">
									<img src="images/burn.gif" alt="Create media" border="0" title="Create media"/>		
								</a>
			 				</xsl:when>
							<xsl:when test="mediaStatus = 999 and /model/mcmNotAvail = 'false'"><!-- error stati  -->
								<a href="mcm_console.m?action=queue&amp;mediaPk={mediaPk}">
									<img src="images/burn.gif" alt="Retry" border="0" title="Retry"/>		
								</a>
				 			</xsl:when>
				 			<xsl:otherwise>
				 				&#160;
				 			</xsl:otherwise>
		        		</xsl:choose>
			 		</td>
			        <td title="Command">
						<xsl:if test="/model/admin='true' and (mediaStatus = 999 or mediaStatus = 0 or mediaStatus = 4)"><!-- delete enabled if status is either open, completed or error! -->
							<a href="mcm_console.m?action=delete&amp;mediaPk={mediaPk}">
								<xsl:attribute name="onclick">return confirm('Delete media <xsl:value-of select="filesetID"/> ?')</xsl:attribute>
								<img src="images/delete.gif" alt="Delete" border="0" title="Delete media"/>		
							</a>
			 			</xsl:if>
			 		</td>
			 	</table>
		 	</td>
		</tr>
	</xsl:template>
	   
</xsl:stylesheet>


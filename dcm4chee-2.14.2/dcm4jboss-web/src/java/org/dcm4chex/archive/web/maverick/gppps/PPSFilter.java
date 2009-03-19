/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is part of dcm4che, an implementation of DICOM(TM) in
 * Java(TM), available at http://sourceforge.net/projects/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * TIANI Medgraph AG.
 * Portions created by the Initial Developer are Copyright (C) 2003-2005
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * Gunter Zeilinger <gunter.zeilinger@tiani.com>
 * Franz Willer <franz.willer@gwi-ag.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.dcm4chex.archive.web.maverick.gppps;

import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.dict.Tags;
import org.dcm4chex.archive.web.maverick.model.BasicFilterModel;

/**
 * @author franz.willer
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class PPSFilter extends BasicFilterModel {
    
    private String startDate;
    
	public PPSFilter() {
        super();
	}
    
	/**
	 * @return Returns the startDate.
	 */
	public String getStartDate() {
		return startDate;
	}
	public void setStartDate(String startDate) throws ParseException {
        this.startDate = startDate;
        this.setDateRange(ds, Tags.PPSStartDate, startDate);
	}
	
	/**
	 * @return Returns the status.
	 */
	public String getStatus() {
		return ds.getString(Tags.PPSStatus);
	}
	/**
	 * @param status The status to set.
	 */
	public void setStatus(String status) {
		ds.putCS(Tags.PPSStatus, status);
	}
	/**
	 * @return
	 */
	public String getSopIuid() {
		return ds.getString(Tags.SOPInstanceUID);
	}
	
	public void setSopIuid( String uid ) {
		ds.putUI(Tags.SOPInstanceUID, uid);
	}
	/**
	 * @return
	 */
	public String getPatientID() {
		return ds.getString(Tags.PatientID);
	}
	
	public void setPatientID( String id ) {
		ds.putLO(Tags.PatientID, id);
	}

	
	
    public void init() {
        String d = new SimpleDateFormat(DATE_FORMAT).format(new Date());
        try {
            setStartDate( d );
        } catch (ParseException ignore) {
        }
    }
}

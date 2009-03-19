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

package org.dcm4chex.archive.web.maverick.model;


import org.dcm4che.dict.Tags;
import org.dcm4cheri.util.StringUtils;
import org.dcm4chex.archive.common.PrivateTags;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 4863 $ $Date: 2007-08-16 14:42:09 +0200 (Thu, 16 Aug 2007) $
 * @since 05.10.2004
 *
 */
public class StudyFilterModel extends AbstractModel {

	private String name; //hold orig input! in dataset append an asterix!
    public StudyFilterModel() {
    }

    public final String getPatientID() {
        if ( !ds.containsValue(Tags.IssuerOfPatientID))
            return ds.getString(Tags.PatientID);
        else
            return ds.getString(Tags.PatientID)+"^^^"+ds.getString(Tags.IssuerOfPatientID);
    }

    public final void setPatientID(String patientID) {
        String issuer = null;
        if ( patientID != null ) {
            int pos=patientID.indexOf("^^^");
            if ( pos >= 0 ) {
                if ( pos+3 < patientID.length())
                    issuer = patientID.substring(pos+3);
                patientID = pos == 0 ? null : patientID.substring(0,pos);
            }
        }
        ds.putLO(Tags.PatientID, patientID);
        ds.putLO(Tags.IssuerOfPatientID, issuer);
    }

    public final String getPatientName() {
        return name;
    }

    /**
     * Set patient name filter value.
     * <p>
     * Use auto wildcard match to get all patient beginning with given string.
     * <p>
     * This feature is only used if <code>patientName</code> doesn't already 
     * contain a wildcard caracter ('?' or '*')! 
     * 
     * @param patientName
     */
    public final void setPatientName(String patientName) {
    	name = patientName;
    	if ( patientName != null && 
    		 patientName.length() > 0 && 
			 patientName.indexOf('*') == -1 &&
			 patientName.indexOf('?') == -1) patientName+="*";
        ds.putPN(Tags.PatientName, patientName);
    }

    public final String getAccessionNumber() {
        return ds.getString(Tags.AccessionNumber);
    }

    public final void setAccessionNumber(String s) {
        ds.putSH(Tags.AccessionNumber, s);
    }

    public final String getStudyDateRange() {
        return getDateRange(ds, Tags.StudyDate);
    }

    public final void setStudyDateRange(String s) {
        setDateRange(ds, Tags.StudyDate, s);
    }

    public final String getStudyDescription() {
        return ds.getString(Tags.StudyDescription);
    }

    public final void setStudyDescription(String s) {
        ds.putLO(Tags.StudyDescription, s);
    }

    public final String getStudyID() {
        return ds.getString(Tags.StudyID);
    }

    public final void setStudyID(String s) {
        ds.putSH(Tags.StudyID, s);
    }
    
    public final void setStudyUID(String s) {
        ds.putUI(Tags.StudyInstanceUID, s);
    }
    public final void setSeriesUID(String s) {
        ds.putUI(Tags.SeriesInstanceUID, s);
    }

    public final String getModality() {
        return StringUtils.toString(ds.getStrings(Tags.ModalitiesInStudy), '\\');
    }

    public final void setModality(String s) {
        ds.putCS(Tags.ModalitiesInStudy, StringUtils.split(s, '\\'));
    }
    
    public final void setCallingAET(String aet ) {
    	ds.setPrivateCreatorID(PrivateTags.CreatorID);
    	ds.putAE(PrivateTags.CallingAET, aet);
    }
    public final void setCallingAETs(String[] aets ) {
    	ds.setPrivateCreatorID(PrivateTags.CreatorID);
    	ds.putAE(PrivateTags.CallingAET, aets);
    }
    
    /**
     * Returns -1 because pk isnt use here.
     */
    public long getPk() {
    	return -1;
    }

}
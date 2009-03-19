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

package org.dcm4chex.archive.web.maverick;

import java.util.Map;

import org.dcm4che.data.Dataset;
import org.dcm4che.dict.Tags;
import org.dcm4chex.archive.web.maverick.model.PatientModel;

/**
 * @author umberto.cappellini@tiani.com
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 3328 $ $Date: 2007-05-09 21:22:23 +0200 (Wed, 09 May 2007) $
 */
public class PatientMergeCtrl extends Dcm4cheeFormController {

    private long pk;

    private long[] to_be_merged;

    private String merge = null;

    private String cancel = null;
    
    private String view_name = null;

    protected String perform() {
        if ( view_name == null || view_name.length() < 3 ) {
        	view_name = SUCCESS;
        }
        try {
            if (merge != null) executeMerge();
            return view_name;
        } catch (Exception e1) {
            FolderForm.getFolderForm(getCtx()).setExternalPopupMsg("folder.err_merge",new String[]{e1.getMessage()});
            return view_name;
        }
    }
    
    private String makeMergeDesc(Dataset ds) {
        return "Merged with [" + ds.getString(Tags.PatientID) +
        	"]" +  ds.getString(Tags.PatientName);
    }

    
    private void executeMerge() throws Exception {
        long[] priors = new long[to_be_merged.length-1];
        for (int i = 0, j = 0; i < to_be_merged.length; i++) {
            if (to_be_merged[i] != pk)
                priors[j++] = to_be_merged[i];
        }
        ContentEditDelegate delegate = FolderSubmitCtrl.getDelegate(getCtx());
        Map mergedPatMap = delegate.mergePatients( pk, priors);
        if ( mergedPatMap != null ) {
            if ( mergedPatMap.containsKey("ERROR")) {
                FolderForm.getFolderForm(getCtx()).setExternalPopupMsg("folder.err_merge",new String[]{(String) mergedPatMap.get("ERROR")});
                return;
            }
	        Dataset dominant = (Dataset) mergedPatMap.get("DOMINANT");
	        Dataset[] priorPats = (Dataset[]) mergedPatMap.get("MERGED");
	        Dataset prior;
	        for (int i = 0; i < priorPats.length; i++) {
	            prior = priorPats[i];
	            AuditLoggerDelegate.logPatientRecord(getCtx(),
	                    AuditLoggerDelegate.MODIFY,
	                    dominant.getString(Tags.PatientID),
	                    dominant.getString(Tags.PatientName),
	                    makeMergeDesc(prior));
	            AuditLoggerDelegate.logPatientRecord(getCtx(),
	                    AuditLoggerDelegate.DELETE,
	                    prior.getString(Tags.PatientID),
	                    prior.getString(Tags.PatientName),
	                    makeMergeDesc(dominant));
	        }
        }
    }

    public final void setPk(long pk) {
        this.pk = pk;
    }

    public final void setToBeMerged(long[] tbm) {
        this.to_be_merged = tbm;
    }

    public final void setMerge(String merge) {
        this.merge = merge;
    }

	/**
	 * @param view_name The view_name to set.
	 */
	public void setView_name(String view_name) {
		this.view_name = view_name;
	}
	
    public PatientModel getPatient(long ppk) {
        return FolderForm.getFolderForm(getCtx())
                .getPatientByPk(ppk);
    }

}
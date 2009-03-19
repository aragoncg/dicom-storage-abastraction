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

package org.dcm4chex.archive.web.maverick.trash;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.dict.Tags;
import org.dcm4chex.archive.common.PrivateTags;
import org.dcm4chex.archive.web.maverick.BasicFolderForm;
import org.infohazard.maverick.flow.ControllerContext;

/**
 * @author franz.willer@gwi-ag.com
 * @version $Revision: 3160 $ $Date: 2007-02-28 17:36:25 +0100 (Wed, 28 Feb 2007) $
 * @since 19.12.2005
 */
public class TrashFolderForm extends BasicFolderForm {

    static final String FOLDER_ATTRNAME = "trashFolderFrom";

	protected static Logger log = Logger.getLogger(TrashFolderForm.class);
	
	private Dataset ds = DcmObjectFactory.getInstance().newDataset();
	
    static TrashFolderForm getTrashFolderForm(ControllerContext ctx) {
    	HttpServletRequest request = ctx.getRequest();
        TrashFolderForm form = (TrashFolderForm) request.getSession()
                .getAttribute(FOLDER_ATTRNAME);
        if (form == null) {
            form = new TrashFolderForm(request);
            request.getSession().setAttribute(FOLDER_ATTRNAME, form);
            initLimit(ctx.getServletConfig().getInitParameter("limitNrOfStudies"), form);
        }
        initLimit(request.getParameter("limitNrOfStudies"), form);
		form.clearPopupMsg();
        
        return form;
    }
    
	private TrashFolderForm( HttpServletRequest request ) {
    	super(request);
    }
	
    public final String getPatientID() {
        return ds.getString(Tags.PatientID);
    }

    public final void setPatientID(String patientID) {
        ds.putLO(Tags.PatientID, patientID);
    }

    public final String getPatientName() {
        return ds.getString(Tags.PatientName);
    }

    public final void setPatientName(String patientName) {
        ds.putPN(Tags.PatientName, patientName);
    }

	/**
	 * @return Returns the studyUID.
	 */
	public String getStudyUID() {
        return ds.getString(Tags.StudyInstanceUID);
	}
	/**
	 * @param studyUID The studyUID to set.
	 */
	public void setStudyUID(String studyUID) {
        ds.putUI(Tags.StudyInstanceUID, studyUID);
	}
    public final String getAccessionNumber() {
        return ds.getString(Tags.AccessionNumber);
    }

    public final void setAccessionNumber(String accessionNumber) {
        ds.putSH(Tags.AccessionNumber, accessionNumber);
    }

    public final void setCallingAETs(String[] aets ) {
    	ds.setPrivateCreatorID(PrivateTags.CreatorID);
    	ds.putAE(PrivateTags.CallingAET, aets);
    }
    
    public Dataset filterDS() {
    	return ds;
    }
    
	public String getModelName() { return "TRASH"; }

	/* (non-Javadoc)
	 * @see org.dcm4chex.archive.web.maverick.BasicFormPagingModel#gotoCurrentPage()
	 */
	public void gotoCurrentPage() {
		//We doesnt need this method here. TrashFolderCtrl does not use performPrevious/performNext!
	}
	
}
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

import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.util.UIDGenerator;
import org.dcm4chex.archive.ejb.interfaces.ContentManager;
import org.dcm4chex.archive.ejb.interfaces.ContentManagerHome;
import org.dcm4chex.archive.util.EJBHomeFactory;
import org.dcm4chex.archive.web.maverick.model.PatientModel;
import org.dcm4chex.archive.web.maverick.model.StudyModel;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 2659 $ $Date: 2006-07-28 16:55:02 +0200 (Fr, 28 Jul 2006) $
 * @since 5.10.2004
 *
 */
public class InspectDicomHeaderCtrl extends Dcm4cheeFormController {

	private long patPk = -1;
    private long studyPk = -1;
    private long seriesPk = -1;
    private long instancePk = -1;

    public final void setPatPk(long pk) {
        this.patPk = pk;
    }
    public final void setStudyPk(long pk) {
        this.studyPk = pk;
    }
    public final void setSeriesPk(long pk) {
        this.seriesPk = pk;
    }
    public final void setInstancePk(long pk) {
        this.instancePk = pk;
    }

	protected String perform() throws Exception {
	    FolderForm  form = FolderForm.getFolderForm(getCtx());
	    StringBuffer sb = new StringBuffer();
		try {
		    Dataset ds = lookupContentManager().getHeaderInfo(patPk, studyPk, seriesPk, instancePk);
            getCtx().getRequest().getSession().setAttribute("dataset2view", ds);
			if ( instancePk != -1 )
				sb.append("INSTANCE,");
			if ( seriesPk != -1 )
				sb.append("SERIES,");
			if ( studyPk != -1 ) 
				sb.append("STUDY,");
			if ( patPk != -1 ) {
				sb.append("PATIENT,");
			}
            sb.setLength(sb.length()-1);
            getCtx().getRequest().getSession().setAttribute("titleOfdataset2view", 
            		form.formatMessage("folder.dicom_header", new String[]{sb.toString()}));
			return INSPECT;
		} catch ( Exception x ) {
			x.printStackTrace();
			form.setExternalPopupMsg("folder.err_inspect", new String[]{"pks:pat:"+patPk+", study:"+studyPk+
					", series:"+seriesPk+", instance:"+instancePk});
			return ERROR;
		}
	}
	
	private ContentManager lookupContentManager() throws Exception {
	    ContentManagerHome home = (ContentManagerHome) EJBHomeFactory
	         .getFactory().lookup(ContentManagerHome.class,
	               ContentManagerHome.JNDI_NAME);
	    return home.create();
	}
	
}
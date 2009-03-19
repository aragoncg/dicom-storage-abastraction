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

import java.util.List;

import org.dcm4che.data.Dataset;
import org.dcm4chex.archive.ejb.interfaces.ContentManager;
import org.dcm4chex.archive.ejb.interfaces.ContentManagerHome;
import org.dcm4chex.archive.util.EJBHomeFactory;
import org.dcm4chex.archive.web.maverick.Dcm4cheeFormController;
import org.dcm4chex.archive.web.maverick.model.StudyModel;

/**
 * 
 * @author franz.willer@gwi-ag.com
 * @version $Revision: 2531 $ $Date: 2006-06-20 16:49:49 +0200 (Tue, 20 Jun 2006) $
 * @since 19.12.2005
 */
public class ExpandTrashPatientCtrl extends Dcm4cheeFormController {

    protected int patPk;
    protected boolean expand;
    
    public final void setPatPk(int patPk) {
        this.patPk = patPk;
    }

    public final void setExpand(boolean expand) {
        this.expand = expand;
    }

    protected String perform() throws Exception {
        TrashFolderForm folderForm = TrashFolderForm.getTrashFolderForm(getCtx());
        if ( expand ) { 
		    ContentManagerHome home = (ContentManagerHome) EJBHomeFactory
		            .getFactory().lookup(ContentManagerHome.class,
		                    ContentManagerHome.JNDI_NAME);
		    ContentManager cm = home.create();
		    try {
		        List studies = cm.listStudiesOfPrivatePatient(patPk);
		        for (int i = 0, n = studies.size(); i < n; i++)
		            studies.set(i, new StudyModel((Dataset) studies.get(i)));
		        folderForm.getPatientByPk(patPk).setStudies(studies);
		    } finally {
		        try {
		            cm.remove();
		        } catch (Exception e) {
		        }
		    }
        } else {
        	folderForm.getPatientByPk(patPk).getStudies().clear();
        }
        return SUCCESS;
    }

	protected String getCtrlName() {
		return "trash";
	}
    
}
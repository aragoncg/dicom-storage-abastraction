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

package org.dcm4chex.archive.web.maverick.tf;

import javax.servlet.http.HttpServletRequest;

import org.dcm4chex.archive.web.maverick.Dcm4cheeFormController;
import org.dcm4chex.archive.web.maverick.FolderForm;

/**
 * @author franz.willer@gwi-ag.com
 * @version $Revision: 3146 $ $Date: 2007-02-23 09:58:19 +0100 (Fri, 23 Feb 2007) $
 */
public class TFSelectorCtrl extends Dcm4cheeFormController {

    private TeachingFileDelegate delegate = null;
	private static final String CANCEL = "cancel";
	private static final String TFSELECT = "tfselect";

    protected Object makeFormBean() {
    	TFModel model = TFModel.getModel(getCtx().getRequest());
    	if ( delegate == null) { 
    		delegate = new TeachingFileDelegate();
    		try {
	    		delegate.init(getCtx());
	    		model.setDispositions(delegate.getConfiguredDispositions());
	    		model.getManifestModel().setDefaultAuthor( delegate.getObserverPerson(model.getUser()));
	    		model.clear();
    		} catch ( Exception x) {
    			throw new NullPointerException("Cant create TFModel or TF delegate!");
    		}
    	}
    	return model;
    }
    protected String perform() {
    	TFModel model = (TFModel) getForm();
        try {
        	if ( model.getNumberOfInstances() < 1) {
        		FolderForm.setExternalPopupMsg(getCtx(),"tf.err_selection", null);
        		return CANCEL;
        	}
        	model.clearPopupMsg();
        	HttpServletRequest rq = getCtx().getRequest();
        	model.getManifestModel().fillParams(rq);
        	if ( rq.getParameter("cancel") != null || rq.getParameter("cancel.x") != null ) {
        		return CANCEL;
        	}
        	if ( rq.getParameter("clear") != null || rq.getParameter("clear.x") != null ) {
        		model.clear();
        		return TFSELECT;
        	}

            if ( rq.getParameter("export") != null || rq.getParameter("export.x") != null ) {
	        	delegate.exportTF(model);
	        	model.clear();
	        	return SUCCESS;//export done
        	}
            return TFSELECT;//Show selection page for docTitle, delay reason,..
        } catch (Exception x) {
        	model.setPopupMsg("tf.err",x.getMessage());
        	return ERROR;
        }
    }
    

}
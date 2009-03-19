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

package org.dcm4chex.archive.web.maverick.xdsi;

import org.apache.log4j.Logger;
import org.dcm4chex.archive.web.maverick.Dcm4cheeFormController;
import org.dcm4chex.archive.web.maverick.FolderForm;
import org.dcm4chex.archive.web.maverick.model.PatientModel;

/**
 * 
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 6485 $ $Date: 2008-06-18 19:21:54 +0200 (Wed, 18 Jun 2008) $
 * @since 28.01.2004
 */
public class XDSQueryCtrl extends Dcm4cheeFormController {

    private String patPk;
    private String type;

    private static Logger log = Logger.getLogger( XDSQueryCtrl.class.getName() );
    
    public final void setPatPk(String patPk) {
        this.patPk = patPk;
    }
    
    public final void setQueryType( String type ) {
    	this.type = type;
    }

    protected String perform() throws Exception {
    	FolderForm folderForm = FolderForm.getFolderForm(getCtx());
    	PatientModel pat = folderForm.getPatientByPk(Integer.parseInt(patPk));
    	String patId = pat.getPatientID();
    	String issuer = pat.getIssuerOfPatientID();
    	if (type == null) {
            log.warn("No Query type specified! e.g. queryType=findDocuments");
            return SUCCESS;
        }
        log.info("issuer:"+issuer);
        XDSQueryDelegate delegate = XDSQueryDelegate.getInstance(getCtx());
        if ( getCtx().getRequest().getParameter("useRefs") != null ) 
        	delegate.setUseLeafFind(false);
        XDSConsumerModel model = PatientModel.getConsumerModel();
        try {
	        if ( "findDocuments".equals(type)) {
	            delegate.findDocuments(patId, issuer, model);
	            log.info("ConsumerModel after findDocuments:"+model);
	        } else if ("clearDocumentList".equals(type)) {
	        	delegate.clearDocumentList(patId, issuer, model);
	        } else {
	            log.warn("Query type "+type+" not supported!");
	        }
        } catch ( Exception x ) {
        	log.error("XDS Query failed!",x);
        	model.setPopupMsg("xdsi.err",x.getMessage());
        }
        return SUCCESS;
    }

}
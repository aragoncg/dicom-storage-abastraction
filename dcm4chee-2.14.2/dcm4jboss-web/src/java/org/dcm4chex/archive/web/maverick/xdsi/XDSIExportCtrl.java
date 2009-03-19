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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.xml.registry.JAXRException;

import org.apache.log4j.Logger;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.data.FileFormat;
import org.dcm4che.dict.Tags;
import org.dcm4che.dict.UIDs;
import org.dcm4cheri.util.UIDGeneratorImpl;
import org.dcm4chex.archive.util.FileUtils;
import org.dcm4chex.archive.web.maverick.Dcm4cheeFormController;
import org.dcm4chex.archive.web.maverick.FolderForm;

/**
 * @author franz.willer@gwi-ag.com
 * @version $Revision: 7709 $ $Date: 2008-10-22 16:41:47 +0200 (Wed, 22 Oct 2008) $
 */
public class XDSIExportCtrl extends Dcm4cheeFormController {

    private XDSIExportDelegate delegate = null;
    private XDSQueryDelegate qryDelegate = null;

    private static final String XDSI_EXPORT = "xdsi_export";
    private static final String CANCEL = "cancel";

    private static final Logger log = Logger.getLogger( XDSIExportCtrl.class.getName() );

    protected Object makeFormBean() {
        HttpServletRequest rq = getCtx().getRequest();
        XDSIModel model = XDSIModel.getModel(rq);
        model.setConsumerModel( XDSConsumerModel.getModel(rq));
        delegate = XDSIExportDelegate.getInstance(getCtx());
        qryDelegate = XDSQueryDelegate.getInstance(getCtx());
        if ( model.getAuthorRoles() == null ) 
            this.clear(model, true);
        return model;
    }
    protected String perform() {
        XDSIModel model = (XDSIModel) getForm();
        try {
            HttpServletRequest rq = getCtx().getRequest();
            if ( rq.getParameter("docUID") != null ) {
                Set set = new HashSet();
                set.add(rq.getParameter("docUID"));
                model.setInstances(set);
                model.setPdfExport(true);
            } else if (rq.getParameter("export") == null) {
                if ( rq.getParameter("pdfUID") != null ) {
                    Set set = new HashSet();
                    set.add(rq.getParameter("docUID"));
                    model.setPdfUID(rq.getParameter("pdfUID"));
                    model.setPdfExport(false);
                }
                model.setPdfExport(false);
            }
            if ( model.getNumberOfInstances() < 1) {
                FolderForm.setExternalPopupMsg(this.getCtx(),"xdsi.err_selection", null);
                return CANCEL;
            }
            model.clearPopupMsg();
            if ( rq.getParameter("cancel") != null || rq.getParameter("cancel.x") != null ) {
                return CANCEL;
            }
            if ( rq.getParameter("clear") != null || rq.getParameter("clear.x") != null ) {
                clear(model, true);
                return XDSI_EXPORT;
            }
            if ( rq.getParameter("addEventCode") != null || rq.getParameter("addEventCode.x") != null ) {
                model.addSelectedEventCode();
                return XDSI_EXPORT;
            }
            if ( rq.getParameter("delEventCode") != null || rq.getParameter("delEventCode.x") != null ) {
                model.removeSelectedEventCode();
                return XDSI_EXPORT;
            }
            if ( rq.getParameter("deselectAllEventCodes") != null || rq.getParameter("deselectAllEventCodes.x") != null ) {
                model.deselectAllEventCodes();
                return XDSI_EXPORT;
            }

            if ( rq.getParameter("export") != null || rq.getParameter("export.x") != null ) {
                return export(model);
            }

            if ( rq.getParameter("query") != null || rq.getParameter("query.x") != null ) {
                qryDelegate.findDocuments(model);
                return XDSI_EXPORT;
            }
            if ( rq.getParameter("queryFolder") != null || rq.getParameter("queryFolder.x") != null ) {
                qryDelegate.findFolders(model);
                return XDSI_EXPORT;
            }

            if ( rq.getParameter("addAssociation") != null || rq.getParameter("addAssociation.x") != null ) {
                model.addAssociation( null, model.getDocument( Integer.parseInt(rq.getParameter("selectedDocument"))), 
                        rq.getParameter("assocType"), rq.getParameter("assocStatus"));
                return XDSI_EXPORT;
            }
            if ( rq.getParameter("delAssociation") != null || rq.getParameter("addAssociation.x") != null ) {
                model.getAssociations().remove(Integer.parseInt(rq.getParameter("selectedAssociation")));
                return XDSI_EXPORT;
            }
            if ( rq.getParameter("deselectAllAssociations") != null || rq.getParameter("deselectAllAssociations.x") != null ) {
                model.clearAssociations();
                return XDSI_EXPORT;
            }
            if ( rq.getParameter("addFolder") != null || rq.getParameter("addFolder.x") != null ) {
                model.addLinkFolder( 
                        model.getFolder( Integer.parseInt(rq.getParameter("selectedFolder"))));
                return XDSI_EXPORT;
            }
            if ( rq.getParameter("delFolder") != null || rq.getParameter("delFolder.x") != null ) {
                model.getLinkFolders().remove(Integer.parseInt(rq.getParameter("selectedLinkFolder")));
                return XDSI_EXPORT;
            }
            if ( rq.getParameter("deselectAllFolders") != null || rq.getParameter("deselectAllFolders.x") != null ) {
                model.getLinkFolders().clear();
                return XDSI_EXPORT;
            }
            if ( rq.getParameter("createFolder") != null || rq.getParameter("createFolder.x") != null ) {
                return createFolder(rq, model);
            }
            if ( rq.getParameter("submitAndCreateFolder") != null || rq.getParameter("submitAndCreateFolder.x") != null ) {
                return submitAndCreateFolder(rq, model);
            }

            return XDSI_EXPORT;//Show selection page for authorRole, ... selection
        } catch (Exception x) {
            model.setPopupMsg("xdsi.err",x.getMessage());
            return ERROR;
        }
    }

    private String submitAndCreateFolder(HttpServletRequest rq, XDSIModel model) throws Exception {
        Properties props = model.listMetadataProperties();
        try {
            props.setProperty("folder.uniqueId", UIDGeneratorImpl.getInstance().createUID() );
            String folderName = rq.getParameter("folderName");
            String folderComment = rq.getParameter("folderComment");
            if ( folderName != null ) props.setProperty("folder.name", folderName);
            if ( folderComment != null ) props.setProperty("folder.comment", folderComment);
            return export( model );
        } finally {
            props.remove("folder.uniqueId");
            props.remove("folderName");
            props.remove("folderComment");
            props.remove("folder.name");
            props.remove("folder.comment");
        }
    }

    private String export(XDSIModel model) throws Exception {
        if ( !model.getLinkFolders().isEmpty() ) {
            Iterator iter = model.getLinkFolders().iterator();
            StringBuffer sb = new StringBuffer().append(((XDSFolderObject)iter.next()).getId()); 
            for ( ; iter.hasNext() ;) {
                sb.append('|').append( ((XDSFolderObject)iter.next()).getId() );
            }
            model.listMetadataProperties().setProperty("folder_assoc.uniqueId", sb.toString());
            log.debug("folder_assoc.uniqueId:"+sb);
        } else {
            model.listMetadataProperties().remove("folder_assoc.uniqueId");
        }
        if ( ! delegate.exportXDSI(model) ) {
            model.setPopupMsg("xdsi.err_failed","");
            return XDSI_EXPORT;
        }
        clear(model, false);
        FolderForm.setExternalPopupMsg(getCtx(), "xdsi.done", null);
        return SUCCESS;//export done
    }

    private String createFolder(HttpServletRequest rq, XDSIModel model) throws JAXRException {
        Properties props = model.listMetadataProperties();
        try {
            String folderName = rq.getParameter("folderName");
            String folderComment = rq.getParameter("folderComment");
            props.setProperty("folder.patDatasetIUID", (String) model.getInstances().iterator().next() );
            props.setProperty("folder.uniqueId", UIDGeneratorImpl.getInstance().createUID() );
            if ( folderName != null ) props.setProperty("folder.name", folderName);
            if ( folderComment != null ) props.setProperty("folder.comment", folderComment);
            if ( ! delegate.createFolder(model) ) {
                model.setPopupMsg("xdsi.err_failed","");
            } else {

            }
        } catch (Exception x) {
            model.setPopupMsg("xdsi.err",x.getMessage());
            return ERROR;
        } finally {
            props.remove("folder.patDatasetIUID");
            props.remove("folder.uniqueId");
            props.remove("folder.name");
            props.remove("folder.comment");
        }
        return XDSI_EXPORT;
    }

    private void clear(XDSIModel model, boolean reload) {
        model.clear();
        model.setMetadataProperties(delegate.joinMetadataProperties(new Properties()));
        if ( reload ) {
            model.setDocTitles(delegate.getConfiguredDocTitles());
            model.setAuthorRoles(delegate.getConfiguredAuthorRoles());
            model.setEventCodes(delegate.getConfiguredEventCodes());
            model.setClassCodes(delegate.getConfiguredClassCodes());
            model.setContentTypeCodes( delegate.getConfiguredContentTypeCodes());
            model.setHealthCareFacilityTypeCodes( delegate.getConfiguredHealthCareFacilityTypeCodes());
        }    	
    }


}
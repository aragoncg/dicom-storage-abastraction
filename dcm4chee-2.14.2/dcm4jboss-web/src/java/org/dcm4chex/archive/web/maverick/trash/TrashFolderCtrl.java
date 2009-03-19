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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.dcm4che.data.Dataset;
import org.dcm4chex.archive.ejb.interfaces.ContentManager;
import org.dcm4chex.archive.ejb.interfaces.ContentManagerHome;
import org.dcm4chex.archive.ejb.jdbc.QueryPrivateStudiesCmd;
import org.dcm4chex.archive.util.EJBHomeFactory;
import org.dcm4chex.archive.web.maverick.AuditLoggerDelegate;
import org.dcm4chex.archive.web.maverick.ContentEditDelegate;
import org.dcm4chex.archive.web.maverick.FolderCtrl;
import org.dcm4chex.archive.web.maverick.model.InstanceModel;
import org.dcm4chex.archive.web.maverick.model.PatientModel;
import org.dcm4chex.archive.web.maverick.model.SeriesModel;
import org.dcm4chex.archive.web.maverick.model.StudyModel;

/**
 * 
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 7908 $ $Date: 2008-11-04 11:40:47 +0100 (Tue, 04 Nov 2008) $
 * @since 28.01.2004
 */
public class TrashFolderCtrl extends FolderCtrl {

    public static final String TRASH = "trash";
    public static final String LOGOUT = "logout";

    public static final int DELETED = 1;//private type DELETED

    private static ContentEditDelegate delegate = null;

    public static ContentEditDelegate getDelegate() {
        return delegate;
    }

    /**
     * Get the model for the view.
     * @throws 
     */
    protected Object makeFormBean() {
        if ( delegate == null ) {
            delegate = new ContentEditDelegate();
            try {
                delegate.init( getCtx() );
            } catch( Exception x ) {
                log.error("Cant make form bean!", x );
            }
            StudyModel.setHttpRoot(getCtx().getServletContext().getRealPath("/"));//set http root to check if a studyStatus image is available.
        }
        return TrashFolderForm.getTrashFolderForm(getCtx());
    }

    protected String perform() throws Exception {
        try {
            TrashFolderForm folderForm = (TrashFolderForm) getForm();
            if ( getPermissions().getPermissionsForApp("trash") == null ) return FOLDER;
            folderForm.clearPopupMsg();
            setSticky(folderForm.getStickyPatients(), "stickyPat");
            setSticky(folderForm.getStickyStudies(), "stickyStudy");
            setSticky(folderForm.getStickySeries(), "stickySeries");
            setSticky(folderForm.getStickyInstances(), "stickyInst");
            HttpServletRequest rq = getCtx().getRequest();
            if ( rq.getParameter("showWithoutStudies") != null ) {
                folderForm.setShowWithoutStudies( "true".equals( rq.getParameter("showWithoutStudies")));
            }
            if (rq.getParameter("logout") != null || rq.getParameter("logout.x") != null ) 
                return logout();

            rq.getSession().setAttribute("dcm4chee-session", "ACTIVE");
            if (folderForm.getTotal() < 1 || rq.getParameter("filter") != null
                    || rq.getParameter("filter.x") != null) { return query(true); }
            if (rq.getParameter("prev") != null
                    || rq.getParameter("prev.x") != null
                    || rq.getParameter("next") != null
                    || rq.getParameter("next.x") != null) { return query(false); }
            if (rq.getParameter("emptyTrash") != null
                    || rq.getParameter("emptyTrash.x") != null) { 
                emptyTrash(); 
                folderForm.removeStickies();
                query(true);
                return TRASH;
            }
            if (rq.getParameter("del") != null
                    || rq.getParameter("del.x") != null) { return delete(); }
            if (rq.getParameter("undel") != null
                    || rq.getParameter("undel.x") != null) { return undelete(rq); }
            return TRASH;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private String query(boolean newQuery) throws Exception {

        ContentManager cm = lookupContentManager();

        try {
            TrashFolderForm folderForm = (TrashFolderForm) getForm();
            folderForm.setCallingAETs(getAEFilterPermissions());
            if (newQuery) {
                folderForm.setTotal(new QueryPrivateStudiesCmd(folderForm.filterDS(), DELETED, 
                        !folderForm.isShowWithoutStudies()).count());
            }
            List studyList = new QueryPrivateStudiesCmd(folderForm.filterDS(), DELETED, !folderForm.isShowWithoutStudies()).list(folderForm.getOffset(), folderForm.getLimit());
            List patList = new ArrayList();
            PatientModel curPat = null;
            for (int i = 0, n = studyList.size(); i < n; i++) {
                Dataset ds = (Dataset) studyList.get(i);
                PatientModel pat = new PatientModel(ds);
                if (!pat.equals(curPat)) {
                    patList.add(curPat = pat);
                }
                StudyModel study = new StudyModel(ds);
                if (study.getPk() != -1 && !curPat.getStudies().contains(study)) {
                    curPat.getStudies().add(study);
                }
            }

            folderForm.updatePatients(patList);
        } finally {
            try {
                cm.remove();
            } catch (Exception e) {
            }
        }
        return TRASH;
    }

    private void emptyTrash() {
        delegate.emptyTrash();
    }

    private String delete() throws Exception {
        TrashFolderForm folderForm = (TrashFolderForm) getForm();
        deletePatients(folderForm.getPatients());
        folderForm.removeStickies();
        query(true);
        return TRASH;
    }

    private void deletePatients(List patients)
    throws Exception {
        TrashFolderForm folderForm = (TrashFolderForm) getForm();
        for (int i = 0, n = patients.size(); i < n; i++) {
            PatientModel pat = (PatientModel) patients.get(i);
            if (folderForm.isSticky(pat)) {
                List studies = listStudiesOfPatient(pat.getPk());
                delegate.deletePatient(pat.getPk());
            } else {
                deleteStudies( pat );
            }
        }
    }

    private void deleteStudies( PatientModel pat )
    throws Exception {
        List studies = pat.getStudies();
        TrashFolderForm folderForm = (TrashFolderForm) getForm();
        for (int i = 0, n = studies.size(); i < n; i++) {
            StudyModel study = (StudyModel) studies.get(i);
            if (folderForm.isSticky(study)) {
                delegate.deleteStudy(study.getPk());
            } else {
                final int deletedInstances = deleteSeries( study.getSeries() );
            }
        }
    }

    private int deleteSeries(List series)
    throws Exception {
        int numInsts = 0;
        TrashFolderForm folderForm = (TrashFolderForm) getForm();
        for (int i = 0, n = series.size(); i < n; i++) {
            SeriesModel serie = (SeriesModel) series.get(i);
            if (folderForm.isSticky(serie)) {
                delegate.deleteSeries(serie.getPk());
                numInsts += serie.getNumberOfInstances();
            } else {
                numInsts += deleteInstances(serie.getInstances());
            }
        }
        return numInsts;
    }

    private int deleteInstances(List instances) throws Exception {
        int numInsts = 0;
        TrashFolderForm folderForm = (TrashFolderForm) getForm();
        for (int i = 0, n = instances.size(); i < n; i++) {
            InstanceModel instance = (InstanceModel) instances.get(i);
            if (folderForm.isSticky(instance)) {
                delegate.deleteInstance(instance.getPk());
            }
        }
        return numInsts;
    }

    private String undelete(HttpServletRequest rq) throws Exception {
        String cmd = rq.getParameter("undel");
        TrashFolderForm folderForm = (TrashFolderForm) getForm();
        if ( "patient".equals(cmd)) {
            delegate.undeletePatient(Integer.parseInt( rq.getParameter("patPk") ) );
        } else if ( "study".equals(cmd) ) {
            delegate.undeleteStudy(Integer.parseInt( rq.getParameter("studyPk") ) );
        } else if ( "series".equals(cmd) ) {
            delegate.undeleteSeries(Integer.parseInt( rq.getParameter("seriesPk") ) );
        } else if ( "instance".equals(cmd) ) {
            delegate.undeleteInstance(Integer.parseInt( rq.getParameter("instancePk") ) );
        } else {
            undeletePatients(folderForm.getPatients());
        }
        folderForm.removeStickies();
        query(true);
        return TRASH;
    }

    private void undeletePatients(List patients)
    throws Exception {
        TrashFolderForm folderForm = (TrashFolderForm) getForm();
        for (int i = 0, n = patients.size(); i < n; i++) {
            PatientModel pat = (PatientModel) patients.get(i);
            if (folderForm.isSticky(pat)) {
                delegate.undeletePatient(pat.getPk());
            } else
                undeleteStudies( pat );
        }
    }

    private void undeleteStudies( PatientModel pat )
    throws Exception {
        List studies = pat.getStudies();
        TrashFolderForm folderForm = (TrashFolderForm) getForm();
        for (int i = 0, n = studies.size(); i < n; i++) {
            StudyModel study = (StudyModel) studies.get(i);
            if (folderForm.isSticky(study)) {
                delegate.undeleteStudy(study.getPk());
            } else {
                undeleteSeries( study.getSeries() );
            }
        }
    }

    private int undeleteSeries(List series)
    throws Exception {
        int numInsts = 0;
        TrashFolderForm folderForm = (TrashFolderForm) getForm();
        for (int i = 0, n = series.size(); i < n; i++) {
            SeriesModel serie = (SeriesModel) series.get(i);
            if (folderForm.isSticky(serie)) {
                delegate.undeleteSeries(serie.getPk());
            } else {
                numInsts += undeleteInstances(serie.getInstances());
            }
        }
        return numInsts;
    }

    private int undeleteInstances(List instances) throws Exception {
        int numInsts = 0;
        TrashFolderForm folderForm = (TrashFolderForm) getForm();
        for (int i = 0, n = instances.size(); i < n; i++) {
            InstanceModel instance = (InstanceModel) instances.get(i);
            if (folderForm.isSticky(instance)) {
                delegate.undeleteInstance(instance.getPk());
            }
        }
        return numInsts;
    }

    private void setSticky(Set stickySet, String attr) {
        stickySet.clear();
        String[] newValue = getCtx().getRequest().getParameterValues(attr);
        if (newValue != null) {
            stickySet.addAll(Arrays.asList(newValue));
        }
    }

    private ContentManager lookupContentManager() throws Exception {
        ContentManagerHome home = (ContentManagerHome) EJBHomeFactory
        .getFactory().lookup(ContentManagerHome.class,
                ContentManagerHome.JNDI_NAME);
        return home.create();
    }

    private List listStudiesOfPatient(long patPk) throws Exception {
        ContentManagerHome home = (ContentManagerHome) EJBHomeFactory
        .getFactory().lookup(ContentManagerHome.class,
                ContentManagerHome.JNDI_NAME);
        ContentManager cm = home.create();
        try {
            return cm.listStudiesOfPrivatePatient(patPk);
        } finally {
            try {
                cm.remove();
            } catch (Exception e) {
            }
        }
    }





    /**
     * 
     */
    public void clearSticky() {
        TrashFolderForm folderForm = (TrashFolderForm) getForm();
        folderForm.getStickyPatients().clear();		
        folderForm.getStickyStudies().clear();		
        folderForm.getStickySeries().clear();		
        folderForm.getStickyInstances().clear();		
    }


    protected void logProcedureRecord( PatientModel pat, StudyModel study, String desc ) {
        AuditLoggerDelegate.logProcedureRecord(getCtx(),
                AuditLoggerDelegate.MODIFY,
                pat.getPatientID(),
                pat.getPatientName(),
                study.getPlacerOrderNumber(),
                study.getFillerOrderNumber(),
                study.getStudyIUID(),
                study.getAccessionNumber(),
                desc );
    }

    private String logout() {
        getCtx().getRequest().getSession().invalidate();
        return LOGOUT;
    }

    protected String getCtrlName() {
        return "trash";
    }

}
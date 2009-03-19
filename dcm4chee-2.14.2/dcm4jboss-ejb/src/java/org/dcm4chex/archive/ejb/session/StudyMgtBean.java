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

package org.dcm4chex.archive.ejb.session;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.FinderException;
import javax.ejb.ObjectNotFoundException;
import javax.ejb.RemoveException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.log4j.Logger;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.dict.Status;
import org.dcm4che.dict.Tags;
import org.dcm4che.net.DcmServiceException;
import org.dcm4chex.archive.ejb.conf.AttributeFilter;
import org.dcm4chex.archive.ejb.interfaces.InstanceLocal;
import org.dcm4chex.archive.ejb.interfaces.InstanceLocalHome;
import org.dcm4chex.archive.ejb.interfaces.PatientLocal;
import org.dcm4chex.archive.ejb.interfaces.PatientLocalHome;
import org.dcm4chex.archive.ejb.interfaces.PatientUpdateLocal;
import org.dcm4chex.archive.ejb.interfaces.PatientUpdateLocalHome;
import org.dcm4chex.archive.ejb.interfaces.SeriesLocal;
import org.dcm4chex.archive.ejb.interfaces.SeriesLocalHome;
import org.dcm4chex.archive.ejb.interfaces.StudyLocal;
import org.dcm4chex.archive.ejb.interfaces.StudyLocalHome;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 8056 $ $Date: 2008-04-21 13:49:36 +0200 (Mon, 21 Apr
 *          2008) $
 * @since Jun 6, 2005
 * 
 * @ejb.bean name="StudyMgt" type="Stateless" view-type="remote"
 *           jndi-name="ejb/StudyMgt"
 * 
 * @ejb.transaction-type type="Container"
 * @ejb.transaction type="Required"
 * 
 * @ejb.ejb-ref ejb-name="Patient" view-type="local" ref-name="ejb/Patient"
 * @ejb.ejb-ref ejb-name="Study" view-type="local" ref-name="ejb/Study"
 * @ejb.ejb-ref ejb-name="Series" view-type="local" ref-name="ejb/Series"
 * @ejb.ejb-ref ejb-name="Instance" view-type="local" ref-name="ejb/Instance"
 * @ejb.ejb-ref ejb-name="PatientUpdate" view-type="local"
 *              ref-name="ejb/PatientUpdate"
 */
public abstract class StudyMgtBean implements SessionBean {

    private static final Logger log = Logger.getLogger(StudyMgtBean.class);

    private PatientLocalHome patHome;

    private StudyLocalHome studyHome;

    private SeriesLocalHome seriesHome;

    private InstanceLocalHome instHome;

    private PatientUpdateLocalHome patientUpdateHome;

    public void setSessionContext(SessionContext arg0) throws EJBException,
            RemoteException {
        Context jndiCtx = null;
        try {
            jndiCtx = new InitialContext();
            patHome = (PatientLocalHome) jndiCtx
                    .lookup("java:comp/env/ejb/Patient");
            studyHome = (StudyLocalHome) jndiCtx
                    .lookup("java:comp/env/ejb/Study");
            seriesHome = (SeriesLocalHome) jndiCtx
                    .lookup("java:comp/env/ejb/Series");
            instHome = (InstanceLocalHome) jndiCtx
                    .lookup("java:comp/env/ejb/Instance");
            patientUpdateHome = (PatientUpdateLocalHome) jndiCtx
                    .lookup("java:comp/env/ejb/PatientUpdate");

        } catch (NamingException e) {
            throw new EJBException(e);
        } finally {
            if (jndiCtx != null) {
                try {
                    jndiCtx.close();
                } catch (NamingException ignore) {
                }
            }
        }
    }

    public void unsetSessionContext() {
        patHome = null;
        studyHome = null;
        seriesHome = null;
        instHome = null;
    }

    /**
     * @ejb.interface-method
     */
    public void createStudy(Dataset ds) throws DcmServiceException,
            CreateException, FinderException {
        checkDuplicateStudy(ds.getString(Tags.StudyInstanceUID));
        studyHome.create(ds, findOrCreatePatient(ds));
    }

    private PatientLocal findOrCreatePatient(Dataset ds)
            throws FinderException, CreateException {
        Collection c = patHome.selectByPatientDemographic(ds);
        if (c.size() != 1) {
            return patHome.create(ds);
        }
        return patHome.followMergedWith((PatientLocal) c.iterator().next());
    }

    private void checkDuplicateStudy(String suid) throws FinderException,
            DcmServiceException {
        try {
            studyHome.findByStudyIuid(suid);
            throw new DcmServiceException(Status.DuplicateSOPInstance, suid);
        } catch (ObjectNotFoundException e) {
        }
    }

    private StudyLocal getStudy(String suid) throws FinderException,
            DcmServiceException {
        try {
            return studyHome.findByStudyIuid(suid);
        } catch (ObjectNotFoundException e) {
            throw new DcmServiceException(Status.NoSuchObjectInstance, suid);
        }
    }

    /**
     * This method is invoked when post-storage message is processed. All
     * patient and study attributes will be replaced with the new data, which is
     * different from data coercion during the storage, where only empty
     * attibutes are updated.
     * 
     * @ejb.interface-method
     */
    public void updateStudyAndPatientOnly(String iuid, Dataset ds)
            throws DcmServiceException {
        try {
            StudyLocal study = getStudy(iuid);
            AttributeFilter patientFilter = AttributeFilter
                    .getPatientAttributeFilter();
            AttributeFilter studyFilter = AttributeFilter
                    .getStudyAttributeFilter();
            Dataset patientAttr = patientFilter.filter(ds);
            Dataset studyAttr = studyFilter.filter(ds);

            PatientUpdateLocal patientUpdate = patientUpdateHome.create();
            try {
                patientUpdate.updatePatient(study, patientAttr);
            } finally {
                patientUpdate.remove();
            }
            updateStudy(iuid, studyAttr);
        } catch (Exception e) {
            throw new EJBException(e);
        }
    }

    /**
     * @ejb.interface-method
     */
    public void updateStudy(String iuid, Dataset ds) throws DcmServiceException {
        try {
            StudyLocal study = getStudy(iuid);
            if (study == null) {
                // Study may be deleted already
                log
                        .warn("Unable to update the study that does not exist. StudyIuid: "
                                + iuid);
                return;
            }

            Dataset attrs = study.getAttributes(false);
            attrs.putAll(ds);
            study.setAttributes(attrs);
            DcmElement seriesSq = ds.get(Tags.RefSeriesSeq);
            if (seriesSq != null) {
                Set dirtyStudies = new HashSet();
                Set dirtySeries = new HashSet();
                for (int i = 0, n = seriesSq.countItems(); i < n; ++i) {
                    updateSeries(seriesSq.getItem(i), study, dirtyStudies,
                            dirtySeries);
                }
                updateDerivedSeriesFields(dirtySeries);
                updateDerivedStudyFields(dirtyStudies);
            }
        } catch (FinderException e) {
            throw new EJBException(e);
        } catch (CreateException e) {
            throw new EJBException(e);
        }
    }

    private void updateDerivedStudyFields(Set dirtyStudies)
            throws FinderException {
        for (Iterator it = dirtyStudies.iterator(); it.hasNext();) {
            String iuid = (String) it.next();
            StudyLocal study = studyHome.findByStudyIuid(iuid);
            UpdateDerivedFieldsUtils.updateDerivedFieldsOf(study);
        }
    }

    private void updateDerivedSeriesFields(Set dirtySeries)
            throws FinderException {
        for (Iterator it = dirtySeries.iterator(); it.hasNext();) {
            String iuid = (String) it.next();
            SeriesLocal series = seriesHome.findBySeriesIuid(iuid);
            UpdateDerivedFieldsUtils.updateDerivedFieldsOf(series);
        }
    }

    private void updateSeries(Dataset ds, StudyLocal study, Set dirtyStudies,
            Set dirtySeries) throws FinderException, CreateException {
        try {
            SeriesLocal series = seriesHome.findBySeriesIuid(ds
                    .getString(Tags.SeriesInstanceUID));
            StudyLocal prevStudy = series.getStudy();
            if (!study.isIdentical(prevStudy)) {
                log.info("Move " + series.asString() + " from "
                        + prevStudy.asString() + " to " + study.asString());
                series.setStudy(study);
                dirtyStudies.add(study.getStudyIuid());
                dirtyStudies.add(prevStudy.getStudyIuid());
            }
            Dataset attrs = series.getAttributes(false);
            String newModality = ds.getString(Tags.Modality);
            if (newModality != null
                    && !newModality.equals(attrs.getString(Tags.Modality))) {
                dirtyStudies.add(study.getStudyIuid());
            }
            attrs.putAll(ds);
            series.setAttributes(attrs);
            DcmElement sopSq = ds.get(Tags.RefSOPSeq);
            if (sopSq != null) {
                for (int i = 0, n = sopSq.countItems(); i < n; ++i) {
                    updateInstance(sopSq.getItem(i), series, dirtyStudies,
                            dirtySeries);
                }
            }
        } catch (ObjectNotFoundException e) {
            seriesHome.create(ds, study);
            dirtyStudies.add(study.getStudyIuid());
        }
    }

    private void updateInstance(Dataset ds, SeriesLocal series,
            Set dirtyStudies, Set dirtySeries) throws FinderException,
            CreateException {
        try {
            InstanceLocal inst = instHome.findBySopIuid(ds
                    .getString(Tags.RefSOPInstanceUID));
            SeriesLocal prevSeries = inst.getSeries();
            if (!series.isIdentical(prevSeries)) {
                log.info("Move " + inst.asString() + " from "
                        + prevSeries.asString() + " to " + series.asString());
                inst.setSeries(series);
                dirtySeries.add(series.getSeriesIuid());
                dirtyStudies.add(series.getStudy().getStudyIuid());
                dirtySeries.add(prevSeries.getSeriesIuid());
                dirtyStudies.add(prevSeries.getStudy().getStudyIuid());
            }
            Dataset attrs = inst.getAttributes(false);
            attrs.putAll(ds);
            inst.setAttributes(attrs);
        } catch (ObjectNotFoundException e) {
            instHome.create(ds, series);
            dirtySeries.add(series.getSeriesIuid());
            dirtyStudies.add(series.getStudy().getStudyIuid());
        }
    }

    /**
     * @ejb.interface-method
     */
    public void deleteStudy(String iuid) throws DcmServiceException {
        try {
            getStudy(iuid).remove();
        } catch (FinderException e) {
            throw new EJBException(e);
        } catch (RemoveException e) {
            throw new EJBException(e);
        }
    }

    /**
     * @ejb.interface-method
     */
    public void deleteSeries(String[] iuids) {
        try {
            Set dirtyStudies = new HashSet();
            for (int i = 0; i < iuids.length; i++) {
                SeriesLocal series = seriesHome.findBySeriesIuid(iuids[i]);
                dirtyStudies.add(series.getStudy().getStudyIuid());
                series.remove();
            }
            updateDerivedStudyFields(dirtyStudies);
        } catch (ObjectNotFoundException ignore) {
        } catch (FinderException e) {
            throw new EJBException(e);
        } catch (RemoveException e) {
            throw new EJBException(e);
        }
    }

    /**
     * @ejb.interface-method
     */
    public void deleteInstances(String[] iuids) throws DcmServiceException {
        try {
            Set dirtySeries = new HashSet();
            Set dirtyStudies = new HashSet();
            for (int i = 0; i < iuids.length; i++) {
                InstanceLocal inst = instHome.findBySopIuid(iuids[i]);
                SeriesLocal series = inst.getSeries();
                dirtySeries.add(series.getSeriesIuid());
                dirtyStudies.add(series.getStudy().getStudyIuid());
                inst.remove();
            }
            updateDerivedSeriesFields(dirtySeries);
            updateDerivedStudyFields(dirtyStudies);
        } catch (ObjectNotFoundException ignore) {
        } catch (FinderException e) {
            throw new EJBException(e);
        } catch (RemoveException e) {
            throw new EJBException(e);
        }
    }

    /**
     * @ejb.interface-method
     */
    public void updateStudyStatusId(String iuid, String statusId)
            throws FinderException, DcmServiceException {
        StudyLocal study = getStudy(iuid);
        Dataset attrs = study.getAttributes(false);
        if (!statusId.equals(attrs.getString(Tags.StudyStatusID))) {
            attrs.putCS(Tags.StudyStatusID, statusId);
            study.setAttributes(attrs);
        }
    }

    /**
     * @ejb.interface-method
     */
    public String getStudyStatusId(String iuid, String statusId)
            throws FinderException, DcmServiceException {
        return getStudy(iuid).getStudyStatusId();
    }
}

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
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Iterator;

import javax.ejb.EJBException;
import javax.ejb.FinderException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.log4j.Logger;
import org.dcm4chex.archive.ejb.interfaces.InstanceLocal;
import org.dcm4chex.archive.ejb.interfaces.SeriesLocal;
import org.dcm4chex.archive.ejb.interfaces.StudyLocal;
import org.dcm4chex.archive.ejb.interfaces.StudyLocalHome;

/**
 * 
 * @author franz.willer@gwi-ag.com
 * @version $Revision: 8056 $ $Date: 2008-11-12 14:31:15 +0100 (Wed, 12 Nov 2008) $
 * @since 25.03.2005
 * 
 * @ejb.bean name="ConsistencyCheck" type="Stateless" view-type="remote"
 *           jndi-name="ejb/ConsistencyCheck"
 * 
 * @ejb.transaction-type type="Container"
 * 
 * @ejb.transaction type="Required"
 * 
 * @ejb.ejb-ref ejb-name="Patient" view-type="local" ref-name="ejb/Patient"
 * 
 * @ejb.ejb-ref ejb-name="Study" view-type="local" ref-name="ejb/Study"
 * 
 * @ejb.ejb-ref ejb-name="Series" view-type="local" ref-name="ejb/Series"
 * 
 * @ejb.ejb-ref ejb-name="Instance" view-type="local" ref-name="ejb/Instance"
 */
public abstract class ConsistencyCheckBean implements SessionBean {

    private StudyLocalHome studyHome;

    private static final Logger log = Logger
            .getLogger(ConsistencyCheckBean.class);

    public void setSessionContext(SessionContext arg0) throws EJBException,
            RemoteException {
        Context jndiCtx = null;
        try {
            jndiCtx = new InitialContext();
            studyHome = (StudyLocalHome) jndiCtx
                    .lookup("java:comp/env/ejb/Study");
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
        studyHome = null;
    }

    /**
     * Return studies to check consistency..
     * <p>
     * <DL>
     * <DD>1) Find (0-<code>limit</code>) studies with a creation date between
     * <code>createdAfter and createdBefore</code> and not checked before
     * checkedAfter</DD>
     * </DL>
     * 
     * @param createdAfter
     *            Timestamp: studies must be created after this timestamp.
     * @param createdBefore
     *            Timestamp: studies must be created before this timestamp.
     * @param checkedBefore
     *            Timestamp: studies must be checked before this timestamp.
     * @param limit
     *            Max number of returned studies.
     * 
     * @return int array with pk of studies to check.
     * @ejb.interface-method
     */
    public long[] findStudiesToCheck(Timestamp createdAfter,
            Timestamp createdBefore, Timestamp checkedBefore, int limit)
            throws FinderException {
        if (log.isDebugEnabled())
            log.debug("findStudiesToCheck: created between " + createdAfter
                    + " - " + createdBefore + " checkedBefore" + checkedBefore
                    + " limit:" + limit);
        Collection c = studyHome.findStudyToCheck(createdAfter, createdBefore,
                checkedBefore, limit);
        if (c.size() < 1)
            return new long[0];
        Iterator iter = c.iterator();
        long[] ia = new long[c.size()];
        int i = 0;
        while (iter.hasNext()) {
            ia[i++] = ((StudyLocal) iter.next()).getPk().longValue();
        }
        return ia;
    }

    /**
     * @ejb.interface-method
     */
    public boolean updateStudy(long study_pk,
            int availabilityOfExternalRetrieveable) {
        boolean updated = false;
        try {
            StudyLocal study = studyHome.findByPrimaryKey(new Long(study_pk));
            Collection col = study.getSeries();
            Iterator iter = col.iterator();
            SeriesLocal series;
            Collection instances;
            InstanceLocal instance;
            while (iter.hasNext()) {
                series = (SeriesLocal) iter.next();
                instances = series.getInstances();
                Iterator iter1 = instances.iterator();
                while (iter1.hasNext()) {
                    instance = (InstanceLocal) iter1.next();
                    if (instance.updateRetrieveAETs()) {
                        log.info("Retrieve AETs in Instance "
                                + instance.getSopIuid() + " updated!");
                        updated = true;
                    }
                    if (instance.updateAvailability(
                            availabilityOfExternalRetrieveable)) {
                        log.info("Availability in Instance "
                                + instance.getSopIuid() + " updated!");
                        updated = true;
                    }
                }
                if (series.updateNumberOfSeriesRelatedInstances()) {
                    log.info("Number of Series Related Instances in Series "
                            + series.getSeriesIuid() + " updated!");
                    updated = true;
                }
                if (series.updateRetrieveAETs()) {
                    log.info("Retrieve AETs in Series "
                            + series.getSeriesIuid() + " updated!");
                    updated = true;
                }
                if (series.updateExternalRetrieveAET()) {
                    log.info("External Retrieve AET in Series "
                            + series.getSeriesIuid() + " updated!");
                    updated = true;
                }
                if (series.updateFilesetId()) {
                    log.info("Fileset ID in Series "
                            + series.getSeriesIuid() + " updated!");
                    updated = true;
                }
                if (series.updateAvailability()) {
                    log.info("Availability in Series "
                            + series.getSeriesIuid() + " updated!");
                    updated = true;
                }
            }
            if (study.updateNumberOfStudyRelatedSeries()) {
                log.info("Number of Study Related Series in Study "
                        + study.getStudyIuid() + " updated!");
                updated = true;
            }
            if (study.updateNumberOfStudyRelatedInstances()) {
                log.info("Number of Study Related Instances in Study "
                        + study.getStudyIuid() + " updated!");
                updated = true;
            }
            if (study.updateRetrieveAETs()) {
                log.info("Retrieve AETs in Study "
                        + study.getStudyIuid() + " updated!");
                updated = true;
            }
            if (study.updateExternalRetrieveAET()) {
                log.info("External Retrieve AET in Study "
                        + study.getStudyIuid() + " updated!");
                updated = true;
            }
            if (study.updateFilesetId()) {
                log.info("Fileset ID Study "
                        + study.getStudyIuid() + " updated!");
                updated = true;
            }
            if (study.updateAvailability()) {
                log.info("Availability in Study "
                        + study.getStudyIuid() + " updated!");
                updated = true;
            }
            if (study.updateModalitiesInStudy()) {
                log.info("Modalities In Study in Study "
                        + study.getStudyIuid() + " updated!");
                updated = true;
            }
            if (study.updateSOPClassesInStudy()) {
                log.info("SOP Classes in Study " + study.getStudyIuid() + " updated!");
                updated = true;
            }
            /*
                        if (retrieveAETs)
                                if (updateRetrieveAETs(pk, numI)) updated = true;
                        if (externalRettrieveAETs)
                                if (updateExternalRetrieveAET(pk, numI)) updated = true;
                        if (filesetId)
                                if (updateFilesetId(pk, numI)) updated = true;
                        if (availibility)
                                if (updateAvailability(pk, numI)) updated = true;
                        if (modsInStudies)
                                if (updateModalitiesInStudy(pk, numI)) updated = true;
                        return updated;
            }
        */
            study.setTimeOfLastConsistencyCheck(new Timestamp(System
                    .currentTimeMillis()));
            return updated;
        } catch (FinderException e) {
            throw new EJBException(e);
        }
    }

}
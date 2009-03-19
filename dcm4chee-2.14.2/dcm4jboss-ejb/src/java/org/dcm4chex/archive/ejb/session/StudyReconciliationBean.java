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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.FinderException;
import javax.ejb.ObjectNotFoundException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.log4j.Logger;
import org.dcm4che.data.Dataset;
import org.dcm4che.dict.Status;
import org.dcm4che.net.DcmServiceException;
import org.dcm4chex.archive.ejb.interfaces.PatientUpdateLocal;
import org.dcm4chex.archive.ejb.interfaces.PatientUpdateLocalHome;
import org.dcm4chex.archive.ejb.interfaces.SeriesLocal;
import org.dcm4chex.archive.ejb.interfaces.StudyLocal;
import org.dcm4chex.archive.ejb.interfaces.StudyLocalHome;
import org.dcm4chex.archive.exceptions.NonUniquePatientException;
import org.dcm4chex.archive.exceptions.PatientMergedException;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 8056 $ $Date: 2008-11-12 14:31:15 +0100 (Wed, 12 Nov 2008) $
 * @since Jun 6, 2005
 * 
 * @ejb.bean name="StudyReconciliation" type="Stateless" view-type="remote"
 *           jndi-name="ejb/StudyReconciliation"
 *           
 * @ejb.transaction-type type="Container"
 * @ejb.transaction type="Required"
 * 
 * @ejb.ejb-ref ejb-name="Patient" view-type="local" ref-name="ejb/Patient"
 * @ejb.ejb-ref ejb-name="Study" view-type="local" ref-name="ejb/Study"
 * @ejb.ejb-ref ejb-name="Series" view-type="local" ref-name="ejb/Series"
 * @ejb.ejb-ref ejb-name="Instance" view-type="local" ref-name="ejb/Instance"
 * @ejb.ejb-ref ejb-name="PatientUpdate" view-type="local" ref-name="ejb/PatientUpdate" 
 * 
 * @ejb.env-entry name="AttributeFilterConfigURL" type="java.lang.String"
 *                value="resource:dcm4chee-attribute-filter.xml"
 */
public abstract class StudyReconciliationBean implements SessionBean {

    private static final Logger log = Logger.getLogger(StudyReconciliationBean.class);
	
	private StudyLocalHome studyHome;

	private PatientUpdateLocal patientUpdate;

	public void setSessionContext(SessionContext arg0) throws EJBException,
			RemoteException {
		Context jndiCtx = null;
		try {
			jndiCtx = new InitialContext();
			studyHome = (StudyLocalHome) jndiCtx
					.lookup("java:comp/env/ejb/Study");
            patientUpdate = ((PatientUpdateLocalHome) jndiCtx
            		.lookup("java:comp/env/ejb/PatientUpdate")).create();

		} catch (NamingException e) {
			throw new EJBException(e);
		} catch (CreateException e) {
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
	
	private StudyLocal getStudy(String suid) 
			throws FinderException, DcmServiceException {
		try {
			return studyHome.findByStudyIuid(suid);
		} catch (ObjectNotFoundException e) {
			throw new DcmServiceException(Status.NoSuchSOPClass, suid);
		}
	}
	
	/**
     * @throws FinderException
	 * @ejb.interface-method
     */
    public Collection getStudyIuidsWithStatus(int status,Timestamp createdBefore, int limit) throws FinderException {
    	Collection col = studyHome.findStudiesWithStatus( status, createdBefore, limit );
    	ArrayList studyIuids = new ArrayList();
    	for ( Iterator iter = col.iterator() ; iter.hasNext() ;) {
    		studyIuids.add( ((StudyLocal) iter.next()).getStudyIuid());
    	}
    	return studyIuids;
    }

	/**
     * @throws DcmServiceException
	 * @throws FinderException
	 * @ejb.interface-method
     */
    public void updateStatus(Collection studyIuids, int status) throws FinderException, DcmServiceException {
    	if ( studyIuids == null ) return;
    	for ( Iterator iter = studyIuids.iterator() ; iter.hasNext() ;) {
    		getStudy((String) iter.next()).setStudyStatus(status);	
    	}
    }
    
	/**
     * @throws DcmServiceException
	 * @throws FinderException
	 * @ejb.interface-method
     */
    public void updateStatus(String studyIuid, int status) throws FinderException, DcmServiceException {
    	if ( studyIuid == null ) return;
   		getStudy(studyIuid).setStudyStatus(status);	
    }
    
    /**
     * @ejb.interface-method
     */
    public void updatePatient(Dataset attrs)
            throws FinderException, CreateException {
    	patientUpdate.updatePatient(attrs);
    }
    
    /**
     * @ejb.interface-method
     */
    public void mergePatient(Dataset dominant, Dataset prior)
            throws FinderException, CreateException {
    	patientUpdate.mergePatient(dominant, prior);
    }

	/**
     * @throws DcmServiceException
	 * @throws FinderException
	 * @ejb.interface-method
     */
    public void updateStudyAndSeries(String studyIuid, int studyStatus, Map map) throws FinderException, DcmServiceException {
    	if ( studyIuid == null ) return;
   		StudyLocal study = getStudy(studyIuid);
   		study.setStudyStatus(studyStatus);
   		if ( map != null && !map.isEmpty()) {
   			Iterator iter = study.getSeries().iterator();
   			Dataset ds, dsOrig;
   			SeriesLocal sl;
   			do {
   				sl = (SeriesLocal)iter.next();
   				ds = sl.getAttributes(false);
   				dsOrig = (Dataset)map.get(sl.getSeriesIuid());
   				ds.putAll(dsOrig);
   				sl.setAttributes(ds);
   			} while ( iter.hasNext() );
   			ds = study.getAttributes(false);
   			ds.putAll(dsOrig); 
   			study.setAttributes(ds); 
   			study.updateModalitiesInStudy();
   			study.updateSOPClassesInStudy();
   		}
    }
    
}

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.FinderException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.log4j.Logger;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.dict.Tags;
import org.dcm4chex.archive.common.PrivateTags;
import org.dcm4chex.archive.ejb.interfaces.InstanceLocal;
import org.dcm4chex.archive.ejb.interfaces.InstanceLocalHome;
import org.dcm4chex.archive.ejb.interfaces.PatientLocal;
import org.dcm4chex.archive.ejb.interfaces.PatientLocalHome;
import org.dcm4chex.archive.ejb.interfaces.SeriesLocal;
import org.dcm4chex.archive.ejb.interfaces.SeriesLocalHome;
import org.dcm4chex.archive.ejb.interfaces.StudyLocal;
import org.dcm4chex.archive.ejb.interfaces.StudyLocalHome;
import org.dcm4chex.archive.util.Convert;

/**
 * 
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 8056 $ $Date: 2008-11-12 14:31:15 +0100 (Wed, 12 Nov 2008) $
 * @since 14.01.2004
 * 
 * @ejb.bean
 *  name="ContentEdit"
 *  type="Stateless"
 *  view-type="both"
 *  jndi-name="ejb/ContentEdit"
 * 
 * @ejb.transaction-type 
 *  type="Container"
 * 
 * @ejb.transaction 
 *  type="Required"
 * 
 * @ejb.ejb-ref ejb-name="Patient" view-type="local" ref-name="ejb/Patient" 
 * 
 * @ejb.ejb-ref ejb-name="Study" view-type="local" ref-name="ejb/Study" 
 * 
 * @ejb.ejb-ref ejb-name="Series" view-type="local" ref-name="ejb/Series" 
 * 
 * @ejb.ejb-ref ejb-name="Instance" view-type="local" ref-name="ejb/Instance" 
 *  
 * @ejb.ejb-ref ejb-name="File" view-type="local" ref-name="ejb/File"
 *  
 */
public abstract class ContentEditBean implements SessionBean {

	private static final int CHANGE_MODE_NO = 0;
	private static final int CHANGE_MODE_STUDY = 0x04;
	private static final int CHANGE_MODE_SERIES = 0x02;
	private static final int CHANGE_MODE_INSTANCE = 0x01;

	private static final int DELETED = 1;
	
    private PatientLocalHome patHome;
    private StudyLocalHome studyHome;
    private SeriesLocalHome seriesHome;
    private InstanceLocalHome instHome;

    private static final DcmObjectFactory dof = DcmObjectFactory.getInstance();
    
    private static Logger log = Logger.getLogger( ContentEditBean.class.getName() );

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
     * @throws CreateException
     * @ejb.interface-method
     */
    public Dataset createPatient(Dataset ds) throws CreateException {
        return patHome.create(ds).getAttributes(true);
    }
    
    /**
     * @ejb.interface-method
     */
    public Map mergePatients(long patPk, long[] mergedPks) {
    	Map map = new HashMap();
    	try {
	        PatientLocal dominant = patHome.findByPrimaryKey(new Long(patPk));
            if ( checkCircularMerge(dominant, mergedPks) ) {
                log.warn("Circular merge detected (dominant:"+dominant.getPatientId()+
                        "^^^"+dominant.getIssuerOfPatientId()+")! Merge order ignored!");
                map.put("ERROR", "Circular Merge detected!");
                return map;
            }
            map.put("DOMINANT",dominant.getAttributes(false) );
            Dataset[] mergedPats = new Dataset[mergedPks.length];
            map.put("MERGED",mergedPats);
	        ArrayList list = new ArrayList();
	        for (int i = 0; i < mergedPks.length; i++) {
	            if ( patPk == mergedPks[i] ) continue;
	            PatientLocal priorPat = patHome.findByPrimaryKey(new Long(mergedPks[i]));
	            mergedPats[i] = priorPat.getAttributes(false);
            	list.addAll(priorPat.getStudies());
	            dominant.getStudies().addAll(priorPat.getStudies());
	            dominant.getMpps().addAll(priorPat.getMpps());
	            dominant.getMwlItems().addAll(priorPat.getMwlItems());                
                dominant.getGsps().addAll(priorPat.getGsps());
	            priorPat.setMergedWith(dominant);
            }
	        ArrayList col = new ArrayList();
            Iterator iter = list.iterator();
            StudyLocal sl;
            while ( iter.hasNext() ) {
            	sl = (StudyLocal) iter.next();
            	col.add( getStudyMgtDataset( sl, sl.getSeries(), null ) );
            }
            map.put("NOTIFICATION_DS", col);
            return map;
        } catch (FinderException e) {
            throw new EJBException(e);
        }        
    }

    private boolean checkCircularMerge(PatientLocal pat, long[] mergedPks) {
        PatientLocal mergedWith = pat.getMergedWith();
        if ( mergedWith != null ) {
            long pk = mergedWith.getPk().longValue();
            for ( int i = 0 ; i < mergedPks.length ; i++) {
                if ( pk == mergedPks[i]) {
                   return true;
                }
            }
            return checkCircularMerge(pat, mergedPks);
        }
        return false;
    }

	/**
     * @throws CreateException
     * @ejb.interface-method
     */
    public Dataset createStudy(Dataset ds, long patPk) throws CreateException {
    	try {
	        PatientLocal patient = patHome.findByPrimaryKey(new Long(patPk));
	        Dataset ds1 = studyHome.create(ds, patient).getAttributes(true);
	        if ( log.isDebugEnabled() ) { 
                log.debug("createStudy ds1:");
                log.debug(ds1);
            }
	        ds1.putAll(patient.getAttributes(true));
	        if ( log.isDebugEnabled() ) { 
                log.debug("createStudy ds1 with patient:");
                log.debug(ds1);
            }
	        return ds1;
        } catch (FinderException e) {
            throw new EJBException(e);
        }
	        
    }
    
    /**
     * @throws CreateException
     * @ejb.interface-method
     */
    public Dataset createSeries(Dataset ds, long studyPk) throws CreateException {
    	try {
	        StudyLocal study = studyHome.findByPrimaryKey(new Long(studyPk));
	        SeriesLocal series =  seriesHome.create(ds, study);
	        Collection col = new ArrayList(); col.add( series );
	        return getStudyMgtDataset( study, col, null, CHANGE_MODE_SERIES, 
                    series.getAttributes(true) );
        } catch (FinderException e) {
            throw new EJBException(e);
        }
	        
    }
    
    /**
     * @ejb.interface-method
     */
    public Collection updatePatient(Dataset ds) {

        try {
        	Collection col = new ArrayList();
            ds.setPrivateCreatorID(PrivateTags.CreatorID);
            final long pk = Convert.toLong(ds.getByteBuffer(PrivateTags.PatientPk).array());
            PatientLocal patient = patHome
                    .findByPrimaryKey(new Long(pk));
            patient.setAttributes(ds);
            Collection studies = patient.getStudies();
            Iterator iter = patient.getStudies().iterator();
            StudyLocal sl;
            while ( iter.hasNext() ) {
            	sl = (StudyLocal) iter.next();
            	col.add( getStudyMgtDataset( sl, sl.getSeries(), null ) );
            }
            return col;
        } catch (FinderException e) {
            throw new EJBException(e);
        }
    }

    /**
     * @ejb.interface-method
     */
    public Dataset updateStudy(Dataset ds) {

        try {
            ds.setPrivateCreatorID(PrivateTags.CreatorID);
            final long pk = Convert.toLong(ds.getByteBuffer(PrivateTags.StudyPk).array());
            StudyLocal study = studyHome
                    .findByPrimaryKey(new Long(pk));
            study.setAttributes(ds);
            return getStudyMgtDataset( study, study.getSeries(), null,
                    CHANGE_MODE_STUDY, study.getAttributes(true) );            
        } catch (FinderException e) {
            throw new EJBException(e);
        }
    }
    
    /**
     * @ejb.interface-method
     */
    public Dataset updateSeries(Dataset ds) {

        try {
            ds.setPrivateCreatorID(PrivateTags.CreatorID);
            final long pk = Convert.toLong(ds.getByteBuffer(PrivateTags.SeriesPk).array());
            SeriesLocal series = seriesHome.findByPrimaryKey(new Long(pk));
	        series.setAttributes(ds);
            StudyLocal study = series.getStudy();
            study.updateModalitiesInStudy();
            study.updateSOPClassesInStudy();
            Collection col = new ArrayList();
            col.add( series );
            return getStudyMgtDataset( study, col, null, CHANGE_MODE_SERIES,
                    series.getAttributes(true) );            
        } catch (FinderException e) {
            throw new EJBException(e);
        }
    }
    
    /**
     * @throws FinderException
     * @ejb.interface-method
     */
    public Collection moveStudies(long[] study_pks, long patient_pk) throws FinderException {
    	Collection col = new ArrayList();
        PatientLocal pat = patHome.findByPrimaryKey(new Long(patient_pk));
        Collection studies = pat.getStudies();
        Dataset dsPat = pat.getAttributes(true);
        Dataset ds1;
        for (int i = 0; i < study_pks.length; i++) {
            StudyLocal study = studyHome.findByPrimaryKey(new Long(
                    study_pks[i]));
            PatientLocal oldPat = study.getPatient();
            if (oldPat.isIdentical(pat)) continue;
            studies.add(study);
            ds1 = getStudyMgtDataset( study, study.getSeries(), null, 
                    CHANGE_MODE_STUDY, dsPat );
            col.add( ds1 );
            
        }
        return col;
    }

    
    /**
     * @throws FinderException
     * @ejb.interface-method
     */
    public Dataset moveSeries(long[] series_pks, long study_pk) throws FinderException {
        StudyLocal study = studyHome.findByPrimaryKey(new Long(
                study_pk));
        Collection seriess = study.getSeries();
        Collection movedSeriess = new ArrayList();
        for (int i = 0; i < series_pks.length; i++) {
            SeriesLocal series = seriesHome.findByPrimaryKey(new Long(
                    series_pks[i]));
            StudyLocal oldStudy = series.getStudy();
            if (oldStudy.isIdentical(study)) continue;
            seriess.add(series);                
            movedSeriess.add( series );
            UpdateDerivedFieldsUtils.updateDerivedFieldsOf(oldStudy);
        }
        UpdateDerivedFieldsUtils.updateDerivedFieldsOf(study);
        return getStudyMgtDataset( study, movedSeriess, null );
    }
    
    /**
     * @ejb.interface-method
     */
    public Dataset moveInstances(long[] instance_pks, long series_pk)
    		throws FinderException {
        SeriesLocal series = seriesHome.findByPrimaryKey(new Long(
                series_pk));
        Collection instances = series.getInstances();
        for (int i = 0; i < instance_pks.length; i++) {
            InstanceLocal instance = instHome.findByPrimaryKey(new Long(
                    instance_pks[i]));
            SeriesLocal oldSeries = instance.getSeries();
            if (oldSeries.isIdentical(series)) continue;
            instances.add(instance);                
            UpdateDerivedFieldsUtils.updateDerivedFieldsOf(oldSeries);
            UpdateDerivedFieldsUtils.updateDerivedFieldsOf(oldSeries.getStudy());
        }
        UpdateDerivedFieldsUtils.updateDerivedFieldsOf(series);
        UpdateDerivedFieldsUtils.updateDerivedFieldsOf(series.getStudy());
        Collection col = new ArrayList(); col.add( series );
        return getStudyMgtDataset( series.getStudy(), col, instances );
    }

    private Dataset getStudyMgtDataset( StudyLocal study, Collection series, Collection instances ) {
    	return getStudyMgtDataset( study, series, instances, 0, null );
    }

    private Dataset getStudyMgtDataset( StudyLocal study, Collection series, Collection instances, int chgMode, Dataset changes ) {
    	Dataset ds = dof.newDataset();
    	ds.putUI( Tags.StudyInstanceUID, study.getStudyIuid() );
    	log.debug("getStudyMgtDataset: studyIUID:"+study.getStudyIuid());
        ds.putSH( Tags.AccessionNumber, study.getAccessionNumber());
        ds.putLO(Tags.PatientID, study.getPatient().getPatientId() );
        ds.putLO(Tags.IssuerOfPatientID, study.getPatient().getIssuerOfPatientId() );
        ds.putPN(Tags.PatientName, study.getPatient().getPatientName() );
    	if ( chgMode == CHANGE_MODE_STUDY) ds.putAll( changes );
		DcmElement refSeriesSeq = ds.putSQ( Tags.RefSeriesSeq );
		Iterator iter = series.iterator();
		while ( iter.hasNext() ) {
			SeriesLocal sl = (SeriesLocal) iter.next();
			Dataset dsSer = refSeriesSeq.addNewItem();
	    	if ( chgMode == CHANGE_MODE_SERIES ) dsSer.putAll( changes );
			dsSer.putUI( Tags.SeriesInstanceUID, sl.getSeriesIuid() );
			Collection colInstances = ( instances != null && series.size() == 1 ) ? instances : sl.getInstances();
			Iterator iter2 = colInstances.iterator();
			DcmElement refSopSeq = null;
			if ( iter2.hasNext() )
				refSopSeq = dsSer.putSQ( Tags.RefSOPSeq );
			while ( iter2.hasNext() ) {
				InstanceLocal il = (InstanceLocal) iter2.next();
				Dataset dsInst = refSopSeq.addNewItem();
		    	if ( chgMode == CHANGE_MODE_INSTANCE ) dsInst.putAll( changes );
				dsInst.putUI( Tags.RefSOPClassUID, il.getSopCuid() );
				dsInst.putUI( Tags.RefSOPInstanceUID, il.getSopIuid() );
				dsInst.putAE( Tags.RetrieveAET, il.getRetrieveAETs() );
			}
		}
		if ( log.isDebugEnabled() ) { log.debug("return StgMgtDataset:");log.debug(ds);}
    	return ds;
    }
}
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

package org.dcm4chex.archive.dcm.mwlscu;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.xml.transform.Templates;

import org.apache.log4j.Logger;
import org.dcm4che.data.Command;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.dict.Tags;
import org.dcm4che.dict.UIDs;
import org.dcm4che.net.ActiveAssociation;
import org.dcm4che.net.Association;
import org.dcm4che.net.AssociationFactory;
import org.dcm4che.net.Dimse;
import org.dcm4che.net.DimseListener;
import org.dcm4chex.archive.config.DicomPriority;
import org.dcm4chex.archive.config.RetryIntervalls;
import org.dcm4chex.archive.dcm.AbstractScuService;
import org.dcm4chex.archive.ejb.interfaces.MWLManager;
import org.dcm4chex.archive.ejb.interfaces.MWLManagerHome;
import org.dcm4chex.archive.mbean.SchedulerDelegate;
import org.dcm4chex.archive.mbean.TemplatesDelegate;
import org.dcm4chex.archive.util.EJBHomeFactory;
import org.dcm4chex.archive.util.XSLTUtils;

/**
 * MBean to configure and service modality worklist managment issues.
 * 
 * @author franz.willer
 * @version $Revision: 5629 $ $Date: 2008-01-02 13:07:12 +0100 (Mi, 02 Jan 2008) $
 * 
 */
public class MwlReplicaService extends AbstractScuService {

    private static final String NONE = "NONE";
    private static final String MWL_REPLICA_RQ_XSL = "mwl-replica-rq.xsl";
    private static final String MWL_REPLICA_RSP_XSL = "mwl-replica-rsp.xsl";

	private static final int PCID = 1;
    private static final int MSG_ID = 1;

	private static Logger log = Logger.getLogger(MwlReplicaService.class.getName());
	private SimpleDateFormat dfDA = new SimpleDateFormat("yyyy/MM/dd");
	private SimpleDateFormat dfDT = new SimpleDateFormat("yyyy/MM/dd hh:mm");
	private SimpleDateFormat dfFull = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss,SSS");
	
    private final SchedulerDelegate scheduler = new SchedulerDelegate(this);
    private long taskInterval = 0L;
    private Integer listenerID;
    private String timerIDMwlReplica;
    private String issuerOfPatient;
    private boolean forceIssuerCoercion;
    private boolean debugMode;
    private long msBefore = -1;
    private long msAfter = -1;
    private List errorHistory = new ArrayList();
    
    /** Holds the AET of modality worklist service. */
    private String[] calledAETs;
	private Object lastErrorMsg;
	private int countEqualErrors=0;
	private int errorCount=0;
	private int totalErrorCount=0;
    
    /** DICOM priority. Used for move and media creation action. */
    private int priority = 0;
    
    protected TemplatesDelegate templates = new TemplatesDelegate(this);
    

    private final NotificationListener mwlReplicationListener = new NotificationListener() {
        public void handleNotification(Notification notif, Object handback) {
        	log.debug("mwlReplicationListener notified!");
                try {
                	process();
                } catch (Exception e) {
                    log.error("MWL Replication process failed!", e);
                    errorHistory.add("process:"+e.getMessage());
                }
        }
    };

    public ObjectName getSchedulerServiceName() {
        return scheduler.getSchedulerServiceName();
    }

    public void setSchedulerServiceName(ObjectName schedulerServiceName) {
        scheduler.setSchedulerServiceName(schedulerServiceName);
    }
    
    public String getTimerIDMwlReplica() {
		return timerIDMwlReplica;
	}

	public void setTimerIDMwlReplica(String timerIDMwlReplica) {
		this.timerIDMwlReplica = timerIDMwlReplica;
	}

	public final String getTaskInterval() {
        return RetryIntervalls.formatIntervalZeroAsNever(taskInterval);
    }

    public void setTaskInterval(String interval) throws Exception {
        long oldInterval = taskInterval;
            taskInterval = RetryIntervalls.parseIntervalOrNever(interval);
        if (getState() == STARTED && oldInterval != taskInterval) {
            scheduler.stopScheduler(timerIDMwlReplica, listenerID,
            		mwlReplicationListener);
            listenerID = scheduler.startScheduler(timerIDMwlReplica, taskInterval,
            		mwlReplicationListener);
        }
    }
    
    public String getTimeBefore() {
		return msBefore == -1 ? NONE :RetryIntervalls.formatInterval(msBefore);
	}

	public void setTimeBefore(String timeBefore) {
		this.msBefore = NONE.equals(timeBefore) ? -1 : RetryIntervalls.parseInterval(timeBefore);
	}

	public String getTimeAfter() {
		return msAfter == -1 ? NONE : RetryIntervalls.formatInterval(msAfter);
	}

	public void setTimeAfter(String timeAfter) {
		this.msAfter = NONE.equals(timeAfter) ? -1 : RetryIntervalls.parseInterval(timeAfter);
	}

	/**
     * Returns the AET(s) that holds the work list (Modality Work List SCP).
     * 
     * @return The retrieve AET.
     */
    public final String getCalledAET() {
    	StringBuilder sb = new StringBuilder(calledAETs[0]);
    	for ( int i = 1 ; i < calledAETs.length ; i++) {
    		sb.append("\\").append(calledAETs[i]);
    	}
        return sb.toString();
    }

    /**
     * Set the retrieve AET.
     * 
     * @param aet
     *            The retrieve AET to set.
     */
    public final void setCalledAET(String aet) {
    	StringTokenizer st = new StringTokenizer(aet, "\\");
        calledAETs = new String[st.countTokens()];
        for ( int i = 0; st.hasMoreTokens() ; i++) {
        	calledAETs[i] = st.nextToken();
        }
    }

    public String getIssuerOfPatient() {
		return issuerOfPatient == null ? NONE : issuerOfPatient;
	}

	public void setIssuerOfPatient(String issuerOfPatient) {
		this.issuerOfPatient = NONE.equalsIgnoreCase(issuerOfPatient) ? null : issuerOfPatient;
	}

	public boolean isForceIssuerCoercion() {
		return forceIssuerCoercion;
	}

	public void setForceIssuerCoercion(boolean forceIssuerCoercion) {
		this.forceIssuerCoercion = forceIssuerCoercion;
	}

	
	public boolean isDebugMode() {
		return debugMode;
	}

	public void setDebugMode(boolean debugMode) {
		this.debugMode = debugMode;
	}

	/**
     * Returns the DICOM priority as int value.
     * <p>
     * This value is used for CFIND. 0..MED, 1..HIGH, 2..LOW
     * 
     * @return Returns the priority.
     */
    public final String getPriority() {
        return DicomPriority.toString(priority);
    }

    /**
     * Set the DICOM priority.
     * 
     * @param priority
     *            The priority to set.
     */
    public final void setPriority(String priority) {
        this.priority = DicomPriority.toCode(priority);
    }
    
    
    public int getErrorCount() {
    	return errorCount;
    }
    public int getTotalErrorCount() {
    	return this.totalErrorCount;
    }
    public String showErrorHistory() {
    	StringBuffer sb = new StringBuffer(errorHistory.size()*20);
    	sb.append(errorCount).append(" (").append(totalErrorCount).append(" since service start) Errors:");
    	for ( Iterator iter= errorHistory.iterator(); iter.hasNext() ; ) {
    		sb.append("\n").append(iter.next());
    	}
    	return sb.toString();
    }
    public void resetErrorHistory() {
    	errorHistory.clear();
    	lastErrorMsg = null;
    	countEqualErrors = 0;
    	errorCount = 0;
    }
    

    protected void startService() throws Exception {
        listenerID = scheduler.startScheduler(timerIDMwlReplica, taskInterval,
        		mwlReplicationListener);
    }

    protected void stopService() throws Exception {
        scheduler.stopScheduler(timerIDMwlReplica, listenerID,
        		mwlReplicationListener);
        super.stopService();
    }

    private void logResponse(Dataset rsp, String aet) {
        if (log.isDebugEnabled()) {
            log.debug("Received matching MWL item from " + aet + " :");
            log.debug(rsp);
        }
    }

    private boolean process() {
    	log.info("MWL Replication process started!");
    	try {
			List l = replicateMWLEntries(calledAETs, null);
			log.debug(l);
			return true;
		} catch (Exception e) {
			log.error("MWL replication process failed!", e);
			return false;
		}
    }
    public List replicateMWLEntries(final String[] aets, Dataset searchDS) {
		List l;
		try {
			l = replicateMWLEntries(aets[0], null);
		} catch (Exception x) {
			log.error("Replicate MWL Entries from "+aets[0]+" failed!",x);
            addError(aets[0],x);
			l = new ArrayList();
		}
		for ( int i = 1 ; i < aets.length ; i++){
			try {
				l.addAll(replicateMWLEntries(aets[i], null));
			} catch (Exception x) {
				log.error("Replicate MWL Entries from "+aets[i]+" failed!",x);
	            addError(aets[i],x);
			}
		}
    	return l;
    }
    public List replicateMWLEntries(final String aet, Dataset searchDS) throws Exception {
    	log.info("Start MWL replication from "+aet);
        if ( searchDS == null ) {
        	searchDS = getSearchDSForDate(new Date(), msBefore, msAfter);
        	log.debug("No searchDS given! Use standard 'today list' searchDS instead:");
        }
        searchDS = coerceAttributes(searchDS, aet, MWL_REPLICA_RQ_XSL);
    	final List result = new ArrayList();
        if ( debugMode ) {
            log.info("Query MWL SCP: " + aet + " with keys:");log.info(searchDS);
        	DcmElement spsSq = searchDS.get(Tags.SPSSeq);
        	log.info("DEBUG Mode! No MWL Query will be performed!\nDate Time Range:"+spsSq == null ? 
        			"No SPSSQ!" : getDateTimeRangeString(spsSq));
        	return result;
        }
        log.debug("Query MWL SCP: " + aet + " with keys:");log.debug(searchDS);
    	final MWLManager manager = mwlMgt();
        try {
            ActiveAssociation aa = openAssociation(aet,
                    UIDs.ModalityWorklistInformationModelFIND);
            try {
                Command cmd = DcmObjectFactory.getInstance().newCommand();
                cmd.initCFindRQ(MSG_ID, UIDs.ModalityWorklistInformationModelFIND,
                        priority);
                Dimse mcRQ = AssociationFactory.getInstance().newDimse(PCID, cmd,
                        searchDS);
                aa.invoke(mcRQ, new DimseListener(){

                    public void dimseReceived(Association assoc, Dimse dimse) {
                        Command rspCmd = dimse.getCommand();
                        if (rspCmd.isPending()) {
                            try {
                                Dataset rsp = dimse.getDataset();
                                logResponse(rsp,aet);
                                if ( issuerOfPatient != null ) {
                                	if ( forceIssuerCoercion || !rsp.containsValue(Tags.IssuerOfPatientID)) {
                                		rsp.putSH(Tags.IssuerOfPatientID, issuerOfPatient);
                                		log.debug("Issuer of patient coerced to "+issuerOfPatient);
                                	}
                                }
                                rsp = coerceAttributes(rsp, aet, MWL_REPLICA_RSP_XSL);
                                if ( !manager.updateWorklistItem(rsp) ) {
                                	manager.addWorklistItem(rsp);
                                }
                                result.add(rsp);
                            } catch (Exception e) {
                            	log.error("Error occured!", e);
                                addError("dimseReceived:",e);
                            }
                        } else {
                            if (log.isDebugEnabled()) {
                                log.debug("Received final C-FIND RSP from " 
                                        + aet + " :" + dimse);
                            }                        
                        }
                        
                    }});
           } finally {
                try {
                    aa.release(true);
                } catch (Exception e) {
                    log.warn("Failed to release " + aa.getAssociation());
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            addError(aet, e);
        }
        log.debug("Result of MWL replica for "+aet+":");log.debug(result);
        return result;
    }

	private void addError(final String aet, Exception e) {
		errorCount++;
		this.totalErrorCount++;
		String msg = aet+":"+e.getMessage();
		if ( msg.equals(lastErrorMsg)) {
			msg = dfFull.format(new Date())+" Repeated "+(++countEqualErrors)+" times";
			if (countEqualErrors > 1 ) {
				int idx = errorHistory.size();idx--;
				errorHistory.set(idx, msg);
			} else {
				errorHistory.add(msg);
			}
		} else {
			lastErrorMsg = msg;
			countEqualErrors = 0;
			errorHistory.add(dfFull.format(new Date())+" "+msg);
		}
	}
	
    private String getDateTimeRangeString(DcmElement spsSq) {
    	Date[] dates = spsSq.getItem().getDateTimeRange(Tags.SPSStartDate, Tags.SPSStartTime);
    	if ( dates == null) return "NONE";
    	StringBuffer sb = new StringBuffer();
    	sb.append(dates[0]).append("-").append(dates[1]);
		return sb.toString();
	}

	public List replicateMWLEntriesForDate(String aet, String date) throws Exception {
    	Date d = isEmpty(date) ? new Date() : dfDA.parse(date);
    	if ( isEmpty(aet) ) {
    		return replicateMWLEntries(calledAETs, getSearchDSForDate(d,d) );
    	} else {
    		return replicateMWLEntries(aet, getSearchDSForDate(d,d) );
    	}
    }
    
    public List replicateMWLEntriesForDateRange(String aet, String startDate, String endDate) throws Exception {
    	Date start = isEmpty(startDate) ? null : dfDT.parse(startDate);
    	Date end = isEmpty(endDate) ? null : dfDT.parse(endDate);
    	if ( isEmpty(aet) ) {
    		return replicateMWLEntries(calledAETs, getSearchDSForDate(start,end) );
    	} else {
        	return replicateMWLEntries(aet, getSearchDSForDate(start,end) );
    	
    	}
    }

    public final ObjectName getTemplatesServiceName() {
        return templates.getTemplatesServiceName();
    }

    public final void setTemplatesServiceName(ObjectName serviceName) {
        templates.setTemplatesServiceName(serviceName);
    }
    
	private boolean isEmpty(String param) {
		return param == null || param.length() < 1;
	}

    private Dataset getSearchDSForDate(Date baseDate, long msBefore, long msAfter) {
    	if ( msBefore < 0 && msAfter < 0) 
    		return getSearchDSForDate(baseDate, baseDate);
		Date start = msBefore < 0 ? null : new Date( baseDate.getTime() - msBefore );
		Date end = msAfter < 0 ? null : new Date( baseDate.getTime() + msAfter );
		return getSearchDSForDate(start, end);
    }
    
    private Dataset getSearchDSForDate(Date start, Date end) {
		Dataset ds = DcmObjectFactory.getInstance().newDataset();
        Dataset dsSPS = ds.putSQ(Tags.SPSSeq).addNewItem();
        dsSPS.putAE( Tags.ScheduledStationAET );
        addStartDate(start, end, dsSPS);
		
        
        dsSPS.putCS( Tags.Modality );
        dsSPS.putPN( Tags.ScheduledPerformingPhysicianName );
        dsSPS.putLO( Tags.SPSDescription );
        dsSPS.putSH( Tags.ScheduledStationName );
        dsSPS.putSH( Tags.SPSLocation );
    	Dataset dsSpcs = addCodeSQ(ds, Tags.ScheduledProtocolCodeSeq);
    	{
    		DcmElement pCtxSq = dsSpcs.putSQ( Tags.ProtocolContextSeq );
    		Dataset dsPCtxItem = pCtxSq.addNewItem();
    		dsPCtxItem.putCS( Tags.ValueType );
    		addCodeSQ(ds, Tags.ConceptNameCodeSeq);
    		dsPCtxItem.putDT( Tags.DateTime );
    		dsPCtxItem.putPN( Tags.PersonName );
    		dsPCtxItem.putUT( Tags.TextValue );
    		addCodeSQ(ds, Tags.ConceptCodeSeq);
    		dsPCtxItem.putDS( Tags.NumericValue );
        	addCodeSQ(ds, Tags.MeasurementUnitsCodeSeq);
    		//TODO: all other from protocol code SQ 
    	}
        dsSPS.putLO( Tags.PreMedication );
        dsSPS.putSH( Tags.SPSID );
        dsSPS.putLO( Tags.RequestedContrastAgent );
        dsSPS.putCS(Tags.SPSStatus);

//Requested Procedure        
        ds.putSH( Tags.RequestedProcedureID );
        ds.putLO( Tags.RequestedProcedureDescription );
        addCodeSQ(ds, Tags.RequestedProcedureCodeSeq);
        ds.putUI( Tags.StudyInstanceUID );
        {
            Dataset dsRefStdy = ds.putSQ( Tags.RefStudySeq ).addNewItem();
            dsRefStdy.putUI( Tags.RefSOPClassUID );
            dsRefStdy.putUI( Tags.RefSOPInstanceUID );
        }
        ds.putSH(Tags.RequestedProcedurePriority);
        ds.putLO(Tags.PatientTransportArrangements);
        //other Attrs from requested procedure Module
        ds.putLO(Tags.ReasonForTheRequestedProcedure);
        ds.putLT(Tags.RequestedProcedureComments);
        addCodeSQ(ds, Tags.ReasonforRequestedProcedureCodeSeq);
        ds.putSQ(Tags.RefStudySeq);
        ds.putLO(Tags.RequestedProcedureLocation);
        ds.putLO(Tags.ConfidentialityCode);
        ds.putSH(Tags.ReportingPriority);
        ds.putPN(Tags.NamesOfIntendedRecipientsOfResults);
        ds.putSQ(Tags.IntendedRecipientsOfResultsIdentificationSeq);
        
//imaging service request
        ds.putLT( Tags.ImagingServiceRequestComments );
        ds.putPN( Tags.RequestingPhysician );
        ds.putSQ(Tags.RequestingPhysicianIdentificationSeq);
        ds.putPN( Tags.ReferringPhysicianName );
        ds.putSQ(Tags.ReferringPhysicianIdentificationSeq);
        ds.putLO( Tags.RequestingService );
        ds.putSH( Tags.AccessionNumber );
        ds.putDA(Tags.IssueDateOfImagingServiceRequest);
        ds.putTM(Tags.IssueTimeOfImagingServiceRequest);
        ds.putLO( Tags.PlacerOrderNumber );
        ds.putLO( Tags.FillerOrderNumber );
        ds.putPN(Tags.OrderEnteredBy);
        ds.putSH(Tags.OrderEntererLocation);
        ds.putSH(Tags.OrderCallbackPhoneNumber);
        
//Patient/Visit Identification
        ds.putPN( Tags.PatientName );
        ds.putLO( Tags.PatientID);
        ds.putLO( Tags.AdmissionID );
//Visit Status
        ds.putLO(Tags.CurrentPatientLocation);
        
//Visit Relationship
        {
            Dataset dsRefPat = ds.putSQ( Tags.RefPatientSeq ).addNewItem();
            dsRefPat.putUI( Tags.RefSOPClassUID );
            dsRefPat.putUI( Tags.RefSOPInstanceUID );
        }
//Patient demographic
        ds.putDA( Tags.PatientBirthDate );
        ds.putCS( Tags.PatientSex );
        ds.putDS( Tags.PatientWeight );
        ds.putLO( Tags.ConfidentialityPatientData );
//Patient medical
        ds.putLO( Tags.PatientState );
        ds.putUS( Tags.PregnancyStatus );
        ds.putLO( Tags.MedicalAlerts );
        ds.putLO( Tags.ContrastAllergies );
        ds.putLO( Tags.SpecialNeeds );
        log.debug("### searchDS:");log.debug(ds);
		return ds;
	}

	private void addStartDate(Date start, Date end, Dataset dsSPS) {
		if ( start != null && end != null ) {
			if (dfDA.format(start).equals(dfDA.format(end)) ) {
				dsSPS.putDA(Tags.SPSStartDate, start);
				if ( start.getTime() != end.getTime() ) {
					dsSPS.putTM(Tags.SPSStartTime, start, end);
				} else {
					dsSPS.putTM(Tags.SPSStartTime);
				}
			} else {
				dsSPS.putDA(Tags.SPSStartDate, start, end);
				dsSPS.putTM(Tags.SPSStartTime, start, end);
			}
		} else if ( start != end ) { //one is null
			dsSPS.putDA(Tags.SPSStartDate, start, end);
			dsSPS.putTM(Tags.SPSStartTime, start, end);
		} else {
			dsSPS.putDA(Tags.SPSStartDate);
			dsSPS.putTM(Tags.SPSStartTime);
		}
	}

	private Dataset addCodeSQ(Dataset ds, int sqTag){
        DcmElement sq = ds.putSQ( sqTag );
        Dataset item = sq.addNewItem();
        item.putSH( Tags.CodeValue );
        item.putLO( Tags.CodeMeaning );
        item.putSH( Tags.CodingSchemeDesignator );
        item.putSH( Tags.CodingSchemeVersion );        
        return item;
    }
	
	private Dataset coerceAttributes(Dataset ds, String aet, String xsl) {
        Templates stylesheet = templates.getTemplatesForAET(aet, xsl);
        if (stylesheet != null) {
        	log.debug("Dataset before coercion:");log.debug(ds);
            Dataset out = DcmObjectFactory.getInstance().newDataset();
            try {
                XSLTUtils.xslt(ds, stylesheet, out);
                return out;
            } catch (Exception e) {
                log.error("MWL Replica Attribute coercion failed:", e);
            }
        }
        return ds;
	}
    private MWLManager mwlMgt() throws Exception {
        MWLManagerHome home = (MWLManagerHome) EJBHomeFactory.getFactory()
                .lookup(MWLManagerHome.class, MWLManagerHome.JNDI_NAME);
        return home.create();
    }

}

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

package org.dcm4chex.archive.dcm.mppsscu;

import java.util.Collection;
import java.util.Iterator;
import java.util.StringTokenizer;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.dcm4che.data.Dataset;
import org.dcm4che.dict.Tags;
import org.dcm4chex.archive.config.RetryIntervalls;
import org.dcm4chex.archive.ejb.interfaces.MPPSEmulator;
import org.dcm4chex.archive.ejb.interfaces.MPPSEmulatorHome;
import org.dcm4chex.archive.mbean.SchedulerDelegate;
import org.dcm4chex.archive.util.EJBHomeFactory;
import org.dcm4chex.archive.util.HomeFactoryException;
import org.jboss.system.ServiceMBeanSupport;

/**
 * @author gunter.zeilinger@tiani.com
 * @version Revision $Date: 2008-11-25 11:02:31 +0100 (Tue, 25 Nov 2008) $
 * @since 26.02.2005
 */

public class MPPSEmulatorService extends ServiceMBeanSupport implements
        NotificationListener {
    
    private static final long FIVE_MINUTES = 299999L;

    private static final String IN_PROGRESS = "IN PROGRESS";
    private static final String COMPLETED = "COMPLETED";
    private static final int[] MPPS_CREATE_TAGS = { Tags.SpecificCharacterSet,
            Tags.SOPInstanceUID, Tags.Modality, Tags.ProcedureCodeSeq,
            Tags.RefPatientSeq, Tags.PatientName, Tags.PatientID,
            Tags.IssuerOfPatientID, Tags.PatientBirthDate, Tags.PatientSex,
            Tags.StudyID, Tags.PerformedStationAET, Tags.PerformedStationName,
            Tags.PerformedLocation, Tags.PPSStartDate, Tags.PPSStartTime,
            Tags.PPSEndDate, Tags.PPSEndTime, Tags.PPSStatus, Tags.PPSID,
            Tags.PPSDescription, Tags.PerformedProcedureTypeDescription,
            Tags.PerformedProtocolCodeSeq, Tags.ScheduledStepAttributesSeq, };

    private static final int[] MPPS_SET_TAGS = { Tags.SpecificCharacterSet,
            Tags.SOPInstanceUID, Tags.PPSEndDate, Tags.PPSEndTime,
            Tags.PPSStatus, Tags.PerformedSeriesSeq, };
    
    private final SchedulerDelegate scheduler = new SchedulerDelegate(this);
    
    private long pollInterval = 0L;
    
    private Integer schedulerID;

    private String calledAET;

    private String[] stationAETs = {};
    private long[] delays;
    
    private ObjectName mppsScuServiceName;
    
    private String timerIDCheckSeriesWithoutMPPS;

    public ObjectName getSchedulerServiceName() {
        return scheduler.getSchedulerServiceName();
    }

    public void setSchedulerServiceName(ObjectName schedulerServiceName) {
        scheduler.setSchedulerServiceName(schedulerServiceName);
    }

    public final ObjectName getMppsScuServiceName() {
        return mppsScuServiceName;
    }

    public final void setMppsScuServiceName(ObjectName mppsScuServiceName) {
        this.mppsScuServiceName = mppsScuServiceName;
    }
    
    public final String getCalledAET() {
        return calledAET;
    }

    public final void setCalledAET(String calledAET) {
        this.calledAET = calledAET;
    }
    
    public final String getModalityAETitles() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < stationAETs.length; i++) {
            sb.append(stationAETs[i]);
            if (delays[i] != FIVE_MINUTES) {
                sb.append(':').append(RetryIntervalls.formatInterval(delays[i]));
            }
            sb.append("\r\n");
        }
        return sb.toString();
    }
        
    public final void setModalityAETitles(String s) {
        StringTokenizer stk = new StringTokenizer(s, "\r\n");
        stationAETs = new String[stk.countTokens()];
        delays = new long[stationAETs.length];
        int endAET;
        for (int i = 0; i < stationAETs.length; i++) {
            stationAETs[i] = stk.nextToken().trim();
            delays[i] = FIVE_MINUTES;
            endAET = stationAETs[i].indexOf(':');
            if (endAET >= 0) {
                delays[i] = RetryIntervalls.parseInterval(
                        stationAETs[i].substring(endAET+1));
                stationAETs[i] = stationAETs[i].substring(0, endAET);
            }
        }
    }
    
    public final String getPollInterval() {
        return RetryIntervalls.formatIntervalZeroAsNever(pollInterval);
    }

    public void setPollInterval(String interval) throws Exception {
        this.pollInterval = RetryIntervalls
                .parseIntervalOrNever(interval);
        if (getState() == STARTED) {
            scheduler.stopScheduler(timerIDCheckSeriesWithoutMPPS, schedulerID, this);
            schedulerID = scheduler.startScheduler(timerIDCheckSeriesWithoutMPPS,
            		pollInterval, this);
        }
    }

    public void handleNotification(Notification notification, Object handback) {
        if (stationAETs.length > 0) {
            new Thread(new Runnable(){
                public void run() {
                    emulateMPPS();
                }}).start();
        }
    }

    public int emulateMPPS() {
        log.info("Check for received series without MPPS");
        if (stationAETs.length == 0)
            return 0;
        int num = 0;
        MPPSEmulator mppsEmulator;
        try {
            mppsEmulator = getMPPSEmulatorHome().create();
        } catch (Exception e) {
            log.error("Failed to emulate MPPS:", e);
            return 0;
        }
        for (int i = 0; i < stationAETs.length; ++i) {
            Collection<Long> studyPks;
            try {
                studyPks = (Collection<Long>) mppsEmulator.getStudiesWithMissingMPPS(stationAETs[i],
                        delays[i]);
            } catch (Exception e) {
                log.error("Failed to emulate MPPS for series received from " + 
                        stationAETs[i] + " failed:", e);
                continue;
            }
            log.info("Found "+studyPks.size()+" studies with missing MPPS for sourceAET:"+stationAETs[i]);
            Dataset[] studyMpps;
            Dataset mpps;
            for ( Long studyPk : studyPks ) {
                try {
                    studyMpps = mppsEmulator.generateMPPS(studyPk);
                } catch (Exception x) {
                    log.error("Failed to emulate MPPS for Study pk:" + studyPk, x);
                    continue;
                }
                for ( int j = 0 ; j < studyMpps.length ; j++) {
                    mpps = studyMpps[j];
                    Dataset ssa = mpps.getItem(Tags.ScheduledStepAttributesSeq);
                    String suid = ssa.getString(Tags.StudyInstanceUID);
                    log.info("Emulate MPPS for Study:" + suid + " of Patient:"
                        + mpps.getString(Tags.PatientName)
                            + " received from Station:" + mpps.getString(Tags.PerformedStationAET)+" ("
                            + mpps.getString(Tags.Modality)+ ")");
                    try {
                        createMPPS(mpps);
                        updateMPPS(mpps);
                        ++num;
                    } catch (Exception e) {
                        log.error("Failed to emulate MPPS for Study:" + suid 
                                + " of Patient:" + mpps.getString(Tags.PatientName)
                                + " received from Station:" + mpps.getString(Tags.PerformedStationAET)+" ("
                                + mpps.getString(Tags.Modality)+ "):",
                                e);
                    }
                }
            }
        }
        return num;
    }

    private void fillType2Attrs(Dataset ds, int[] tags) {
        for (int i = 0; i < tags.length; i++) {
            if (!ds.contains(tags[i]))
                ds.putXX(tags[i]);
        }		
    }

    private void createMPPS(Dataset mpps) throws Exception {
        mpps.putCS(Tags.PPSStatus, IN_PROGRESS);
        fillType2Attrs(mpps, MPPS_CREATE_TAGS);
        sendMPPS(true, mpps.subSet(MPPS_CREATE_TAGS), calledAET);
    }

    private void updateMPPS(Dataset mpps) throws Exception {
        mpps.putCS(Tags.PPSStatus, COMPLETED);
        sendMPPS(false, mpps.subSet(MPPS_SET_TAGS), calledAET);
    }

    private MPPSEmulatorHome getMPPSEmulatorHome() throws HomeFactoryException {
        return (MPPSEmulatorHome) EJBHomeFactory.getFactory().lookup(
                MPPSEmulatorHome.class, MPPSEmulatorHome.JNDI_NAME);
    }

    private void sendMPPS(boolean create, Dataset mpps, String destination) 
    throws Exception {
        server.invoke(mppsScuServiceName,
                "sendMPPS", new Object[] { Boolean.valueOf(create), mpps, destination },
                new String[] { boolean.class.getName(), Dataset.class.getName(),
                        String.class.getName() });
    }

    protected void startService() throws Exception {
        schedulerID = scheduler.startScheduler(timerIDCheckSeriesWithoutMPPS,
        		pollInterval, this);
    }

    protected void stopService() throws Exception {
        scheduler.stopScheduler(timerIDCheckSeriesWithoutMPPS, schedulerID, this);
        super.stopService();
    }

	public String getTimerIDCheckSeriesWithoutMPPS() {
		return timerIDCheckSeriesWithoutMPPS;
	}

	public void setTimerIDCheckSeriesWithoutMPPS(
			String timerIDCheckSeriesWithoutMPPS) {
		this.timerIDCheckSeriesWithoutMPPS = timerIDCheckSeriesWithoutMPPS;
	}
}

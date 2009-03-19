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

package org.dcm4chex.archive.dcm.movescu;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.management.ObjectName;

import org.dcm4che.data.Command;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.dict.Status;
import org.dcm4che.dict.Tags;
import org.dcm4che.dict.UIDs;
import org.dcm4che.net.ActiveAssociation;
import org.dcm4che.net.AssociationFactory;
import org.dcm4che.net.DcmServiceException;
import org.dcm4che.net.Dimse;
import org.dcm4chex.archive.config.RetryIntervalls;
import org.dcm4chex.archive.dcm.AbstractScuService;
import org.dcm4chex.archive.mbean.JMSDelegate;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 6258 $ $Date: 2008-04-30 00:41:49 +0200 (Wed, 30 Apr 2008) $
 * @since 17.12.2003
 */
public class MoveScuService extends AbstractScuService implements
        MessageListener {

    private static final int PCID_MOVE = 1;

    private static final String DEF_CALLED_AET = "QR_SCP";

    private String calledAET = DEF_CALLED_AET;

    private HashMap retryIntervalls = new HashMap();
    private int concurrency = 1;

    private String queueName;
    
    private boolean forceCalledAET;

    private JMSDelegate jmsDelegate = new JMSDelegate(this);

    public final ObjectName getJmsServiceName() {
        return jmsDelegate.getJmsServiceName();
    }

    public final void setJmsServiceName(ObjectName jmsServiceName) {
        jmsDelegate.setJmsServiceName(jmsServiceName);
    }

    public final String getQueueName() {
        return queueName;
    }

    public final void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public final String getCalledAET() {
        return calledAET;
    }

    public final void setCalledAET(String retrieveAET) {
        this.calledAET = retrieveAET;
    }

    public final int getConcurrency() {
        return concurrency;
    }

    public final void setConcurrency(int concurrency) throws Exception {
        if (concurrency <= 0)
            throw new IllegalArgumentException("Concurrency: " + concurrency);
        if (this.concurrency != concurrency) {
            final boolean restart = getState() == STARTED;
            if (restart)
                stop();
            this.concurrency = concurrency;
            if (restart)
                start();
        }
    }

    public String getRetryIntervalls() {
        StringBuffer sb = new StringBuffer();
        Map.Entry entry;
        String key;
        String defaultIntervalls = null;
        for ( Iterator iter = retryIntervalls.entrySet().iterator() ; iter.hasNext() ; ) {
            entry = (Map.Entry) iter.next();
            key = (String)entry.getKey();
            if ( key == null ) {
                defaultIntervalls = ((RetryIntervalls) entry.getValue()).toString();
            } else {
                sb.append('[').append(key).append(']').append( (RetryIntervalls) entry.getValue() );
                sb.append(System.getProperty("line.separator", "\n"));
            }
        }
        if ( defaultIntervalls != null ) {
            sb.append(defaultIntervalls);
            sb.append(System.getProperty("line.separator", "\n"));
        }
        
        return sb.length() > 1 ? sb.toString() : "NEVER";
    }

    public void setRetryIntervalls(String text) {
        retryIntervalls.clear();
        if ( "NEVER".equals(text)) return;
        StringTokenizer st = new StringTokenizer(text,";\r\n");
        String token, key;
        int pos;
        while ( st.hasMoreTokens()) {
            token = st.nextToken();
            pos = token.indexOf(']');
            if ( pos == -1 ) {
                retryIntervalls.put(null, new RetryIntervalls(token));
            } else {
                key = token.substring(1,pos);
                retryIntervalls.put(key, new RetryIntervalls(token.substring(pos+1)));
            }
        }
    }

    public boolean isForceCalledAET() {
        return forceCalledAET;
    }

    public void setForceCalledAET(boolean forceCalledAET) {
        this.forceCalledAET = forceCalledAET;
    }

    public void scheduleMove(String retrieveAET, String destAET,
            int priority, String pid, String studyIUID, String seriesIUID,
            String[] sopIUIDs, long scheduledTime) {
        scheduleMoveOrder(new MoveOrder(retrieveAET, destAET, priority, pid,
                studyIUID, seriesIUID, sopIUIDs), scheduledTime);
    }

    public void scheduleMove(String retrieveAET, String destAET, int priority,
            String pid, String[] studyIUIDs, String[] seriesIUIDs,
            String[] sopIUIDs, long scheduledTime) {
        scheduleMoveOrder(new MoveOrder(retrieveAET, destAET, priority, pid,
                studyIUIDs, seriesIUIDs, sopIUIDs), scheduledTime);
    }

    private void scheduleMoveOrder(MoveOrder order, long scheduledTime) {
        try {
            log.info("Schedule order: " + order);            
            jmsDelegate.queue(queueName, order, JMSDelegate.toJMSPriority(order
                    .getPriority()), scheduledTime);
        } catch (Exception e) {
            log.error("Failed to schedule order: " + order);
        }
    }

    protected void startService() throws Exception {
        jmsDelegate.startListening(queueName, this, concurrency);
    }

    protected void stopService() throws Exception {
        jmsDelegate.stopListening(queueName);
    }

    public void onMessage(Message message) {
        ObjectMessage om = (ObjectMessage) message;
        try {
            MoveOrder order = (MoveOrder) om.getObject();
            log.info("Start processing " + order);
            try {
                process(order);
                log.info("Finished processing " + order);
            } catch (Exception e) {
                final int failureCount = order.getFailureCount() + 1;
                order.setFailureCount(failureCount);
                RetryIntervalls retry = (RetryIntervalls)retryIntervalls.get(order.getMoveDestination());
                if ( retry == null ) retry = (RetryIntervalls)retryIntervalls.get(null);
                final long delay = retry == null ? -1l : retry.getIntervall(failureCount);
                if (delay == -1L) {
                    log.error("Give up to process " + order, e);
                } else {
                    log.warn("Failed to process " + order
                            + ". Scheduling retry.", e);
                    scheduleMoveOrder(order, System.currentTimeMillis() + delay);
                }
            }
        } catch (Throwable e) {
            log.error("unexpected error during processing message: " + message,
                    e);
        }
    }

    private void process(MoveOrder order) throws Exception {
        String aet = order.getRetrieveAET();
        if (forceCalledAET || aet == null) {
            aet = calledAET;
        }
        
        ActiveAssociation aa = openAssociation(aet,
                UIDs.PatientRootQueryRetrieveInformationModelMOVE);
        
        try {
            invokeDimse(aa, order);
        } finally {
            try {
                aa.release(true);
                // workaround to ensure that the final MOVE-RSP is processed
                // before to continue
                Thread.sleep(10);
            } catch (Exception e) {
                log.warn(
                        "Failed to release association " + aa.getAssociation(),
                        e);
            }
        }
    }

    private void invokeDimse(ActiveAssociation aa, MoveOrder order)
            throws InterruptedException, IOException, DcmServiceException {
        AssociationFactory af = AssociationFactory.getInstance();
        DcmObjectFactory dof = DcmObjectFactory.getInstance();
        Command cmd = dof.newCommand();
        cmd.initCMoveRQ(aa.getAssociation().nextMsgID(),
                UIDs.PatientRootQueryRetrieveInformationModelMOVE, order
                        .getPriority(), order.getMoveDestination());
        Dataset ds = dof.newDataset();
        ds.putCS(Tags.QueryRetrieveLevel, order.getQueryRetrieveLevel());
        putLO(ds, Tags.PatientID, order.getPatientId());
        putUI(ds, Tags.StudyInstanceUID, order.getStudyIuids());
        putUI(ds, Tags.SeriesInstanceUID, order.getSeriesIuids());
        putUI(ds, Tags.SOPInstanceUID, order.getSopIuids());
        log.debug("Move Identifier:\n");
        log.debug(ds);
        Dimse dimseRsp = aa.invoke(af.newDimse(PCID_MOVE, cmd, ds)).get();
        Command cmdRsp = dimseRsp.getCommand();
        int status = cmdRsp.getStatus();
        if (status != 0) {
            if (status == Status.SubOpsOneOrMoreFailures
                    && order.getSopIuids() != null) {
                Dataset moveRspData = dimseRsp.getDataset();
                if (moveRspData != null) {
                    String[] failedUIDs = ds
                            .getStrings(Tags.FailedSOPInstanceUIDList);
                    if (failedUIDs != null && failedUIDs.length != 0) {
                        order.setSopIuids(failedUIDs);
                    }
                }
            }
            throw new DcmServiceException(status, cmdRsp
                    .getString(Tags.ErrorComment));
        }
    }

    private static void putLO(Dataset ds, int tag, String s) {
        if (s != null)
            ds.putLO(tag, s);
    }

    private static void putUI(Dataset ds, int tag, String[] uids) {
        if (uids != null)
            ds.putUI(tag, uids);
    }
}
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

import java.util.HashMap;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.dcm4che.data.Command;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.dict.Tags;
import org.dcm4che.dict.UIDs;
import org.dcm4che.net.ActiveAssociation;
import org.dcm4che.net.Association;
import org.dcm4che.net.AssociationFactory;
import org.dcm4che.net.DcmServiceException;
import org.dcm4che.net.Dimse;
import org.dcm4chex.archive.common.PrivateTags;
import org.dcm4chex.archive.config.ForwardingRules;
import org.dcm4chex.archive.config.RetryIntervalls;
import org.dcm4chex.archive.dcm.AbstractScuService;
import org.dcm4chex.archive.dcm.mppsscp.MPPSScpService;
import org.dcm4chex.archive.mbean.JMSDelegate;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 5722 $ $Date: 2008-01-18 16:34:15 +0100 (Fri, 18 Jan 2008) $
 * @since 15.11.2004
 * 
 */
public class MPPSScuService extends AbstractScuService implements
        MessageListener, NotificationListener {

    private static final int PCID_MPPS = 1;

    private static final int[] EXCLUDE_TAGS = { Tags.SOPClassUID,
            Tags.SOPInstanceUID };

    private RetryIntervalls retryIntervalls = new RetryIntervalls();

    private ForwardingRules forwardingRules = new ForwardingRules("");

    public final String getForwardingRules() {
        return forwardingRules.toString();
    }

    public final void setForwardingRules(String forwardingRules) {
        this.forwardingRules = new ForwardingRules(forwardingRules);
    }

    private ObjectName mppsScpServiceName;

    private String queueName;

    private int concurrency = 1;

    private JMSDelegate jmsDelegate = new JMSDelegate(this);

    public final ObjectName getJmsServiceName() {
        return jmsDelegate.getJmsServiceName();
    }

    public final void setJmsServiceName(ObjectName jmsServiceName) {
        jmsDelegate.setJmsServiceName(jmsServiceName);
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

    public final ObjectName getMppsScpServiceName() {
        return mppsScpServiceName;
    }

    public final void setMppsScpServiceName(ObjectName mppsScpServiceName) {
        this.mppsScpServiceName = mppsScpServiceName;
    }

    public final String getQueueName() {
        return queueName;
    }

    public final void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public String getRetryIntervalls() {
        return retryIntervalls.toString();
    }

    public void setRetryIntervalls(String text) {
        retryIntervalls = new RetryIntervalls(text);
    }

    protected void startService() throws Exception {
        jmsDelegate.startListening(queueName, this, concurrency);
        server.addNotificationListener(mppsScpServiceName, this,
                MPPSScpService.NOTIF_FILTER, null);
    }

    protected void stopService() throws Exception {
        server.removeNotificationListener(mppsScpServiceName, this,
                MPPSScpService.NOTIF_FILTER, null);
        jmsDelegate.stopListening(queueName);
    }

    public void handleNotification(Notification notif, Object handback) {
        Dataset mpps = (Dataset) notif.getUserData();
        mpps.setPrivateCreatorID(PrivateTags.CreatorID);
        Map param = new HashMap();
        param.put("calling", new String[] { 
                mpps.getString(PrivateTags.CallingAET) });
        String[] destAETs = forwardingRules
                .getForwardDestinationsFor(param);
        for (int i = 0; i < destAETs.length; i++) {
            MPPSOrder order = new MPPSOrder(mpps.excludePrivate(),
                    ForwardingRules.toAET(destAETs[i]));
            try {
                log.info("Scheduling " + order);
                jmsDelegate.queue(queueName, order, Message.DEFAULT_PRIORITY,
                        0L);
            } catch (Exception e) {
                log.error("Failed to schedule " + order, e);
            }
        }

    }

    public void onMessage(Message message) {
        ObjectMessage om = (ObjectMessage) message;
        try {
            MPPSOrder order = (MPPSOrder) om.getObject();
            log.info("Start processing " + order);
            try {
                sendMPPS(order.isCreate(), order.getDataset(), 
                        order.getDestination());
                log.info("Finished processing " + order);
            } catch (Exception e) {
                final int failureCount = order.getFailureCount() + 1;
                order.setFailureCount(failureCount);
                final long delay = retryIntervalls.getIntervall(failureCount);
                if (delay == -1L) {
                    log.error("Give up to process " + order, e);
                } else {
                    log.warn("Failed to process " + order
                            + ". Scheduling retry.", e);
                    jmsDelegate.queue(queueName, order, 0, System
                            .currentTimeMillis()
                            + delay);
                }
            }
        } catch (JMSException e) {
            log.error("jms error during processing message: " + message, e);
        } catch (Throwable e) {
            log.error("unexpected error during processing message: " + message,
                    e);
        }
    }

    public void sendMPPS(boolean create, Dataset mpps, String aet)
            throws Exception {
        ActiveAssociation aa = openAssociation(aet,
                UIDs.ModalityPerformedProcedureStep);
        Association a = aa.getAssociation();
        try {
            DcmObjectFactory dof = DcmObjectFactory.getInstance();
            Command cmdRq = dof.newCommand();
            if (create) {
                cmdRq.initNCreateRQ(a.nextMsgID(),
                        UIDs.ModalityPerformedProcedureStep, mpps
                                .getString(Tags.SOPInstanceUID));
            } else {
                cmdRq.initNSetRQ(a.nextMsgID(),
                        UIDs.ModalityPerformedProcedureStep, mpps
                                .getString(Tags.SOPInstanceUID));
            }
            Dimse dimseRq = AssociationFactory.getInstance()
                    .newDimse(PCID_MPPS, cmdRq, mpps.exclude(EXCLUDE_TAGS));
            final Dimse dimseRsp = aa.invoke(dimseRq).get();
            final Command cmdRsp = dimseRsp.getCommand();
            final int status = cmdRsp.getStatus();
            switch (status) {
            case 0x0000:
                break;
            case 0x0116:
                log.warn("Received Warning Status 116H " +
                        "(=Attribute Value Out of Range) from remote AE "
                        + aet);
                break;
            default:
                log.error("Received Error Status "
                        + Integer.toHexString(status) + "H, Error Comment: "
                        + cmdRsp.getString(Tags.ErrorComment));
                throw new DcmServiceException(status, cmdRsp
                        .getString(Tags.ErrorComment));
            }
        } finally {
            try {
                aa.release(true);
            } catch (Exception e) {
                log.warn("Failed to release " + aa.getAssociation());
            }
        }
    }
    
}
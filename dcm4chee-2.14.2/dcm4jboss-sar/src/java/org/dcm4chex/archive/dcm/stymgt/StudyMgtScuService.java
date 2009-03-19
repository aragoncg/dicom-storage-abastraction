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

package org.dcm4chex.archive.dcm.stymgt;

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
import org.dcm4che.dict.Status;
import org.dcm4che.dict.Tags;
import org.dcm4che.dict.UIDs;
import org.dcm4che.net.ActiveAssociation;
import org.dcm4che.net.AssociationFactory;
import org.dcm4che.net.DcmServiceException;
import org.dcm4che.net.Dimse;
import org.dcm4chex.archive.config.ForwardingRules;
import org.dcm4chex.archive.config.RetryIntervalls;
import org.dcm4chex.archive.dcm.AbstractScuService;
import org.dcm4chex.archive.mbean.JMSDelegate;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 6321 $ $Date: 2008-05-19 15:53:42 +0200 (Mon, 19 May 2008) $
 * @since 15.11.2004
 * 
 */
public class StudyMgtScuService extends AbstractScuService implements
        MessageListener, NotificationListener {

    private static final int MSG_ID = 1;

    private static final int PCID_STYMGT = 1;

    private RetryIntervalls retryIntervalls = new RetryIntervalls();

    private ForwardingRules forwardingRules = new ForwardingRules("");

    private ObjectName scpServiceName;

    private String queueName;

    private boolean retryIfNoSuchSOPInstance = false;

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

    public final String getForwardingRules() {
        return forwardingRules.toString();
    }

    public final void setForwardingRules(String forwardingRules) {
        this.forwardingRules = new ForwardingRules(forwardingRules);
    }

    public final ObjectName getStudyMgtScpServiceName() {
        return scpServiceName;
    }

    public boolean isRetryIfNoSuchSOPInstance() {
        return retryIfNoSuchSOPInstance;
    }

    public void setRetryIfNoSuchSOPInstance(boolean retry) {
        this.retryIfNoSuchSOPInstance = retry;
    }

    public final void setStudyMgtScpServiceName(ObjectName scpServiceName) {
        this.scpServiceName = scpServiceName;
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
        server.addNotificationListener(scpServiceName, this,
                StudyMgtScpService.NOTIF_FILTER, null);
    }

    protected void stopService() throws Exception {
        server.removeNotificationListener(scpServiceName, this,
                StudyMgtScpService.NOTIF_FILTER, null);
        jmsDelegate.stopListening(queueName);
    }

    public void handleNotification(Notification notif, Object handback) {
        StudyMgtOrder order = (StudyMgtOrder) notif.getUserData();
        forward(order.getCallingAET(), order.getCalledAET(), order
                .getSOPInstanceUID(), order.getCommandField(), order
                .getActionTypeID(), order.getDataset());
    }

    public int forward(String origCallingAET, String origCalledAET,
            String iuid, int commandField, int actionTypeID, Dataset dataset) {
        int count = 0;
        Map keys = new HashMap();
        keys.put("calling", new String[] { origCallingAET });
        keys.put("called", new String[] { origCalledAET });
        keys.put("command", new String[] { StudyMgtOrder.commandAsString(
                commandField, actionTypeID) });
        String[] forwardAETs = forwardingRules.getForwardDestinationsFor(keys);
        for (int i = 0; i < forwardAETs.length; i++) {
            StudyMgtOrder order = new StudyMgtOrder(origCallingAET,
                    forwardAETs[i], commandField, actionTypeID, iuid, dataset);
            try {
                log.info("Scheduling " + order);
                jmsDelegate.queue(queueName, order, Message.DEFAULT_PRIORITY,
                        0L);
                ++count;
            } catch (Exception e) {
                log.error("Failed to schedule " + order, e);
            }
        }
        return count;
    }

    public void onMessage(Message message) {
        ObjectMessage om = (ObjectMessage) message;
        try {
            StudyMgtOrder order = (StudyMgtOrder) om.getObject();
            log.info("Start processing " + order);
            try {
                try {
                    switch (order.getCommandField()) {
                    case Command.N_ACTION_RQ:
                        naction(order.getCalledAET(),
                                order.getSOPInstanceUID(), order
                                        .getActionTypeID(), order.getDataset());
                        break;
                    case Command.N_CREATE_RQ:
                        ncreate(order.getCalledAET(),
                                order.getSOPInstanceUID(), order.getDataset());
                        break;
                    case Command.N_DELETE_RQ:
                        ndelete(order.getCalledAET(), order.getSOPInstanceUID());
                        break;
                    case Command.N_SET_RQ:
                        nset(order.getCalledAET(), order.getSOPInstanceUID(),
                                order.getDataset());
                        break;
                    }
                } catch (DcmServiceException e) {
                    if (e.getStatus() != Status.NoSuchObjectInstance
                            || retryIfNoSuchSOPInstance)
                        throw e;
                    log.info("No such SOP Instance for " + order);
                }
                order.setException(null);
                log.info("Finished processing " + order);
            } catch (Exception e) {
                final int failureCount = order.getFailureCount() + 1;
                order.setFailureCount(failureCount);
                order.setException(e);
                final long delay = retryIntervalls.getIntervall(failureCount);
                if (delay == -1L) {
                    log.error("Give up to process " + order);
                } else {
                    log.warn("Failed to process " + order
                            + ". Scheduling retry.");
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

    public void ncreate(String aet, String iuid, Dataset ds)
            throws Exception {
        Command cmd = DcmObjectFactory.getInstance().newCommand();
        cmd.initNCreateRQ(MSG_ID, UIDs.Dcm4cheStudyManagement, iuid);
        checkStatus(invoke(aet, cmd, ds).getCommand());
    }

    public void nset(String aet, String iuid, Dataset ds) throws Exception {
        Command cmd = DcmObjectFactory.getInstance().newCommand();
        cmd.initNSetRQ(MSG_ID, UIDs.Dcm4cheStudyManagement, iuid);
        checkStatus(invoke(aet, cmd, ds).getCommand());
    }

    public void naction(String aet, String iuid, int actionTypeID, Dataset ds)
            throws Exception {
        Command cmd = DcmObjectFactory.getInstance().newCommand();
        cmd
                .initNActionRQ(MSG_ID, UIDs.Dcm4cheStudyManagement, iuid,
                        actionTypeID);
        checkStatus(invoke(aet, cmd, ds).getCommand());
    }

    public void ndelete(String aet, String iuid) throws Exception {
        Command cmd = DcmObjectFactory.getInstance().newCommand();
        cmd.initNDeleteRQ(MSG_ID, UIDs.Dcm4cheStudyManagement, iuid);
        checkStatus(invoke(aet, cmd, null).getCommand());
    }

    private Dimse invoke(String aet, Command cmd, Dataset ds)
            throws Exception {
        ActiveAssociation aa = openAssociation(aet,
                    UIDs.Dcm4cheStudyManagement);
        try {
            return aa.invoke(AssociationFactory.getInstance()
                    .newDimse(PCID_STYMGT, cmd, ds)).get();
        } finally {
            try {
                aa.release(true);
            } catch (Exception e) {
                log.warn("Failed to release association " + aa.getAssociation(),
                        e);
            }
        }
    }

    private void checkStatus(Command cmdRsp) throws DcmServiceException {
        if (cmdRsp.getStatus() != 0) {
            throw new DcmServiceException(cmdRsp.getStatus(), cmdRsp
                    .getString(Tags.ErrorComment));
        }
    }

}
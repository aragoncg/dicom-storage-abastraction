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

package org.dcm4chex.archive.dcm.stgcmt;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.management.JMException;
import javax.management.ObjectName;

import org.dcm4che.data.Command;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.data.DcmParser;
import org.dcm4che.data.DcmParserFactory;
import org.dcm4che.data.FileFormat;
import org.dcm4che.dict.Status;
import org.dcm4che.dict.Tags;
import org.dcm4che.dict.UIDs;
import org.dcm4che.net.AAssociateAC;
import org.dcm4che.net.AAssociateRQ;
import org.dcm4che.net.AcceptorPolicy;
import org.dcm4che.net.ActiveAssociation;
import org.dcm4che.net.Association;
import org.dcm4che.net.AssociationFactory;
import org.dcm4che.net.DcmServiceException;
import org.dcm4che.net.DcmServiceRegistry;
import org.dcm4che.net.Dimse;
import org.dcm4che.net.PDU;
import org.dcm4che.net.RoleSelection;
import org.dcm4che.util.UIDGenerator;
import org.dcm4chex.archive.common.Availability;
import org.dcm4chex.archive.config.RetryIntervalls;
import org.dcm4chex.archive.dcm.AbstractScpService;
import org.dcm4chex.archive.ejb.interfaces.AEDTO;
import org.dcm4chex.archive.ejb.interfaces.AEManager;
import org.dcm4chex.archive.ejb.interfaces.MD5;
import org.dcm4chex.archive.ejb.interfaces.Storage;
import org.dcm4chex.archive.ejb.interfaces.StorageHome;
import org.dcm4chex.archive.ejb.jdbc.FileInfo;
import org.dcm4chex.archive.ejb.jdbc.RetrieveCmd;
import org.dcm4chex.archive.exceptions.UnknownAETException;
import org.dcm4chex.archive.mbean.JMSDelegate;
import org.dcm4chex.archive.mbean.TLSConfigDelegate;
import org.dcm4chex.archive.util.EJBHomeFactory;
import org.dcm4chex.archive.util.FileUtils;

/**
 * @author <a href="mailto:gunter@tiani.com">Gunter Zeilinger</a>
 * @version $Revision: 7630 $ $Date: 2008-10-17 14:49:42 +0200 (Fri, 17 Oct 2008) $
 * @since Jan 5, 2005
 */
public class StgCmtScuScpService extends AbstractScpService implements
        MessageListener {

    private static final int MSG_ID = 1;

    private static final int ERR_STGCMT_RJ = -2;

    private static final int ERR_ASSOC_RJ = -1;

    private static final int PCID_STGCMT = 1;

    private ObjectName queryRetrieveScpServiceName;

    private String queueName = "StgCmtScuScp";

    private TLSConfigDelegate tlsConfig = new TLSConfigDelegate(this);

    private int acTimeout = 5000;

    private int dimseTimeout = 0;

    private int soCloseDelay = 500;

    private RetryIntervalls scuRetryIntervalls = new RetryIntervalls();

    private RetryIntervalls scpRetryIntervalls = new RetryIntervalls();

    private StgCmtScuScp stgCmtScuScp = new StgCmtScuScp(this);

    private long receiveResultInSameAssocTimeout;

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

    public final String getScuRetryIntervalls() {
        return scuRetryIntervalls.toString();
    }

    public final void setScuRetryIntervalls(String s) {
        this.scuRetryIntervalls = new RetryIntervalls(s);
    }

    public final String getScpRetryIntervalls() {
        return scpRetryIntervalls.toString();
    }

    public final void setScpRetryIntervalls(String s) {
        this.scpRetryIntervalls = new RetryIntervalls(s);
    }

    public final ObjectName getTLSConfigName() {
        return tlsConfig.getTLSConfigName();
    }

    public final void setTLSConfigName(ObjectName tlsConfigName) {
        tlsConfig.setTLSConfigName(tlsConfigName);
    }

    public final int getReceiveBufferSize() {
        return tlsConfig.getReceiveBufferSize();
    }

    public final void setReceiveBufferSize(int size) {
        tlsConfig.setReceiveBufferSize(size);
    }

    public final int getSendBufferSize() {
        return tlsConfig.getSendBufferSize();
    }

    public final void setSendBufferSize(int size) {
        tlsConfig.setSendBufferSize(size);
    }

    public final boolean isTcpNoDelay() {
        return tlsConfig.isTcpNoDelay();
    }

    public final void setTcpNoDelay(boolean on) {
        tlsConfig.setTcpNoDelay(on);
    }

    public final String getQueueName() {
        return queueName;
    }

    public final void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public final ObjectName getQueryRetrieveScpServiceName() {
        return queryRetrieveScpServiceName;
    }

    public final void setQueryRetrieveScpServiceName(ObjectName name) {
        this.queryRetrieveScpServiceName = name;
    }

    public final int getAcTimeout() {
        return acTimeout;
    }

    public final void setAcTimeout(int acTimeout) {
        this.acTimeout = acTimeout;
    }

    public final int getDimseTimeout() {
        return dimseTimeout;
    }

    public final void setDimseTimeout(int dimseTimeout) {
        this.dimseTimeout = dimseTimeout;
    }

    public final int getSoCloseDelay() {
        return soCloseDelay;
    }

    public final void setSoCloseDelay(int soCloseDelay) {
        this.soCloseDelay = soCloseDelay;
    }

    public final long getReceiveResultInSameAssocTimeout() {
        return receiveResultInSameAssocTimeout;
    }

    public final void setReceiveResultInSameAssocTimeout(long timeout) {
        if (timeout < 0)
            throw new IllegalArgumentException("timeout: " + timeout);
        this.receiveResultInSameAssocTimeout = timeout;
    }

    protected void bindDcmServices(DcmServiceRegistry services) {
        services.bind(UIDs.StorageCommitmentPushModel, stgCmtScuScp);
    }

    protected void unbindDcmServices(DcmServiceRegistry services) {
        services.unbind(UIDs.StorageCommitmentPushModel);
    }

    protected void enablePresContexts(AcceptorPolicy policy) {
        policy.putPresContext(UIDs.StorageCommitmentPushModel,
                valuesToStringArray(tsuidMap));
        policy.putRoleSelection(UIDs.StorageCommitmentPushModel, true, true);
    }

    protected void disablePresContexts(AcceptorPolicy policy) {
        policy.putPresContext(UIDs.StorageCommitmentPushModel, null);
        policy.removeRoleSelection(UIDs.StorageCommitmentPushModel);
    }

    boolean isLocalRetrieveAET(String aet) {
        try {
            return (Boolean) server.invoke(queryRetrieveScpServiceName,
                    "isLocalRetrieveAET", new Object[]{ aet },
                    new String[]{ String.class.getName() });
        } catch (JMException e) {
            throw new RuntimeException(
                    "Failed to invoke isLocalRetrieveAET() on "
                    + queryRetrieveScpServiceName, e);
        }
    }

    public void queueStgCmtOrder(String calling, String called,
            Dataset actionInfo, boolean scpRole) throws Exception {
        StgCmtOrder order = new StgCmtOrder(calling, called, actionInfo,
                scpRole);
        jmsDelegate.queue(queueName, order, 0, 0);
    }

    protected void startService() throws Exception {
        super.startService();
        jmsDelegate.startListening(queueName, this, concurrency);
    }

    protected void stopService() throws Exception {
        jmsDelegate.stopListening(queueName);
        super.stopService();
    }

    public AEDTO queryAEData(String aet, InetAddress addr)
            throws DcmServiceException, UnknownAETException {
        try {
            Object o = server.invoke(aeServiceName, "getAE", new Object[] {
                    aet, addr }, new String[] { String.class.getName(),
                    InetAddress.class.getName() });
            if (o == null)
                throw new UnknownAETException("Unkown AET: " + aet);
            return (AEDTO) o;
        } catch (JMException e) {
            log.error("Failed to query AEData", e);
            throw new DcmServiceException(Status.ProcessingFailure, e);
        }
    }

    public void onMessage(Message message) {
        ObjectMessage om = (ObjectMessage) message;
        try {
            StgCmtOrder order = (StgCmtOrder) om.getObject();
            log.info("Start processing " + order);
            try {
                process(order);
                log.info("Finished processing " + order);
            } catch (Exception e) {
                final int failureCount = order.getFailureCount() + 1;
                order.setFailureCount(failureCount);
                final RetryIntervalls retryIntervalls = order.isScpRole() ? scpRetryIntervalls
                        : scuRetryIntervalls;
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

    private Dataset commit(StgCmtOrder order) {
        int failureReason = Status.ProcessingFailure;
        Storage storage = null;
        try {
            StorageHome home = (StorageHome) EJBHomeFactory.getFactory()
                    .lookup(StorageHome.class, StorageHome.JNDI_NAME);
            storage = home.create();
        } catch (Exception e) {
            log.error("Failed to access Storage EJB", e);
        }
        Dataset actionInfo = order.getActionInfo();
        DcmElement refSOPSeq = actionInfo.get(Tags.RefSOPSeq);
        Map fileInfos = null;
        if (storage != null) {
            try {
                FileInfo[][] aa = RetrieveCmd.create(refSOPSeq).getFileInfos();
                fileInfos = new HashMap();
                for (int i = 0; i < aa.length; i++) {
                    fileInfos.put(aa[i][0].sopIUID, aa[i]);
                }
            } catch (SQLException e) {
                log.error("Failed to query DB", e);
            }
        }
        Dataset eventInfo = DcmObjectFactory.getInstance().newDataset();
        eventInfo.putUI(Tags.TransactionUID, actionInfo
                .getString(Tags.TransactionUID));
        DcmElement successSOPSeq = eventInfo.putSQ(Tags.RefSOPSeq);
        DcmElement failedSOPSeq = eventInfo.putSQ(Tags.FailedSOPSeq);
        for (int i = 0, n = refSOPSeq.countItems(); i < n; ++i) {
            Dataset refSOP = refSOPSeq.getItem(i);
            if (storage != null
                    && fileInfos != null
                    && (failureReason = commit(storage, refSOP, fileInfos)) == Status.Success) {
                successSOPSeq.addItem(refSOP);
            } else {
                refSOP.putUS(Tags.FailureReason, failureReason);
                failedSOPSeq.addItem(refSOP);
            }
        }
        if (failedSOPSeq.isEmpty()) {
            eventInfo.remove(Tags.FailedSOPSeq);
        }
        return eventInfo;
    }

    private void process(StgCmtOrder order) throws Exception {
        AEManager aeMgr = aeMgr();
        String aet = order.getCalledAET();
        AEDTO localAE = aeMgr.findByAET(order.getCallingAET());
        AEDTO remoteAE = aeMgr.findByAET(aet);
        Dataset ds = order.isScpRole() ? commit(order) : order.getActionInfo();
        AssociationFactory af = AssociationFactory.getInstance();
        Association a = af.newRequestor(tlsConfig.createSocket(localAE, remoteAE));
        a.setAcTimeout(acTimeout);
        a.setDimseTimeout(dimseTimeout);
        a.setSoCloseDelay(soCloseDelay);
        AAssociateRQ rq = af.newAAssociateRQ();
        rq.setCalledAET(aet);
        rq.setCallingAET(order.getCallingAET());
        rq.addPresContext(af.newPresContext(PCID_STGCMT,
                        UIDs.StorageCommitmentPushModel,
                        valuesToStringArray(tsuidMap)));
        if (order.isScpRole()) {
            rq.addRoleSelection(af.newRoleSelection(
                    UIDs.StorageCommitmentPushModel, false, true));
        }
        PDU ac = a.connect(rq);
        if (!(ac instanceof AAssociateAC)) {
            throw new DcmServiceException(ERR_ASSOC_RJ,
                    "Association not accepted by " + aet + ": " + ac);
        }
        ActiveAssociation aa = af.newActiveAssociation(a, null);
        aa.start();
        try {
            if (a.getAcceptedTransferSyntaxUID(PCID_STGCMT) == null)
                throw new DcmServiceException(ERR_STGCMT_RJ,
                        "StgCmt not supported by remote AE: " + aet);
            Command cmd = DcmObjectFactory.getInstance().newCommand();
            if (order.isScpRole()) {
                RoleSelection rs = ((AAssociateAC) ac)
                        .getRoleSelection(UIDs.StorageCommitmentPushModel);
                if (rs == null || !rs.scp()) {
                    log.warn("SCU Role of Storage Commitment Service rejected by "
                                    + aet + " - try to send N_EVENT_REPORT anyway");
                }
                cmd.initNEventReportRQ(1, UIDs.StorageCommitmentPushModel,
                        UIDs.StorageCommitmentPushModelSOPInstance,
                        ds.contains(Tags.FailedSOPSeq) ? 2 : 1);
                invokeDimse(aa, cmd, ds, "StgCmt Result:\n");
            } else {
                cmd.initNActionRQ(MSG_ID, UIDs.StorageCommitmentPushModel,
                        UIDs.StorageCommitmentPushModelSOPInstance, 1);
                ds.putUI(Tags.TransactionUID,
                        UIDGenerator.getInstance().createUID());
                invokeDimse(aa, cmd, ds, "StgCmt Request:\n");
                Thread.sleep(receiveResultInSameAssocTimeout);
            }
        } finally {
            try {
                aa.release(true);
            } catch (Exception e) {
                log.warn(
                        "Failed to release association " + aa.getAssociation(),
                        e);
            }
        }
    }

    private void invokeDimse(ActiveAssociation aa, Command cmd, Dataset ds,
            String prompt) throws InterruptedException, IOException,
            DcmServiceException {
        log.debug(prompt);
        log.debug(ds);
        final AssociationFactory af = AssociationFactory.getInstance();
        Dimse rsp = aa.invoke(af.newDimse(PCID_STGCMT, cmd, ds)).get();
        final Command cmdRsp = rsp.getCommand();
        int status = cmdRsp.getStatus();
        if (status != 0)
            throw new DcmServiceException(status, cmdRsp
                    .getString(Tags.ErrorComment));
    }

    private int commit(Storage storage, Dataset refSOP, Map fileInfos) {
        final String iuid = refSOP.getString(Tags.RefSOPInstanceUID);
        final String cuid = refSOP.getString(Tags.RefSOPClassUID);
        FileInfo[] fileInfo = (FileInfo[]) fileInfos.get(iuid);
        if (fileInfo == null) {
            log.warn("Failed Storage Commitment of Instance[uid=" + iuid
                    + "]: no such object");
            return Status.NoSuchObjectInstance;
        }
        if (!fileInfo[0].sopCUID.equals(cuid)) {
            log.warn("Failed Storage Commitment of Instance[uid=" + iuid
                    + "]: SOP Class in request[" + cuid
                    + "] does not match SOP Class in stored object["
                    + fileInfo[0].sopCUID + "]");
            return Status.ClassInstanceConflict;
        }
        try {
            LinkedHashSet retrieveAETs = new LinkedHashSet();
            for (int i = 0; i < fileInfo.length; i++) {
                retrieveAETs.add(fileInfo[i].fileRetrieveAET);
                checkFile(fileInfo[i]);
            }
            storage.commit(iuid);
            retrieveAETs.add(fileInfo[0].extRetrieveAET);
            retrieveAETs.remove(null);
            if (!retrieveAETs.isEmpty())
                refSOP.putAE(Tags.RetrieveAET, (String[]) retrieveAETs
                        .toArray(new String[retrieveAETs.size()]));
            return Status.Success;
        } catch (Exception e) {
            log.error("Failed Storage Commitment of Instance[uid="
                    + fileInfo[0].sopIUID + "]:", e);
            return Status.ProcessingFailure;
        }
    }

    private void checkFile(FileInfo info) throws IOException {
        if (info.md5 == null || info.basedir == null
                || info.availability != Availability.ONLINE
                || info.basedir.startsWith("ftp://")
                || !isLocalRetrieveAET(info.fileRetrieveAET))
            return;
        File file = FileUtils.toFile(info.basedir, info.fileID);
        log.info("M-READ file:" + file);
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(
                file));
        DigestInputStream dis = new DigestInputStream(in, md);
        try {
            DcmParser parser = DcmParserFactory.getInstance().newDcmParser(dis);
            parser.parseDcmFile(FileFormat.DICOM_FILE, Tags.PixelData);
            if ((parser.getReadTag() & 0xFFFFFFFFL) >= Tags.PixelData) {
                if (parser.getReadLength() == -1) {
                    while (parser.parseHeader() == Tags.Item) {
                        readOut(parser.getInputStream(), parser.getReadLength());
                    }
                }
                readOut(parser.getInputStream(), parser.getReadLength());
                parser.parseDataset(parser.getDcmDecodeParam(), -1);
            }
        } finally {
            try {
                dis.close();
            } catch (IOException ignore) {
            }
        }
        byte[] md5 = md.digest();
        if (!Arrays.equals(md5, MD5.toBytes(info.md5))) {
            throw new IOException("MD5 mismatch");
        }
    }

    private void readOut(InputStream in, int len) throws IOException {
        int toRead = len;
        while (toRead-- > 0) {
            if (in.read() < 0) {
                throw new EOFException();
            }
        }
    }

    void commited(Dataset stgcmtResult) throws DcmServiceException {
        Storage storage = null;
        try {
            StorageHome home = (StorageHome) EJBHomeFactory.getFactory()
                    .lookup(StorageHome.class, StorageHome.JNDI_NAME);
            storage = home.create();
            storage.commited(stgcmtResult);
        } catch (Exception e) {
            log.error("Failed update External AETs in DB records", e);
            throw new DcmServiceException(Status.ProcessingFailure, e);
        } finally {
            if (storage != null)
                try {
                    storage.remove();
                } catch (Exception ignore) {
                }
        }
    }
}

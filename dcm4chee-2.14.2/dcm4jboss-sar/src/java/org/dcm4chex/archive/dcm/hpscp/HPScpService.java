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

package org.dcm4chex.archive.dcm.hpscp;

import java.net.InetAddress;
import java.net.Socket;
import java.sql.Types;

import javax.management.JMException;
import javax.management.ObjectName;

import org.dcm4che.dict.Status;
import org.dcm4che.dict.UIDs;
import org.dcm4che.net.AcceptorPolicy;
import org.dcm4che.net.DcmServiceException;
import org.dcm4che.net.DcmServiceRegistry;
import org.dcm4chex.archive.dcm.AbstractScpService;
import org.dcm4chex.archive.ejb.interfaces.AEDTO;
import org.dcm4chex.archive.ejb.interfaces.AEManager;
import org.dcm4chex.archive.ejb.interfaces.AEManagerHome;
import org.dcm4chex.archive.ejb.jdbc.HPQueryCmd;
import org.dcm4chex.archive.ejb.jdbc.HPRetrieveCmd;
import org.dcm4chex.archive.mbean.TLSConfigDelegate;
import org.dcm4chex.archive.util.EJBHomeFactory;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 7571 $ $Date: 2008-10-16 10:02:19 +0200 (Thu, 16 Oct 2008) $
 * @since Aug 17, 2005
 */
public class HPScpService extends AbstractScpService {

    private final HPStoreScp hpStoreScp = new HPStoreScp(this);

    private final HPFindScp hpFindScp = new HPFindScp(this);

    private final HPMoveScp hpMoveScp = new HPMoveScp(this);

    private TLSConfigDelegate tlsConfig = new TLSConfigDelegate(this);

    private boolean sendPendingMoveRSP = true;

    private int acTimeout = 5000;

    private int dimseTimeout = 0;

    private int soCloseDelay = 500;

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

    public final boolean isSendPendingMoveRSP() {
        return sendPendingMoveRSP;
    }

    public final void setSendPendingMoveRSP(boolean sendPendingMoveRSP) {
        this.sendPendingMoveRSP = sendPendingMoveRSP;
    }

    public final boolean getQueryAccessBlobAsLongVarBinary() {
        return HPQueryCmd.blobAccessType == Types.LONGVARBINARY;
    }

    public final void setQueryAccessBlobAsLongVarBinary(boolean enable) {
        HPQueryCmd.blobAccessType = enable ? Types.LONGVARBINARY : Types.BLOB;
    }

    public final boolean getRetrieveAccessBlobAsLongVarBinary() {
        return HPRetrieveCmd.blobAccessType == Types.LONGVARBINARY;
    }

    public final void setRetrieveAccessBlobAsLongVarBinary(boolean enable) {
        HPRetrieveCmd.blobAccessType = enable ? Types.LONGVARBINARY : Types.BLOB;
    }

    public final String getQueryTransactionIsolationLevel() {
        return HPQueryCmd.transactionIsolationLevelAsString(
                HPQueryCmd.transactionIsolationLevel);
    }

    public final void setQueryTransactionIsolationLevel(String level) {
        HPQueryCmd.transactionIsolationLevel =
            HPQueryCmd.transactionIsolationLevelOf(level);
    }

    public final String getRetrieveTransactionIsolationLevel() {
        return HPRetrieveCmd.transactionIsolationLevelAsString(
                HPRetrieveCmd.transactionIsolationLevel);
    }

    public final void setRetrieveTransactionIsolationLevel(String level) {
        HPRetrieveCmd.transactionIsolationLevel =
            HPRetrieveCmd.transactionIsolationLevelOf(level);
    }
    
    public AEDTO queryAEData(String aet, InetAddress addr)
            throws DcmServiceException {
        try {
            Object o = server.invoke(aeServiceName, "getAE", new Object[] {
                    aet, addr }, new String[] { String.class.getName(),
                    InetAddress.class.getName() });
            if (o == null) {
                throw new DcmServiceException(Status.MoveDestinationUnknown,
                        aet);
            }
            return (AEDTO) o;
        } catch (JMException e) {
            log.error("Failed to query AEData", e);
            throw new DcmServiceException(Status.ProcessingFailure, e);
        }
    }

    Socket createSocket(String moveCalledAET, AEDTO destAE) throws Exception {
        return tlsConfig.createSocket(aeMgr().findByAET(moveCalledAET), destAE);
    }


    protected void bindDcmServices(DcmServiceRegistry services) {
        services.bind(UIDs.HangingProtocolStorage, hpStoreScp);
        services.bind(UIDs.HangingProtocolInformationModelFIND, hpFindScp);
        services.bind(UIDs.HangingProtocolInformationModelMOVE, hpMoveScp);
    }

    protected void unbindDcmServices(DcmServiceRegistry services) {
        services.unbind(UIDs.HangingProtocolStorage);
        services.unbind(UIDs.HangingProtocolInformationModelFIND);
        services.unbind(UIDs.HangingProtocolInformationModelMOVE);
    }

    protected void enablePresContexts(AcceptorPolicy policy) {
        String[] tsuids = valuesToStringArray(tsuidMap);
        policy.putPresContext(UIDs.HangingProtocolStorage, tsuids);
        policy.putPresContext(UIDs.HangingProtocolInformationModelFIND, tsuids);
        policy.putPresContext(UIDs.HangingProtocolInformationModelMOVE, tsuids);
    }

    protected void disablePresContexts(AcceptorPolicy policy) {
        policy.putPresContext(UIDs.HangingProtocolStorage, null);
        policy.putPresContext(UIDs.HangingProtocolInformationModelFIND, null);
        policy.putPresContext(UIDs.HangingProtocolInformationModelMOVE, null);
    }
}

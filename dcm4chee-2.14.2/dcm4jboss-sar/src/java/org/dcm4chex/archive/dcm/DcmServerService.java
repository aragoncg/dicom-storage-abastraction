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

package org.dcm4chex.archive.dcm;

import javax.management.Notification;
import javax.management.ObjectName;

import org.dcm4che.net.AcceptorPolicy;
import org.dcm4che.net.AssociationFactory;
import org.dcm4che.net.DcmServiceRegistry;
import org.dcm4che.net.UserIdentityNegotiator;
import org.dcm4che.server.DcmHandler;
import org.dcm4che.server.Server;
import org.dcm4che.server.ServerFactory;
import org.dcm4che.util.DcmProtocol;
import org.dcm4chex.archive.mbean.DicomSecurityDelegate;
import org.dcm4chex.archive.mbean.TLSConfigDelegate;
import org.dcm4chex.archive.notif.CallingAetChanged;
import org.jboss.system.ServiceMBeanSupport;

/**
 * @author Gunter.Zeilinger@tiani.com
 * @version $Revision: 5188 $
 * @since 02.08.2003
 */
public class DcmServerService extends ServiceMBeanSupport {

    private ServerFactory sf = ServerFactory.getInstance();

    private AssociationFactory af = AssociationFactory.getInstance();

    private AcceptorPolicy policy = af.newAcceptorPolicy();

    private DcmServiceRegistry services = af.newDcmServiceRegistry();

    private DcmHandler handler = sf.newDcmHandler(policy, services);

    private Server dcmsrv = sf.newServer(handler);
    
    private DcmProtocol protocol = DcmProtocol.DICOM;

    private TLSConfigDelegate tlsConfig = new TLSConfigDelegate(this);

    private DicomSecurityDelegate dicomSecurity =
            new DicomSecurityDelegate(this);

    public final ObjectName getTLSConfigName() {
        return tlsConfig.getTLSConfigName();
    }

    public final void setTLSConfigName(ObjectName tlsConfigName) {
        tlsConfig.setTLSConfigName(tlsConfigName);
    }

    public final ObjectName getDicomSecurityServiceName() {
        return dicomSecurity.getDicomSecurityServiceName();
    }

    public final void setDicomSecurityServiceName(ObjectName serviceName) {
        this.dicomSecurity.setDicomSecurityServiceName(serviceName);
    }

    public int getPort() {
        return dcmsrv.getPort();
    }

    public void setPort(int port) {
        dcmsrv.setPort(port);
    }

    public String getLocalAddress() {
        return dcmsrv.getLocalAddress();
    }

    public void setLocalAddress(String localAddress) {
        dcmsrv.setLocalAddress(localAddress);
    }

    public String getProtocolName() {
        return protocol.toString();
    }

    public void setProtocolName(String protocolName) {
        this.protocol = DcmProtocol.valueOf(protocolName);
    }

    public DcmHandler dcmHandler() {
        return handler;
    }

    public int getRqTimeout() {
        return handler.getRqTimeout();
    }

    public void setRqTimeout(int newRqTimeout) {
        handler.setRqTimeout(newRqTimeout);
    }

    public int getDimseTimeout() {
        return handler.getDimseTimeout();
    }

    public void setDimseTimeout(int newDimseTimeout) {
        handler.setDimseTimeout(newDimseTimeout);
    }

    public int getSoCloseDelay() {
        return handler.getSoCloseDelay();
    }

    public void setSoCloseDelay(int newSoCloseDelay) {
        handler.setSoCloseDelay(newSoCloseDelay);
    }

    public boolean isPackPDVs() {
        return handler.isPackPDVs();
    }

    public void setPackPDVs(boolean newPackPDVs) {
        handler.setPackPDVs(newPackPDVs);
    }

    public final int getReceiveBufferSize() {
        return dcmsrv.getReceiveBufferSize();        
    }
    
    public final void setReceiveBufferSize(int size) {
        dcmsrv.setReceiveBufferSize(size);
    }

    public final int getSendBufferSize() {
        return dcmsrv.getSendBufferSize();        
    }
    
    public final void setSendBufferSize(int size) {
        dcmsrv.setSendBufferSize(size);
    }
        
    public final boolean isTcpNoDelay() {
        return dcmsrv.isTcpNoDelay();
    }

    public final void setTcpNoDelay(boolean on) {
        dcmsrv.setTcpNoDelay(on);
    }
    
    public int getMaxClients() {
        return dcmsrv.getMaxClients();
    }

    public void setMaxClients(int newMaxClients) {
        dcmsrv.setMaxClients(newMaxClients);
    }

    public int getNumClients() {
        return dcmsrv.getNumClients();
    }

    public int getMaxIdleThreads() {
        return dcmsrv.getMaxIdleThreads();
    }
    
    public int getNumIdleThreads() {
        return dcmsrv.getNumIdleThreads();
    }
    
    public void setMaxIdleThreads(int max) {
        dcmsrv.setMaxIdleThreads(max);
    }
        
    public String[] getCallingAETs() {
        return policy.getCallingAETs();
    }

    public void setCallingAETs(String[] newCallingAETs) {
        policy.setCallingAETs(newCallingAETs);
    }

    public String[] getCalledAETs() {
        return policy.getCalledAETs();
    }

    public void setCalledAETs(String[] newCalledAETs) {
        policy.setCalledAETs(newCalledAETs);
    }

    public int getMaxPDULength() {
        return policy.getMaxPDULength();
    }

    public void setMaxPDULength(int newMaxPDULength) {
        policy.setMaxPDULength(newMaxPDULength);
    }

    public void notifyCallingAETchange(String[] affectedCalledAETs, String[] newCallingAETs) {
        long eventID = this.getNextNotificationSequenceNumber();
        Notification notif = new Notification(CallingAetChanged.class.getName(), this, eventID );
        notif.setUserData( new CallingAetChanged(affectedCalledAETs, newCallingAETs) );
        log.debug("send callingAET changed notif:"+notif);
        this.sendNotification( notif );
    }

    public UserIdentityNegotiator userIdentityNegotiator() {
        return dicomSecurity.userIdentityNegotiator();
    }

    protected void startService() throws Exception {
        dcmsrv.addHandshakeFailedListener(tlsConfig.handshakeFailedListener());
        dcmsrv.addHandshakeCompletedListener(tlsConfig.handshakeCompletedListener());
        dcmsrv.setServerSocketFactory(tlsConfig.serverSocketFactory(protocol
                .getCipherSuites()));
        dcmsrv.start();
    }

    protected void stopService() throws Exception {
        dcmsrv.stop();
    }
}
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
 * Agfa-Gevaert Group.
 * Portions created by the Initial Developer are Copyright (C) 2003-2005
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * See @authors listed below.
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

import java.io.IOException;

import javax.management.ObjectName;

import org.dcm4che.dict.UIDs;
import org.dcm4che.net.AAssociateAC;
import org.dcm4che.net.AAssociateRQ;
import org.dcm4che.net.ActiveAssociation;
import org.dcm4che.net.Association;
import org.dcm4che.net.AssociationFactory;
import org.dcm4che.net.DcmServiceException;
import org.dcm4che.net.PDU;
import org.dcm4che.net.PDataTF;
import org.dcm4chex.archive.ejb.interfaces.AEDTO;
import org.dcm4chex.archive.ejb.interfaces.AEManager;
import org.dcm4chex.archive.ejb.interfaces.AEManagerHome;
import org.dcm4chex.archive.mbean.TLSConfigDelegate;
import org.dcm4chex.archive.util.EJBHomeFactory;
import org.jboss.system.ServiceMBeanSupport;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision: 3340 $ $Date: 2007-05-12 18:00:31 +0200 (Sat, 12 May 2007) $
 * @since Apr 9, 2007
 */
public abstract class AbstractScuService extends ServiceMBeanSupport {

    protected static final int ERR_ASSOC_RJ = -1;
    protected static final int ERR_NO_PC_AC = -2;
    
    protected static final String[] EXPL_VRLE_TS = {
            UIDs.ExplicitVRLittleEndian,
            UIDs.ImplicitVRLittleEndian };

    protected static final String[] ONLY_DEF_TS = { 
            UIDs.ImplicitVRLittleEndian };

    protected String callingAET;
    
    protected String[] offeredTS = ONLY_DEF_TS;

    protected int acTimeout;

    protected int dimseTimeout;

    protected int soCloseDelay;

    protected int maxPDULength = PDataTF.DEF_MAX_PDU_LENGTH;    

    protected TLSConfigDelegate tlsConfig = new TLSConfigDelegate(this);
    
    public final String getCallingAET() {
        return callingAET;
    }

    public final void setCallingAET(String aet) {
        this.callingAET = aet;
    }
    
    public final boolean getExplicitVRLE() {
        return offeredTS == EXPL_VRLE_TS;
    }
    
    public void setExplicitVRLE(boolean explicitVRLE) {
        offeredTS = explicitVRLE ? EXPL_VRLE_TS : ONLY_DEF_TS;
    }
    
    public final ObjectName getTLSConfigName() {
        return tlsConfig.getTLSConfigName();
    }

    public final int getReceiveBufferSize() {
        return tlsConfig.getReceiveBufferSize();
    }

    public final void setReceiveBufferSize(int size) {
        tlsConfig.setReceiveBufferSize(size);
    }

    public final void setTLSConfigName(ObjectName tlsConfigName) {
        tlsConfig.setTLSConfigName(tlsConfigName);
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

    public final int getMaxPDULength() {
        return maxPDULength;
    }

    public final void setMaxPDULength(int maxPDULength) {
        this.maxPDULength = maxPDULength;
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
    
    public ActiveAssociation openAssociation(String calledAET, String asuid)
            throws Exception {
        return openAssociation(calledAET, new String[]{ asuid });
    }
    
    public ActiveAssociation openAssociation(AEDTO remoteAE, String asuid)
            throws Exception {
        return openAssociation(aeMgt().findByAET(callingAET), remoteAE,
                new String[]{ asuid });
    }
    
    public ActiveAssociation openAssociation(String calledAET, String[] asuids)
            throws Exception {
        AEManager aeMgt = aeMgt();
        return openAssociation(aeMgt.findByAET(callingAET), 
                aeMgt.findByAET(calledAET), asuids);
    }

    private ActiveAssociation openAssociation(AEDTO localAE, AEDTO remoteAE,
            String[] asuids) throws IOException, DcmServiceException {
        AssociationFactory af = AssociationFactory.getInstance();
        Association a = af.newRequestor(tlsConfig.createSocket(localAE, remoteAE));
        a.setAcTimeout(acTimeout);
        a.setDimseTimeout(dimseTimeout);
        a.setSoCloseDelay(soCloseDelay);
        AAssociateRQ rq = af.newAAssociateRQ();
        rq.setCalledAET(remoteAE.getTitle());
        rq.setCallingAET(callingAET);
        for (int i = 0; i < asuids.length; i++) {
            rq.addPresContext(af.newPresContext(rq.nextPCID(), asuids[i]));
        }
        rq.setMaxPDULength(maxPDULength);
        PDU ac = a.connect(rq);
        if (!(ac instanceof AAssociateAC)) {
            throw new DcmServiceException(ERR_ASSOC_RJ,
                    "Association not accepted by " + remoteAE + ": " + ac);
        }
        ActiveAssociation aa = af.newActiveAssociation(a, null);
        aa.start();
        if (((AAssociateAC) ac).countAcceptedPresContext() > 0) {
            return aa;
        }
        try {
            aa.release(false);
        } catch (Exception e) {
            log.warn("Failed to release association " + aa.getAssociation(),  e);
        }
        throw new DcmServiceException(ERR_NO_PC_AC,
                "No Presentation Context accepted by " + remoteAE + ": " + ac);
    }

    protected AEManager aeMgt() throws Exception {
        AEManagerHome home = (AEManagerHome) EJBHomeFactory.getFactory()
                .lookup(AEManagerHome.class, AEManagerHome.JNDI_NAME);
        return home.create();
    }
    
}

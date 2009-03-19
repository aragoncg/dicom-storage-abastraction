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

package org.dcm4chex.archive.mbean;

import java.rmi.RemoteException;
import java.util.List;

import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.dict.UIDs;
import org.dcm4che.net.ActiveAssociation;
import org.dcm4che.net.AssociationFactory;
import org.dcm4chex.archive.dcm.AbstractScuService;
import org.dcm4chex.archive.ejb.interfaces.AEDTO;

/**
 * <description>
 * 
 * @author <a href="mailto:franz.willer@gwi-ag.com">Franz WIller</a>
 * @since March 24, 2005
 * @version $Revision: 6690 $ $Date: 2008-07-30 12:52:19 +0200 (Wed, 30 Jul 2008) $
 */
public class EchoService extends AbstractScuService {

    private static final int PCID_ECHO = 1;


    public String[] echoAll() throws RemoteException, Exception {
        List l = aeMgt().findAll();
        String[] sa = new String[l.size()];
        AEDTO remoteAE;
        for (int i = 0, len = sa.length; i < len; i++) {
            remoteAE = (AEDTO) l.get(i);
            try {
                sa[i] = remoteAE + " : " + echo(remoteAE, new Integer(3));
            } catch (Exception x) {
                sa[i] = remoteAE + " failed:" + x.getMessage();
            }
        }
        return sa;
    }

    public String echo(String aet) throws Exception {
        return echo(aeMgt().findByAET(aet), 1);
    }
    public String echo(String aet, Integer nrOfTests) throws Exception {
        return echo(aeMgt().findByAET(aet), nrOfTests);
    }

    public String echo(AEDTO aeData, Integer nrOfTests) throws Exception {
        try {
            ActiveAssociation aa = openAssociation(aeData, UIDs.Verification);
            try {
                echo(aa, nrOfTests.intValue());
                return "Echo " + aeData + " successfully!";
            } finally {
                try {
                    aa.release(true);
                } catch (Exception e) {
                    log.warn("Failed to release " + aa.getAssociation());
                }
            }
        } catch (Exception e) {
            log.error("Echo" + aeData + " failed", e);
            return "Echo" + aeData + " failed: " + e;        
        }
    }
    
    private void echo(ActiveAssociation aa, int nrOfTests)
            throws Exception {
        AssociationFactory aFact = AssociationFactory.getInstance();
        DcmObjectFactory oFact = DcmObjectFactory.getInstance();
        for (int i = 0; i < nrOfTests; i++) {
            aa.invoke(aFact.newDimse(PCID_ECHO, 
                    oFact.newCommand().initCEchoRQ(i)), null);
        }
    }

    public boolean checkEcho(AEDTO aeData) {
        try {
            ActiveAssociation aa = openAssociation(aeData, UIDs.Verification);
            try {
                echo(aa, 1);
                return true;
            } finally {
                try {
                    aa.release(true);
                } catch (Exception e) {
                    log.warn("Failed to release " + aa.getAssociation());
                }
            }
        } catch (Exception e) {
            log.error("Echo" + aeData + " failed", e);
            return false;        
        }
    }
}

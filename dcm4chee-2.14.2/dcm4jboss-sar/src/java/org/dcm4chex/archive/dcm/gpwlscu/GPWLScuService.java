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

package org.dcm4chex.archive.dcm.gpwlscu;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.dcm4che.data.Command;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.dict.UIDs;
import org.dcm4che.net.ActiveAssociation;
import org.dcm4che.net.AssociationFactory;
import org.dcm4che.net.Dimse;
import org.dcm4che.net.FutureRSP;
import org.dcm4chex.archive.config.DicomPriority;
import org.dcm4chex.archive.dcm.AbstractScuService;
import org.dcm4chex.archive.ejb.interfaces.GPWLManager;
import org.dcm4chex.archive.ejb.interfaces.GPWLManagerHome;
import org.dcm4chex.archive.ejb.jdbc.GPWLQueryCmd;
import org.dcm4chex.archive.util.EJBHomeFactory;

/**
 * @author franz.willer
 * @version $Revision: 3262 $ $Date: 2007-04-10 13:34:35 +0200 (Tue, 10 Apr 2007) $ MBean to configure and
 *          service general purpose worklist managment issues.
 *          <p>
 * 
 */
public class GPWLScuService extends AbstractScuService {

    private static final int PCID_GPWL = 1;
    
    /** Holds the AET of general purpos worklist service. */
    private String calledAET;

    /** DICOM priority. Used for move and media creation action. */
    private int priority = 0;

    public final String getCalledAET() {
        return calledAET;
    }

    public final void setCalledAET(String aet) {
        calledAET = aet;
    }

    public final boolean isLocal() {
        return "LOCAL".equalsIgnoreCase(calledAET);
    }

    public final String getPriority() {
        return DicomPriority.toString(priority);
    }

    public final void setPriority(String priority) {
        this.priority = DicomPriority.toCode(priority);
    }

    public boolean deleteGPWLEntry(String spsID) {
        try {
            gpwlMgt().removeWorklistItem(spsID);
            log.info("GPWL entry with id " + spsID + " removed!");
            return true;
        } catch (Exception x) {
            log.error("Can't delete GPWLEntry with id:" + spsID, x);
            return false;
        }
    }

    /**
     * Get a list of work list entries.
     */
    public List findGPWLEntries(Dataset searchDS) {
        if (isLocal()) {
            return findGPWLEntriesLocal(searchDS);
        } else {
            return findGPWLEntriesFromAET(searchDS);
        }
    }

    /**
     * @param searchDS
     * @return
     */
    private List findGPWLEntriesLocal(Dataset searchDS) {
        List l = new ArrayList();
        GPWLQueryCmd queryCmd = null;
        try {
            queryCmd = new GPWLQueryCmd(searchDS);
            queryCmd.execute();
            while (queryCmd.next()) {
                l.add(queryCmd.getDataset());
            }
        } catch (SQLException x) {
            log.error("Exception in findGPWLEntriesLocal! ", x);
        }
        if (queryCmd != null)
            queryCmd.close();
        return l;
    }

    private List findGPWLEntriesFromAET(Dataset searchDS) {
        try {
            ActiveAssociation aa = openAssociation(calledAET,
                    UIDs.GeneralPurposeWorklistInformationModelFIND);
            try {
                ArrayList list = new ArrayList();
                Command cmd = DcmObjectFactory.getInstance().newCommand();
                cmd.initCFindRQ(PCID_GPWL,
                        UIDs.GeneralPurposeWorklistInformationModelFIND,
                        priority);
                Dimse mcRQ = AssociationFactory.getInstance().newDimse(1, cmd,
                        searchDS);
                if (log.isDebugEnabled())
                    log.debug("make CFIND req:" + mcRQ);
                FutureRSP rsp = aa.invoke(mcRQ);
                Dimse dimse = rsp.get();
                if (log.isDebugEnabled())
                    log.debug("CFIND resp:" + dimse);
                List pending = rsp.listPending();
                if (log.isDebugEnabled())
                    log.debug("CFIND pending:" + pending);
                Iterator iter = pending.iterator();
                while (iter.hasNext()) {
                    list.add(((Dimse) iter.next()).getDataset());
                }
                return list;
            } finally {
                try {
                    aa.release(true);
                } catch (Exception e) {
                    log.warn("Failed to release " + aa.getAssociation());
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            return Collections.EMPTY_LIST;
        }
    }

    private GPWLManager gpwlMgt() throws Exception {
        GPWLManagerHome home = (GPWLManagerHome) EJBHomeFactory.getFactory()
                .lookup(GPWLManagerHome.class, GPWLManagerHome.JNDI_NAME);
        return home.create();
    }
}

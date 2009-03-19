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

package org.dcm4chex.archive.dcm.mwlscu;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import org.dcm4che.data.Command;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.dict.Status;
import org.dcm4che.dict.Tags;
import org.dcm4che.dict.UIDs;
import org.dcm4che.net.ActiveAssociation;
import org.dcm4che.net.Association;
import org.dcm4che.net.AssociationFactory;
import org.dcm4che.net.Dimse;
import org.dcm4che.net.DimseListener;
import org.dcm4cheri.util.StringUtils;
import org.dcm4chex.archive.config.DicomPriority;
import org.dcm4chex.archive.dcm.AbstractScuService;
import org.dcm4chex.archive.ejb.interfaces.MWLManager;
import org.dcm4chex.archive.ejb.interfaces.MWLManagerHome;
import org.dcm4chex.archive.ejb.jdbc.GPWLQueryCmd;
import org.dcm4chex.archive.ejb.jdbc.MWLQueryCmd;
import org.dcm4chex.archive.util.EJBHomeFactory;

/**
 * MBean to configure and service modality worklist managment issues.
 * 
 * @author franz.willer
 * @version $Revision: 5787 $ $Date: 2008-01-31 03:48:04 +0100 (Thu, 31 Jan 2008) $
 * 
 */
public class MWLScuService extends AbstractScuService {

    private static final int PCID = 1;

    private static final int MSG_ID = 1;

    private static final int MIN_MAX_RESULT = 10;

    /** Holds the AET of modality worklist service. */
    private String calledAET;
    
    private int maxResults;

    /** DICOM priority. Used for move and media creation action. */
    private int priority = 0;

	private boolean forceMatchingKeyCheck;

    /**
     * Returns the AET that holds the work list (Modality Work List SCP).
     * 
     * @return The retrieve AET.
     */
    public final String getCalledAET() {
        return calledAET;
    }

    /**
     * Set the retrieve AET.
     * 
     * @param aet
     *            The retrieve AET to set.
     */
    public final void setCalledAET(String aet) {
        calledAET = aet;
    }

    public final int getMaxResults() {
        return maxResults;
    }

    public final void setMaxResults(int maxResults) {
        if (maxResults < MIN_MAX_RESULT) {
            throw new IllegalArgumentException(
                    "maxResult: " + maxResults + " lesser than minimal value: "
                    + MIN_MAX_RESULT);
        }
        this.maxResults = maxResults;
    }

    public boolean isForceMatchingKeyCheck() {
		return forceMatchingKeyCheck;
	}

	public void setForceMatchingKeyCheck(boolean forceMatchingKeyCheck) {
		this.forceMatchingKeyCheck = forceMatchingKeyCheck;
	}

	public final boolean isLocal() {
        return "LOCAL".equalsIgnoreCase(calledAET);
    }

    /**
     * Returns the DICOM priority as int value.
     * <p>
     * This value is used for CFIND. 0..MED, 1..HIGH, 2..LOW
     * 
     * @return Returns the priority.
     */
    public final String getPriority() {
        return DicomPriority.toString(priority);
    }

    /**
     * Set the DICOM priority.
     * 
     * @param priority
     *            The priority to set.
     */
    public final void setPriority(String priority) {
        this.priority = DicomPriority.toCode(priority);
    }

    public final boolean getAccessBlobAsLongVarBinary() {
        return MWLQueryCmd.blobAccessType == Types.LONGVARBINARY;
    }

    public final void setAccessBlobAsLongVarBinary(boolean enable) {
        MWLQueryCmd.blobAccessType = enable ? Types.LONGVARBINARY : Types.BLOB;
    }

    public final String getTransactionIsolationLevel() {
        return MWLQueryCmd.transactionIsolationLevelAsString(
                MWLQueryCmd.transactionIsolationLevel);
    }

    public final void setTransactionIsolationLevel(String level) {
        MWLQueryCmd.transactionIsolationLevel = 
                MWLQueryCmd.transactionIsolationLevelOf(level);
    }

    protected void startService() throws Exception {
        super.startService();
    }

    protected void stopService() throws Exception {
        super.stopService();
    }

    public boolean deleteMWLEntry(String rqIDspsID) {
        String[] s2 = StringUtils.split(rqIDspsID, '\\');
        try {
            mwlMgt().removeWorklistItem(s2[0], s2[1]);
            log.info("MWL entry with id " + s2[1] + " removed!");
            return true;
        } catch (Exception x) {
            log.error("Can't delete MWLEntry with id:" + s2[1], x);
            return false;
        }
    }

    /**
     * Get a list of work list entries.
     */
    public int findMWLEntries(Dataset searchDS, List result) {
        log.debug("Query MWL SCP: " + calledAET + " with keys:");
        log.debug(searchDS);
        if (isLocal()) {
            return findMWLEntriesLocal(searchDS, result);
        } else {
            return findMWLEntriesFromAET(searchDS, result);
        }
    }

    /**
     * @param searchDS
     * @return
     */
    public int findMWLEntriesLocal(Dataset searchDS, List result) {
        MWLQueryCmd queryCmd = null;
        try {
            queryCmd = new MWLQueryCmd(searchDS);
            queryCmd.execute();
            while (queryCmd.next()) {
                if (result.size() >= maxResults) {
                    log.info("Found more than " + maxResults 
                            + " matching MWL entries. Skipped!");
                    break;
                }
                Dataset rsp = queryCmd.getDataset();
                logResponse(rsp);
                result.add(rsp);
            }
            return queryCmd.isMatchNotSupported() ? 0xff01 : 0xff00;
        } catch (SQLException x) {
            log.error("Exception in findMWLEntriesLocal! ", x);
            return Status.ProcessingFailure;
        } finally {
            if (queryCmd != null)
                queryCmd.close();
        }
    }

    private void logResponse(Dataset rsp) {
        if (log.isDebugEnabled()) {
            log.debug("Received matching MWL item from " + calledAET + " :");
            log.debug(rsp);
        }
    }

    private int findMWLEntriesFromAET(Dataset searchDS, final List result) {
        try {
            ActiveAssociation aa = openAssociation(calledAET,
                    UIDs.ModalityWorklistInformationModelFIND);
            try {
                Command cmd = DcmObjectFactory.getInstance().newCommand();
                cmd.initCFindRQ(MSG_ID, UIDs.ModalityWorklistInformationModelFIND,
                        priority);
                Dimse mcRQ = AssociationFactory.getInstance().newDimse(PCID, cmd,
                        searchDS);
                final int[] pendingStatus = { 0xff00 };
                final int[] received = { 0 };
                final int[] ignored = { 0 };
                final Dataset keys = DcmObjectFactory.getInstance().newDataset();
                if (forceMatchingKeyCheck) {
	                keys.putAll(searchDS.subSet(new int[]{ Tags.AccessionNumber, Tags.StudyInstanceUID }));
	                Dataset spsItem = searchDS.getItem(Tags.SPSSeq);
	                if ( spsItem != null && spsItem.containsValue(Tags.SPSID)) {
	                	keys.putSQ(Tags.SPSSeq).addItem(spsItem.subSet( new int[]{Tags.SPSID} ));
	                }
                }
                aa.invoke(mcRQ, new DimseListener(){

                    public void dimseReceived(Association assoc, Dimse dimse) {
                        Command rspCmd = dimse.getCommand();
                        if (rspCmd.isPending()) {
                            try {
                                pendingStatus[0] = rspCmd.getStatus();
                                Dataset rsp = dimse.getDataset();
                                logResponse(rsp);
                                if (received[0] < maxResults) {
                                	received[0]++;
                                	if ( keys.isEmpty() || keys.match(rsp, true, true)) {
	                                    result.add(rsp);
	                                    if (received[0] == maxResults) {
	                                        log.info("Cancel MWL FIND operation after receive of "
	                                                + maxResults + " pending C-FIND RSP.");
	                                        cancelFind(assoc);
	                                    }
                                	} else {
                                		ignored[0]++;
                                		log.info("Received MWL FIND Response ignored after additional Matching Key check!");
                                		log.debug("Received Dataset:"); log.debug(rsp);
                                	}
                                } else {
                                    log.debug("Ignore pending C-FIND RSP received after cancel of MWL FIND operation");
                                }
                            } catch (IOException e) {
                                pendingStatus[0] = Status.ProcessingFailure;
                            }
                        } else {
                            if (log.isDebugEnabled()) {
                                log.debug("Received final C-FIND RSP from " 
                                        + calledAET + " :" + dimse);
                                if (ignored[0] > 0) {
                                	log.debug(ignored[0]+" of "+received[0]+" received Response Messages ignored after Matching Key Check!");
                                }
                            }                        
                        }
                        
                    }});
                return pendingStatus[0];
           } finally {
                try {
                    aa.release(true);
                } catch (Exception e) {
                    log.warn("Failed to release " + aa.getAssociation());
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Status.ProcessingFailure;
        }
    }

    private void cancelFind(Association assoc) {
        Command cmd = DcmObjectFactory.getInstance().newCommand();
        cmd.initCCancelRQ(MSG_ID);
        Dimse dimse = AssociationFactory.getInstance().newDimse(PCID, cmd);
        try {
            assoc.write(dimse);
        } catch (IOException e) {
            log.warn("Failed to cancel C-FIND:", e);
        }
    }
    
    private MWLManager mwlMgt() throws Exception {
        MWLManagerHome home = (MWLManagerHome) EJBHomeFactory.getFactory()
                .lookup(MWLManagerHome.class, MWLManagerHome.JNDI_NAME);
        return home.create();
    }

}

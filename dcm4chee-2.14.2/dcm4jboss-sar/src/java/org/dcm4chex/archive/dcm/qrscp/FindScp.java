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

package org.dcm4chex.archive.dcm.qrscp;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import javax.management.ObjectName;
import javax.security.auth.Subject;
import javax.xml.transform.Templates;

import org.dcm4che.data.Command;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.dict.Status;
import org.dcm4che.dict.Tags;
import org.dcm4che.net.AAssociateAC;
import org.dcm4che.net.AAssociateRQ;
import org.dcm4che.net.ActiveAssociation;
import org.dcm4che.net.Association;
import org.dcm4che.net.AssociationListener;
import org.dcm4che.net.DcmServiceBase;
import org.dcm4che.net.DcmServiceException;
import org.dcm4che.net.Dimse;
import org.dcm4che.net.DimseListener;
import org.dcm4che.net.PDU;
import org.dcm4chex.archive.ejb.jdbc.QueryCmd;
import org.dcm4chex.archive.perf.PerfCounterEnum;
import org.dcm4chex.archive.perf.PerfMonDelegate;
import org.dcm4chex.archive.perf.PerfPropertyEnum;
import org.jboss.logging.Logger;

/**
 * @author Gunter.Zeilinger@tiani.com
 * @version $Revision: 8700 $ $Date: 2008-02-27 22:38:35 +0100 (Wed, 27 Feb
 *          2008) $
 * @since 31.08.2003
 */
public class FindScp extends DcmServiceBase implements AssociationListener {
    protected static final int PID = 0;
    protected static final int ISSUER = 1;
    private static final String QUERY_XSL = "cfindrq.xsl";
    private static final String RESULT_XSL = "cfindrsp.xsl";
    private static final String QUERY_XML = "-cfindrq.xml";
    private static final String RESULT_XML = "-cfindrsp.xml";

    private static final MultiDimseRsp NO_MATCH_RSP = new MultiDimseRsp() {

        public DimseListener getCancelListener() {
            return null;
        }

        public Dataset next(ActiveAssociation assoc, Dimse rq, Command rspCmd)
                throws DcmServiceException {
            rspCmd.putUS(Tags.Status, Status.Success);
            return null;
        }

        public void release() {
        }
    };

    protected final QueryRetrieveScpService service;

    protected final boolean filterResult;

    protected final Logger log;

    private PerfMonDelegate perfMon;

    public FindScp(QueryRetrieveScpService service, boolean filterResult) {
        this.service = service;
        this.log = service.getLog();
        this.filterResult = filterResult;
        perfMon = new PerfMonDelegate(this.service);
    }

    public final ObjectName getPerfMonServiceName() {
        return perfMon.getPerfMonServiceName();
    }

    public final void setPerfMonServiceName(ObjectName perfMonServiceName) {
        perfMon.setPerfMonServiceName(perfMonServiceName);
    }

    protected MultiDimseRsp doCFind(ActiveAssociation assoc, Dimse rq,
            Command rspCmd) throws IOException, DcmServiceException {
        Association a = assoc.getAssociation();
        String callingAET = a.getCallingAET();
        try {
            perfMon.start(assoc, rq, PerfCounterEnum.C_FIND_SCP_QUERY_DB);

            Dataset rqData = rq.getDataset();
            perfMon.setProperty(assoc, rq, PerfPropertyEnum.REQ_DIMSE, rq);

            if (log.isDebugEnabled()) {
                log.debug("Identifier:\n");
                log.debug(rqData);
            }

            service.logDIMSE(a, QUERY_XML, rqData);
            service.logDicomQuery(a, rq.getCommand().getAffectedSOPClassUID(),
                    rqData);
            Dataset coerce = service.getCoercionAttributesFor(a, QUERY_XSL,
                    rqData);
            if (coerce != null) {
                service.coerceAttributes(rqData, coerce);
            }
            service.supplementIssuerOfPatientID(rqData, callingAET);
            if (!isUniversalMatching(rqData.getString(Tags.PatientID))
                    && service.isPixQueryCallingAET(callingAET)) {
                pixQuery(rqData);
            }
            boolean hideWithoutIssuerOfPID = 
                    service.isHideWithoutIssuerOfPIDFromAET(callingAET);
            MultiDimseRsp rsp;
            if (service.hasUnrestrictedQueryPermissions(callingAET)) {
                rsp = newMultiCFindRsp(rqData, hideWithoutIssuerOfPID, null);
            } else {
                Subject subject = (Subject) a.getProperty("user");
                if (subject != null) {
                    rsp = newMultiCFindRsp(rqData, hideWithoutIssuerOfPID, subject);
                } else {
                    log
                            .info("Missing user identification -> no records returned");
                    rsp = NO_MATCH_RSP;
                }
            }
            perfMon.stop(assoc, rq, PerfCounterEnum.C_FIND_SCP_QUERY_DB);
            return rsp;
        } catch (Exception e) {
            log.error("Query DB failed:", e);
            throw new DcmServiceException(Status.UnableToProcess, e);
        }
    }

    private boolean isUniversalMatching(String key) {
        if (key == null) {
            return true;
        }
        char[] a = key.toCharArray();
        for (int i = 0; i < a.length; i++) {
            if (a[i] != '*') {
                return false;
            }
        }
        return true;
    }

    private boolean isWildCardMatching(String key) {
        return key.indexOf('*') != -1 || key.indexOf('?') != -1;
    }

    protected void pixQuery(Dataset rqData) throws DcmServiceException {
        ArrayList result = new ArrayList();
        boolean updateRQ = pixQuery(rqData, result);
        DcmElement opidsq = rqData.get(Tags.OtherPatientIDSeq);
        if (opidsq != null) {
            for (int i = 0, n = opidsq.countItems(); i < n; i++) {
                if (pixQuery(opidsq.getItem(i), result)) {
                    updateRQ = true;
                }
            }
        }
        if (updateRQ) {
            Pattern pid0 = toPattern(rqData.getString(Tags.PatientID));
            Pattern issuer0 = toPattern(rqData.getString(Tags.IssuerOfPatientID));
            opidsq = rqData.putSQ(Tags.OtherPatientIDSeq);
            Iterator iter = result.iterator();
            boolean checkIfPID0 = true;
            while (iter.hasNext()) {
                 String[] pid = (String[]) iter.next();
                 if (checkIfPID0 
                         && (pid0 == null || pid0.matcher(pid[PID]).matches())
                         && (issuer0 == null 
                                 || issuer0.matcher(pid[ISSUER]).matches())) {
                    setPID(rqData, pid);
                    checkIfPID0 = false;
                 } else {
                    setPID(opidsq.addNewItem(), pid);
                }
            }
        }
    }

    private static Pattern toPattern(String wc) {
        if (wc == null) {
            return null;
        }
        if (wc.indexOf('*') == -1 && wc.indexOf('?') == -1) {
            return Pattern.compile(wc);
        }
        StringBuilder sb = new StringBuilder(wc.length() + 16);
        StringTokenizer tokens = new StringTokenizer(wc, "*?.", true);
        while (tokens.hasMoreTokens()) {
            String token = tokens.nextToken();
            if (token.equals("*")) {
                token = ".*";
            } else if (token.equals("?")) {
                token = ".";
            } else if (token.equals(".")) {
                token = "\\.";
            }
            sb.append(token);
        }
        return Pattern.compile(sb.toString());
    }

    private void setPID(Dataset rqData, String[] pid) {
        rqData.putLO(Tags.PatientID, pid[PID]);
        rqData.putLO(Tags.IssuerOfPatientID, pid[ISSUER]);
    }

    protected boolean pixQuery(Dataset rqData, ArrayList result)
            throws DcmServiceException {
        String pid = rqData.getString(Tags.PatientID);
        if (isUniversalMatching(pid)) {
            return false;
        }
        String issuer = rqData.getString(Tags.IssuerOfPatientID);
        if (!service.isPixQueryIssuer(issuer) || isWildCardMatching(pid)
                && !service.isPixQueryLocal()) {
            addNewPidAndIssuerTo(new String[] { pid, issuer }, result);
            return false;
        }
        List l = service.queryCorrespondingPIDs(pid, issuer);
        if (l.isEmpty() && !service.isPixQueryLocal()) {
            addNewPidAndIssuerTo(new String[] { pid, issuer }, result);
            return false;
        }
        for (Iterator iter = l.iterator(); iter.hasNext();) {
            addNewPidAndIssuerTo((String[]) iter.next(), result);
        }
        return true;
    }

    private boolean addNewPidAndIssuerTo(String[] pidAndIssuer, List result) {
        for (Iterator iter = result.iterator(); iter.hasNext();) {
            String[] e = (String[]) iter.next();
            if (pidAndIssuer[PID].equals(e[PID])
                    && pidAndIssuer[ISSUER].equals(e[ISSUER])) {
                return false;
            }
        }
        result.add(pidAndIssuer);
        return true;
    }

    protected MultiDimseRsp newMultiCFindRsp(Dataset rqData,
            boolean hideWithoutIssuerOfPID, Subject subject)
            throws SQLException {
        QueryCmd queryCmd = QueryCmd.create(rqData, filterResult,
                service.isNoMatchForNoValue(), hideWithoutIssuerOfPID, subject);
        queryCmd.execute();
        return new MultiCFindRsp(queryCmd);
    }

    protected Dataset getDataset(QueryCmd queryCmd) throws SQLException,
            DcmServiceException {
        return queryCmd.getDataset();
    }

    protected void doMultiRsp(ActiveAssociation assoc, Dimse rq,
            Command rspCmd, MultiDimseRsp mdr) throws IOException,
            DcmServiceException {
        try {
            DimseListener cl = mdr.getCancelListener();
            if (cl != null) {
                assoc.addCancelListener(
                        rspCmd.getMessageIDToBeingRespondedTo(), cl);
            }

            do {
                perfMon.start(assoc, rq, PerfCounterEnum.C_FIND_SCP_RESP_OUT);

                Dataset rspData = mdr.next(assoc, rq, rspCmd);
                Dimse rsp = fact.newDimse(rq.pcid(), rspCmd, rspData);
                doBeforeRsp(assoc, rsp);
                assoc.getAssociation().write(rsp);

                perfMon.setProperty(assoc, rq, PerfPropertyEnum.RSP_DATASET,
                        rspData);
                perfMon.stop(assoc, rq, PerfCounterEnum.C_FIND_SCP_RESP_OUT);

                doAfterRsp(assoc, rsp);
            } while (rspCmd.isPending());
        } finally {
            mdr.release();
        }
    }

    protected class MultiCFindRsp implements MultiDimseRsp {

        private final QueryCmd queryCmd;

        private boolean canceled = false;

        private int pendingStatus = Status.Pending;

        private int count = 0;

        private Templates coerceTpl;

        public MultiCFindRsp(QueryCmd queryCmd) {
            this.queryCmd = queryCmd;
            if (queryCmd.isMatchNotSupported()) {
                pendingStatus = 0xff01;
            } else if (service.isCheckMatchingKeySupported()
                    && queryCmd.isMatchingKeyNotSupported()) {
                pendingStatus = 0xff01;
            }
        }

        public DimseListener getCancelListener() {
            return new DimseListener() {

                public void dimseReceived(Association assoc, Dimse dimse) {
                    canceled = true;
                }
            };
        }

        public Dataset next(ActiveAssociation assoc, Dimse rq, Command rspCmd)
                throws DcmServiceException {
            if (canceled) {
                rspCmd.putUS(Tags.Status, Status.Cancel);
                return null;
            }
            try {
                Association a = assoc.getAssociation();
                queryCmd.setCoercePatientIds(service
                        .isCoerceRequestPatientIdsAET(a.getCallingAET()));
                if (!queryCmd.next()) {
                    rspCmd.putUS(Tags.Status, Status.Success);
                    return null;
                }
                rspCmd.putUS(Tags.Status, pendingStatus);
                Dataset data = getDataset(queryCmd);
                log.debug("Identifier:\n");
                log.debug(data);
                service.logDIMSE(a, RESULT_XML, data);
                if (count++ == 0) {
                    coerceTpl = service.getCoercionTemplates(a, RESULT_XSL);
                }
                Dataset coerce = service.getCoercionAttributesFor(a,
                        RESULT_XSL, data, coerceTpl);
                if (coerce != null) {
                    service.coerceAttributes(data, coerce);
                }
                return data;
            } catch (DcmServiceException e) {
                throw e;
            } catch (SQLException e) {
                log.error("Retrieve DB record failed:", e);
                throw new DcmServiceException(Status.UnableToProcess, e);
            } catch (Exception e) {
                log.error("Corrupted DB record:", e);
                throw new DcmServiceException(Status.UnableToProcess, e);
            }
        }

        public void release() {
            queryCmd.close();
        }
    }

    public void write(Association src, PDU pdu) {
        if (pdu instanceof AAssociateAC)
            perfMon.assocEstEnd(src, Command.C_FIND_RQ);
    }

    public void received(Association src, PDU pdu) {
        if (pdu instanceof AAssociateRQ)
            perfMon.assocEstStart(src, Command.C_FIND_RQ);
    }

    public void write(Association src, Dimse dimse) {
    }

    public void received(Association src, Dimse dimse) {
    }

    public void error(Association src, IOException ioe) {
    }

    public void closing(Association assoc) {
        if (assoc.getAAssociateAC() != null)
            perfMon.assocRelStart(assoc, Command.C_FIND_RQ);
    }

    public void closed(Association assoc) {
        if (assoc.getAAssociateAC() != null)
            perfMon.assocRelEnd(assoc, Command.C_FIND_RQ);
    }
}

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

package org.dcm4chex.archive.hl7;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.management.ObjectName;
import javax.xml.transform.Templates;

import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.dict.Tags;
import org.dcm4cheri.util.StringUtils;
import org.dcm4chex.archive.common.PPSStatus;
import org.dcm4chex.archive.common.SPSStatus;
import org.dcm4chex.archive.ejb.interfaces.MPPSManager;
import org.dcm4chex.archive.ejb.interfaces.MPPSManagerHome;
import org.dcm4chex.archive.ejb.interfaces.MWLManager;
import org.dcm4chex.archive.ejb.interfaces.MWLManagerHome;
import org.dcm4chex.archive.exceptions.DuplicateMWLItemException;
import org.dcm4chex.archive.exceptions.PatientMergedException;
import org.dcm4chex.archive.exceptions.PatientMismatchException;
import org.dcm4chex.archive.util.EJBHomeFactory;
import org.dcm4chex.archive.util.XSLTUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.xml.sax.ContentHandler;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 5781 $ $Date: 2008-01-29 16:36:56 +0100 (Tue, 29 Jan 2008) $
 * @since 17.02.2005
 * 
 */

public class ORMService extends AbstractHL7Service {
    
    private static final String MWL2STORE_XSL = "mwl-cfindrsp2cstorerq.xsl";

    private static final String[] OP_CODES = { "NW", "XO", "CA", "NOOP",
        "SC(SCHEDULED)", "SC(ARRIVED)", "SC(READY)", "SC(STARTED)",
        "SC(COMPLETED)", "SC(DISCONTINUED)" };
    
    private static final List OP_CODES_LIST = Arrays.asList(OP_CODES);

    private static final int NW = 0;

    private static final int XO = 1;

    private static final int CA = 2;

    private static final int NOOP = 3;
    
    private static final int SC_OFF = 4;

    private List orderControls;
    
    private int[] ops = {};
    
    private ObjectName deviceServiceName;

    private String xslPath;

    private String defaultStationAET = "UNKOWN";

    private String defaultStationName = "UNKOWN";

    private String defaultModality = "OT";

    public final String getStylesheet() {
        return xslPath;
    }

    public void setStylesheet(String path) {
        this.xslPath = path;
    }

    public final ObjectName getDeviceServiceName() {
        return deviceServiceName;
    }

    public final void setDeviceServiceName(ObjectName deviceServiceName) {
        this.deviceServiceName = deviceServiceName;
    }

    public String getOrderControlOperationMap() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < ops.length; i++) {
            sb.append(orderControls.get(i)).append(':')
                .append(OP_CODES[ops[i]]).append("\r\n");
        }
        return sb.toString();
    }

    public void setOrderControlOperationMap(String s) {
        StringTokenizer stk = new StringTokenizer(s, " \r\n\t,;");
        int lines = stk.countTokens();
        int[] newops = new int[lines];
        List newocs = new ArrayList(lines);
        for (int i = 0; i < lines; i++) {
            String[] ocop = StringUtils.split(stk.nextToken(), ':');
            if (ocop.length != 2
                    || (newops[i] = OP_CODES_LIST.indexOf(ocop[1])) == -1) {
                throw new IllegalArgumentException(s);
            }
            newocs.add(ocop[0]);            
        }
        ops = newops;
        orderControls = newocs;
    }
    
    public final String getDefaultModality() {
        return defaultModality;
    }

    public final void setDefaultModality(String defaultModality) {
        this.defaultModality = defaultModality;
    }

    public final String getDefaultStationAET() {
        return defaultStationAET;
    }

    public final void setDefaultStationAET(String defaultStationAET) {
        this.defaultStationAET = defaultStationAET;
    }

    public final String getDefaultStationName() {
        return defaultStationName;
    }

    public final void setDefaultStationName(String defaultStationName) {
        this.defaultStationName = defaultStationName;
    }

    public final String getMWL2StoreConfigDir() {
        return templates.getConfigDir();
    }

    public final void setMWL2StoreConfigDir(String path) {
        templates.setConfigDir(path);
    }

    public boolean process(MSH msh, Document msg, ContentHandler hl7out)
            throws HL7Exception {
        process(toOp(msg), msg);
        return true;
    }
    
    public void process(String orderControl, String orderStatus, Document msg)
            throws HL7Exception {
        process(new int[] { toOp(orderControl, orderStatus) }, msg);
    }
    
    private void process(int op[], Document msg) throws HL7Exception {
        try {
            Dataset ds = xslt(msg, xslPath);
            final String pid = ds.getString(Tags.PatientID);
            if (pid == null)
                throw new HL7Exception("AR",
                        "Missing required PID-3: Patient ID (Internal ID)");
            final String pname = ds.getString(Tags.PatientName);
            if (pname == null)
                throw new HL7Exception("AR",
                        "Missing required PID-5: Patient Name");
            mergeProtocolCodes(ds, op);
            ds = addScheduledStationInfo(ds);
            MWLManager mwlManager = getMWLManager();
            DcmElement spsSq = ds.remove(Tags.SPSSeq);
            Dataset sps;
            int opIdx;
            for (int i = 0, n = spsSq.countItems(); i < n; ++i) {
                sps = spsSq.getItem(i);
                ds.putSQ(Tags.SPSSeq).addItem(sps);
                adjustAttributes(ds);
                opIdx = op.length == 1 ? 0 : i;
                switch (op[opIdx]) {
                case NW:
                    addMissingAttributes(ds);
                    log("Schedule", ds);
                    logDataset("Insert MWL Item:", ds);
                    mwlManager.addWorklistItem(ds);
                    updateRequestAttributes(ds, mwlManager);
                    break;
                case XO:
                    log("Update", ds);
                    logDataset("Update MWL Item:", ds);
                    if (!mwlManager.updateWorklistItem(ds)) {
                        log("No Such ", ds);
                        addMissingAttributes(ds);
                        log("->Schedule New ", ds);
                        logDataset("Insert MWL Item:", ds);
                        mwlManager.addWorklistItem(ds);
                    }
                    updateRequestAttributes(ds, mwlManager);
                    break;
                case CA:
                    log("Cancel", ds);
                    if (mwlManager.removeWorklistItem(ds) == null) {
                        log("No Such ", ds);
                    }
                    break;
                case NOOP:
                    log("NOOP", ds);
                    break;
                default:
                    sps.putCS(Tags.SPSStatus, SPSStatus.toString(op[opIdx]-SC_OFF));
                    updateSPSStatus(ds, mwlManager);
                    break;
                }
            }
        } catch (HL7Exception e) {
            throw e;
        } catch (PatientMismatchException e) {
            throw new HL7Exception("AR", e.getMessage(), e);
        } catch (PatientMergedException e) {
            throw new HL7Exception("AR", e.getMessage(), e);
        } catch (DuplicateMWLItemException e) {
            throw new HL7Exception("AR", e.getMessage(), e);
        } catch (Exception e) {
            throw new HL7Exception("AE", e.getMessage(), e);
        }
    }

    private void updateRequestAttributes(Dataset mwlitem,
            MWLManager mwlManager) throws Exception {
        MPPSManager mppsManager = getMPPSManager();
        List mppsList = mppsManager.updateScheduledStepAttributes(mwlitem);
        if (!mppsList.isEmpty()) {
            updateSPSStatus(mwlitem, (Dataset) mppsList.get(0), mwlManager);
            updateRequestAttributesInSeries(mwlitem, mppsList, mppsManager);
        }
    }

    private void updateRequestAttributesInSeries(Dataset mwlitem, List mppsList,
            MPPSManager mppsManager) throws Exception {
        for (Iterator it = mppsList.iterator(); it.hasNext();) {
            Dataset mpps = (Dataset) it.next();
            String aet = mpps.getString(Tags.PerformedStationAET);
            Templates xslt = templates.getTemplatesForAET(aet, MWL2STORE_XSL);
            if (xslt == null) {
                log.warn("Failed to find or load stylesheet "
                            + MWL2STORE_XSL
                            + " for "
                            + aet
                            + ". Cannot update object attributes with request information.");
                continue;
            }
            Dataset rqAttrs = DcmObjectFactory.getInstance().newDataset();
            XSLTUtils.xslt(mwlitem, xslt, rqAttrs);
            DcmElement perfSeriesSq = mpps.get(Tags.PerformedSeriesSeq);
            if (perfSeriesSq != null && !perfSeriesSq.isEmpty()) {
                for (int i = 0, n = perfSeriesSq.countItems(); i < n; i++) {
                    String uid = perfSeriesSq.getItem(i)
                            .getString(Tags.SeriesInstanceUID);
                    mppsManager.updateSeriesAttributes(uid , rqAttrs, i == 0);
                }
            }
        }

    }
    
    private void updateSPSStatus(Dataset mwlitem, Dataset mpps,
            MWLManager mwlManager) throws PatientMismatchException,
            RemoteException {
        String spsStatus;
        Dataset sps = mwlitem.get(Tags.SPSSeq).getItem();
        switch (PPSStatus.toInt(mpps.getString(Tags.PPSStatus))) {
        case PPSStatus.IN_PROGRESS:
            spsStatus = "STARTED";
            break;
        case PPSStatus.COMPLETED:
            spsStatus = "COMPLETED";
            break;
        default: // PPSStatus.DISCOMPLETED
            spsStatus = "DISCONTINUED";
            break;
        }
        sps.putCS(Tags.SPSStatus, spsStatus);
        updateSPSStatus(mwlitem, mwlManager);
    }

    private void updateSPSStatus(Dataset ds, MWLManager mwlManager)
            throws PatientMismatchException, RemoteException {
        log("Change SPS status of MWL Item:", ds);
        if (!mwlManager.updateSPSStatus(ds)) {
            log("No Such ", ds);
        }
    }

    private void log(String op, Dataset ds) {
        Dataset sps = ds.getItem(Tags.SPSSeq);
        log.info(op
                + " Procedure Step[id:"
                + (sps == null ? "<unknown>(SPSSeq missing)"
                               : sps.getString(Tags.SPSID))
                + "] of Requested Procedure[id:"
                + ds.getString(Tags.RequestedProcedureID) + ", uid:"
                + ds.getString(Tags.StudyInstanceUID) + "] of Order[accNo:"
                + ds.getString(Tags.AccessionNumber) + "] for Patient [name:"
                + ds.getString(Tags.PatientName) + ",id:"
                + ds.getString(Tags.PatientID) + "]");
    }

    private MWLManager getMWLManager() throws Exception {
        return ((MWLManagerHome) EJBHomeFactory.getFactory().lookup(
                MWLManagerHome.class, MWLManagerHome.JNDI_NAME)).create();
    }

    private MPPSManager getMPPSManager() throws Exception {
        return ((MPPSManagerHome) EJBHomeFactory.getFactory().lookup(
                MPPSManagerHome.class, MPPSManagerHome.JNDI_NAME)).create();
    }

    private int[] toOp(Document msg) throws HL7Exception {
        Element rootElement = msg.getRootElement();
        List orcs = rootElement.elements("ORC");
        List obrs = rootElement.elements("OBR");
        if (orcs.isEmpty()) {
            throw new HL7Exception("AR", "Missing ORC Segment");                             
        }
        int[] op = new int[orcs.size()];
        for (int i = 0; i < op.length; i++) {           
            List orc = ((Element) orcs.get(i)).elements("field");
            String orderControl = getText(orc, 0);
            String orderStatus = getText(orc, 4);
            if (orderStatus.length() == 0 && obrs.size() > i) {
                // use Result Status (OBR-25), if no Order Status (ORC-5);
                List obr = ((Element) obrs.get(i)).elements("field");
                orderStatus = getText(obr, 24);
            }
            op[i] = toOp(orderControl, orderStatus);
        }
        return op;
    }

    private int toOp(String orderControl, String orderStatus)
            throws HL7Exception {
        int opIndex = orderControls.indexOf(orderControl + "(" + orderStatus + ")");
        if (opIndex == -1) {
            opIndex = orderControls.indexOf(orderControl);
            if (opIndex == -1) {
                throw new HL7Exception("AR", "Illegal Order Control Code ORC-1:"
                        + orderControl);
            }
        }
        return ops[opIndex];
    }

    private String getText(List fields, int i) throws HL7Exception {
        try {
            return ((Element) fields.get(i)).getText();
        } catch (IndexOutOfBoundsException e) {
            return "";
        }
    }

    private Dataset addScheduledStationInfo(Dataset spsItems) throws Exception {
        return (Dataset) server.invoke(deviceServiceName,
                "addScheduledStationInfo", new Object[] { spsItems },
                new String[] { Dataset.class.getName() });
    }

    private void addMissingAttributes(Dataset ds) {
        Dataset sps = ds.getItem(Tags.SPSSeq);
        if (!sps.containsValue(Tags.ScheduledStationAET)) {
            log.info("No Scheduled Station AET - use default: "
                    + defaultStationAET);
            sps.putAE(Tags.ScheduledStationAET, defaultStationAET);
        }
        if (!sps.containsValue(Tags.ScheduledStationName)) {
            log.info("No Scheduled Station Name - use default: "
                    + defaultStationName);
            sps.putSH(Tags.ScheduledStationName, defaultStationName);
        }
        if (!sps.containsValue(Tags.Modality)) {
            log.info("No Modality - use default: " + defaultModality);
            sps.putCS(Tags.Modality, defaultModality);
        }
        if (!sps.containsValue(Tags.SPSStartDate)) {
            log.info("No SPS Start Date - use current date/time");
            Date now = new Date();
            sps.putDA(Tags.SPSStartDate, now);
            sps.putTM(Tags.SPSStartTime, now);
        }
    }

    private void adjustAttributes(Dataset ds) {
        Dataset sps = ds.getItem(Tags.SPSSeq);
        String val;
        Dataset code;
        if ((val = sps.getString(Tags.RequestingPhysician)) != null) {
            log.info("Detect Requesting Physician on SPS Level");
            ds.putPN(Tags.RequestingPhysician, val);
            sps.remove(Tags.RequestingPhysician);
        }
        if ((val = sps.getString(Tags.RequestingService)) != null) {
            log.info("Detect Requesting Service on SPS Level");
            ds.putLO(Tags.RequestingService, val);
            sps.remove(Tags.RequestingService);
        }
        if ((val = sps.getString(Tags.StudyInstanceUID)) != null) {
            log.info("Detect Study Instance UID on SPS Level");
            ds.putUI(Tags.StudyInstanceUID, val);
            sps.remove(Tags.StudyInstanceUID);
        }
        if ((val = sps.getString(Tags.RequestedProcedurePriority)) != null) {
            log.info("Detect Requested Procedure Priority on SPS Level");
            ds.putCS(Tags.RequestedProcedurePriority, val);
            sps.remove(Tags.RequestedProcedurePriority);
        }
        if ((val = sps.getString(Tags.RequestedProcedureID)) != null) {
            log.info("Detect Requested Procedure ID on SPS Level");
            ds.putSH(Tags.RequestedProcedureID, val);
            sps.remove(Tags.RequestedProcedureID);
        }
        if ((val = sps.getString(Tags.RequestedProcedureDescription)) != null) {
            log.info("Detect Requested Procedure Description on SPS Level");
            ds.putLO(Tags.RequestedProcedureDescription, val);
            sps.remove(Tags.RequestedProcedureDescription);
        }
        if ((code = sps.getItem(Tags.RequestedProcedureCodeSeq)) != null) {
            log.info("Detect Requested Procedure Code on SPS Level");
            ds.putSQ(Tags.RequestedProcedureCodeSeq).addItem(code);
            sps.remove(Tags.RequestedProcedureCodeSeq);
        }
    }

    private void mergeProtocolCodes(Dataset orm, int[] op) {
        DcmElement prevSpsSq = orm.remove(Tags.SPSSeq);
        DcmElement newSpsSq = orm.putSQ(Tags.SPSSeq);
        HashMap spcSqMap = new HashMap();
        DcmElement spcSq0, spcSqI;
        Dataset sps;
        String spsid;
        for (int i = 0, j = 0, n = prevSpsSq.countItems(); i < n; ++i) {
            sps = prevSpsSq.getItem(i);
            spsid = sps.getString(Tags.SPSID);
            spcSqI = sps.get(Tags.ScheduledProtocolCodeSeq);
            spcSq0 = (DcmElement) spcSqMap.get(spsid);
            if (spcSq0 != null) {
                spcSq0.addItem(spcSqI.getItem());
            } else {
                spcSqMap.put(spsid, spcSqI);
                newSpsSq.addItem(sps);
                if ( op.length > 1 ) 
                    op[j++] = op[i];
            }
        }
    }
}

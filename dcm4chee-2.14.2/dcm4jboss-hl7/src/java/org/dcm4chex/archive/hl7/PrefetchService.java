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

package org.dcm4chex.archive.hl7;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXResult;

import org.dcm4che.data.Command;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.dict.Tags;
import org.dcm4che.dict.UIDs;
import org.dcm4che.net.ActiveAssociation;
import org.dcm4che.net.AssociationFactory;
import org.dcm4che.net.Dimse;
import org.dcm4che.net.FutureRSP;
import org.dcm4chex.archive.config.DicomPriority;
import org.dcm4chex.archive.config.RetryIntervalls;
import org.dcm4chex.archive.dcm.AbstractScuService;
import org.dcm4chex.archive.mbean.JMSDelegate;
import org.dcm4chex.archive.mbean.TemplatesDelegate;
import org.dcm4chex.archive.util.FileUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.DocumentSource;
import org.dom4j.io.SAXContentHandler;
import org.regenstrief.xhl7.HL7XMLLiterate;
import org.regenstrief.xhl7.HL7XMLReader;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision: 5630 $ $Date: 2008-01-02 15:15:30 +0100 (Wed, 02 Jan 2008) $
 * @since Nov 30, 2006
 */
public class PrefetchService extends AbstractScuService implements
        NotificationListener, MessageListener {

    private static final String ONLINE = "ONLINE";

    private String prefetchSourceAET;
    private String destinationQueryAET;
    private String destinationStorageAET;
    private String xslPath;
    private ObjectName hl7ServerName;
    private ObjectName moveScuServiceName;
    private String queueName;
    private RetryIntervalls retryIntervalls = new RetryIntervalls();
    private int sourceQueryPriority = 0;
    private int destinationQueryPriority = 0;
    private int retrievePriority = 0;
    
    private int concurrency = 1;

    private JMSDelegate jmsDelegate = new JMSDelegate(this);
    private TemplatesDelegate templates = new TemplatesDelegate(this);
    
    public final String getPrefetchSourceAET() {
        return prefetchSourceAET != null ? prefetchSourceAET : "NONE";
    }

    public final void setPrefetchSourceAET(String aet) {
        this.prefetchSourceAET = "NONE".equalsIgnoreCase(aet) ? null : aet;
    }
    
    public final String getDestinationQueryAET() {
        return destinationQueryAET;
    }

    public final void setDestinationQueryAET(String aet) {
        this.destinationQueryAET = aet;
    }

    public final String getDestinationStorageAET() {
        return destinationStorageAET;
    }

    public final void setDestinationStorageAET(String aet) {
        this.destinationStorageAET = aet;
    }

    public final String getSourceQueryPriority() {
        return DicomPriority.toString(sourceQueryPriority);
    }

    public final void setSourceQueryPriority(String cs) {
        this.sourceQueryPriority = DicomPriority.toCode(cs);
    }
    
    public final String getDestinationQueryPriority() {
        return DicomPriority.toString(destinationQueryPriority);
    }

    public final void setDestinationQueryPriority(String cs) {
        this.destinationQueryPriority = DicomPriority.toCode(cs);
    }

    public final String getRetrievePriority() {
        return DicomPriority.toString(retrievePriority);
    }

    public final void setRetrievePriority(String retrievePriority) {
        this.retrievePriority = DicomPriority.toCode(retrievePriority);
    }
    
    public final String getStylesheet() {
        return xslPath;
    }

    public void setStylesheet(String path) {
        this.xslPath = path;
    }
    
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

    public String getRetryIntervalls() {
        return retryIntervalls.toString();
    }

    public void setRetryIntervalls(String text) {
        retryIntervalls = new RetryIntervalls(text);
    }
    
    public final String getQueueName() {
        return queueName;
    }

    public final void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public final ObjectName getHL7ServerName() {
        return hl7ServerName;
    }

    public final void setHL7ServerName(ObjectName hl7ServerName) {
        this.hl7ServerName = hl7ServerName;
    } 

    public final ObjectName getMoveScuServiceName() {
            return moveScuServiceName;
    }

    public final void setMoveScuServiceName(ObjectName moveScuServiceName) {
            this.moveScuServiceName = moveScuServiceName;
    }

    public final ObjectName getTemplatesServiceName() {
        return templates.getTemplatesServiceName();
    }

    public final void setTemplatesServiceName(ObjectName serviceName) {
        templates.setTemplatesServiceName(serviceName);
    }
    

    protected void startService() throws Exception {
        jmsDelegate.startListening(queueName, this, concurrency);
        server.addNotificationListener(hl7ServerName, this,
                HL7ServerService.NOTIF_FILTER, null);
    }

    protected void stopService() throws Exception {
        server.removeNotificationListener(hl7ServerName, this,
                HL7ServerService.NOTIF_FILTER, null);
        jmsDelegate.stopListening(queueName);
    }
    
    public void handleNotification(Notification notif, Object handback) {
        if (prefetchSourceAET == null) {
            return;
        }
        Object[] hl7msg = (Object[]) notif.getUserData();
        Document hl7doc = (Document) hl7msg[1];
        if (isORM_O01_NW(hl7doc)) {
            Dataset findRQ = DcmObjectFactory.getInstance().newDataset();
            try {
                File xslFile = FileUtils.toExistingFile(xslPath);
                Transformer t = templates.getTemplates(xslFile).newTransformer();
                t.transform(new DocumentSource(hl7doc), new SAXResult(findRQ
                        .getSAXHandler2(null)));
            } catch (TransformerException e) {
                log.error("Failed to transform ORM into prefetch request", e);
                return;
            } catch (FileNotFoundException e) {
                log.error("No such stylesheet: " + xslPath);
                return;
            }
            prepareFindReqDS(findRQ);
            PrefetchOrder order = new PrefetchOrder(findRQ);
            try {
                log.info("Scheduling " + order);
                jmsDelegate.queue(queueName, order, Message.DEFAULT_PRIORITY,
                        0L);
            } catch (Exception e) {
                log.error("Failed to schedule " + order, e);
            }            
        }
    }

    private boolean isORM_O01_NW(Document hl7doc) {
        MSH msh = new MSH(hl7doc);
        return "ORM".equals(msh.messageType) && "O01".equals(msh.triggerEvent)
            && "NW".equals(hl7doc.getRootElement().element("ORC")
                    .element(HL7XMLLiterate.TAG_FIELD).getText());
    }

    public void onMessage(Message message) {
        ObjectMessage om = (ObjectMessage) message;
        try {
            PrefetchOrder order = (PrefetchOrder) om.getObject();
            log.info("Start processing " + order);
            try {
                process(order);
                log.info("Finished processing " + order);
            } catch (Exception e) {
                final int failureCount = order.getFailureCount() + 1;
                order.setFailureCount(failureCount);
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

    private void process(PrefetchOrder order) throws Exception {
        Dataset keys = order.getDataset();
        log.debug("SearchDS from order:");log.debug(keys);
        Map srcList = doCFIND(prefetchSourceAET, keys, sourceQueryPriority);
        Map destList = doCFIND(destinationQueryAET, keys, destinationQueryPriority);
        List notAvail = this.getListOfNotAvail(srcList, destList);
        if (notAvail.size() > 0 ) {
            log.debug("notAvail:"+notAvail);
            log.info(notAvail.size()+" Series are not available on destination AE! Schedule for Pre-Fetch");
            for ( Iterator iter = notAvail.iterator() ; iter.hasNext() ; ) {
                scheduleMove( prefetchSourceAET, destinationStorageAET, retrievePriority, 
                        (Dataset) iter.next(), 0l);
            }
        }
    }

    /**
     * @param keys
     */
    private void prepareFindReqDS(Dataset keys) {
        String qrLevel = keys.getString(Tags.QueryRetrieveLevel);
        if ( qrLevel != null && !qrLevel.equals("SERIES") ) {
            log.warn("QueryRetrieveLevel of PrefetchOrder is "+qrLevel+"! Set to SERIES!");
        }
        keys.putCS(Tags.QueryRetrieveLevel,"SERIES");
        if ( !keys.contains(Tags.PatientID) ) keys.putUI(Tags.PatientID);
        if ( !keys.contains(Tags.StudyInstanceUID) ) keys.putUI(Tags.StudyInstanceUID);
        if ( !keys.contains(Tags.SeriesInstanceUID) ) keys.putUI(Tags.SeriesInstanceUID);
        keys.putIS(Tags.NumberOfSeriesRelatedInstances);
        keys.putCS(Tags.InstanceAvailability);
    }
    
    private Map doCFIND(String calledAET, Dataset keys, int priority )
            throws Exception {
        ActiveAssociation assoc = openAssociation(calledAET,
                UIDs.StudyRootQueryRetrieveInformationModelFIND);
        try {
            Map result = new HashMap();
            // send cfind request.
            Command cmd = DcmObjectFactory.getInstance().newCommand();
            cmd.initCFindRQ(1, UIDs.StudyRootQueryRetrieveInformationModelFIND,
                    priority);
            Dimse mcRQ = AssociationFactory.getInstance().newDimse(1, cmd,
                    keys);
            FutureRSP findRsp = assoc.invoke(mcRQ);
            Dimse dimse = findRsp.get();
            List pending = findRsp.listPending();
            Iterator iter = pending.iterator();
            Dataset ds;
            while (iter.hasNext()) {
                ds = ((Dimse) iter.next()).getDataset();
                result.put(ds.getString(Tags.SeriesInstanceUID), ds);
                log.debug(calledAET+": received Dataset:");log.debug(ds);
            }
            if (log.isDebugEnabled()) {
                log.debug(calledAET+" : received final C-FIND RSP :"
                        + dimse);
            }
            return result;
        } finally {
            if (assoc != null)
                try {
                    assoc.release(true);
                } catch (Exception e1) {
                    log.error(
                            "Cant release association for CFIND"
                                    + assoc.getAssociation(), e1);
                }
        }
    }
        
    private List getListOfNotAvail(Map all, Map map ) {
        ArrayList l = new ArrayList();
        Dataset ds, dsAll;
        Entry entry;
        String seriesIUID;
        StringBuffer sb = new StringBuffer();
        for ( Iterator iter = all.entrySet().iterator() ; iter.hasNext() ; ) {
            entry = (Entry) iter.next();
            seriesIUID = (String) entry.getKey();
            dsAll = (Dataset) entry.getValue();
            ds = (Dataset) map.get(seriesIUID);
            sb.setLength(0);
            sb.append("Series ").append(seriesIUID).append(": ");
            if ( ds == null ) {
                log.debug(sb.append("Only known on source AE"));
                l.add( dsAll ); 
            } else if ( ! ONLINE.equals( ds.getString(Tags.InstanceAvailability)) ) {
                log.debug(sb.append("Instances are not available (ONLINE) on destination AE!"));
                l.add( dsAll ); 
            } else {
                int noi = ds.getInt(Tags.NumberOfSeriesRelatedInstances, -1);
                int noi1 = dsAll.getInt(Tags.NumberOfSeriesRelatedInstances, -1);
                sb.append("NumberOfSeriesRelatedInstances ");
                if ( noi == -1 || noi1 == -1 ) {
                    sb.append("is not available to check count of instances! dest:");
                    log.warn(sb.append(noi).append(" src:").append(noi1));
                } else if ( noi < noi1 ) {
                    sb.append("on destination AE is less than on source AE! dest:");
                    log.debug(sb.append(noi).append(" src:").append(noi1));
                    l.add(dsAll);
                }
            }
        }
        return l;
    }
 
    private void scheduleMove(String retrieveAET, String destAET, int priority,
            Dataset ds, long scheduledTime) {
        try {
            server.invoke(moveScuServiceName, "scheduleMove", new Object[] {
                    retrieveAET, destAET, new Integer(priority), ds.getString(Tags.PatientID),
                    ds.getString(Tags.StudyInstanceUID), 
                    ds.getString(Tags.SeriesInstanceUID), null, new Long(scheduledTime) },
                    new String[] { String.class.getName(),
                            String.class.getName(), int.class.getName(),
                            String.class.getName(), String.class.getName(),
                            String.class.getName(), String[].class.getName(),
                            long.class.getName() });
        } catch (Exception e) {
            log.error("Schedule Move failed:", e);
        }
    }
    
    
    public void processFile(String filename) throws DocumentException, IOException, SAXException {
        Dataset findRQ = DcmObjectFactory.getInstance().newDataset();
        HL7XMLReader reader = new HL7XMLReader();
        File file = new File(filename);
        SAXContentHandler hl7in = new SAXContentHandler();
        reader.setContentHandler(hl7in);
        reader.parse(new InputSource( new FileInputStream(file)));
        Document doc = hl7in.getDocument();
        try {
            File xslFile = FileUtils.toExistingFile(xslPath);
            Transformer t = templates.getTemplates(xslFile).newTransformer();
            t.transform(new DocumentSource(doc),
                    new SAXResult(findRQ.getSAXHandler2(null)));
        } catch (TransformerException e) {
            log.error("Failed to transform into prefetch request", e);
            return;
        }
        this.prepareFindReqDS(findRQ);
        PrefetchOrder order = new PrefetchOrder(findRQ);
        try {
            log.info("Scheduling Test PrefetchOrder:" + order);
            jmsDelegate.queue(queueName, order, Message.DEFAULT_PRIORITY, 0L);
        } catch (Exception e) {
            log.error("Failed to schedule Test Order" + order, e);
        }            
     }
    
    
}

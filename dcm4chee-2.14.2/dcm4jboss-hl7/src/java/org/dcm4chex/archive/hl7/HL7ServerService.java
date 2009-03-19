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

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.net.Socket;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Hashtable;

import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.ObjectName;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;

import org.dcm4che.server.Server;
import org.dcm4che.server.ServerFactory;
import org.dcm4che.util.MLLP_Protocol;
import org.dcm4cheri.util.StringUtils;
import org.dcm4chex.archive.mbean.TLSConfigDelegate;
import org.dcm4chex.archive.mbean.TemplatesDelegate;
import org.dcm4chex.archive.util.FileUtils;
import org.dom4j.Document;
import org.dom4j.io.DocumentSource;
import org.dom4j.io.SAXContentHandler;
import org.jboss.system.ServiceMBeanSupport;
import org.jboss.system.server.ServerConfigLocator;
import org.regenstrief.xhl7.HL7XMLReader;
import org.regenstrief.xhl7.HL7XMLWriter;
import org.regenstrief.xhl7.MLLPDriver;
import org.regenstrief.xhl7.XMLWriter;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 6933 $ $Date: 2008-09-03 16:07:05 +0200 (Wed, 03 Sep 2008) $
 * @since 25.10.2004
 * 
 */
public class HL7ServerService extends ServiceMBeanSupport implements
        Server.Handler {

    public static final String EVENT_TYPE = "org.dcm4chex.archive.hl7";

    public static final NotificationFilter NOTIF_FILTER = new NotificationFilter() {

        private static final long serialVersionUID = 4049637871541892405L;

        public boolean isNotificationEnabled(Notification notif) {
            return EVENT_TYPE.equals(notif.getType());
        }
    };

    private String charsetName = "ISO-8859-1";

    private String ackXslPath;

    private String logXslPath;

    private File logDir;

    private String errLogDirPath;
    private File errLogDir;
   
    private Server hl7srv = ServerFactory.getInstance().newServer(this);

    private MLLP_Protocol protocol = MLLP_Protocol.MLLP;

    private boolean fileReceivedHL7AsXML;

    private boolean fileReceivedHL7;
    
    private boolean fileReceivedHL7OnError;
    
    private boolean suppressErrorResponse;

    private boolean sendNotification;

    private int soTimeout = 0;

    private int numberOfReceivedMessages = 0;

    private TLSConfigDelegate tlsConfig = new TLSConfigDelegate(this);

    private TemplatesDelegate templates = new TemplatesDelegate(this);
    
    private Hashtable serviceRegistry = new Hashtable();

    private String[] noopMessageTypes = {};

    public final String getCharsetName() {
		return charsetName;
	}

	public final void setCharsetName(String charsetName) {
		this.charsetName = Charset.forName(charsetName).name();
	}

	public final String getAckStylesheet() {
        return ackXslPath;
    }

    public void setAckStylesheet(String path) {
        this.ackXslPath = path;
    }

    public final String getLogStylesheet() {
        return logXslPath;
    }

    public void setLogStylesheet(String path) {
        this.logXslPath = path;
    }

    public final ObjectName getTemplatesServiceName() {
        return templates.getTemplatesServiceName();
    }

    public final void setTemplatesServiceName(ObjectName serviceName) {
        templates.setTemplatesServiceName(serviceName);
    }
    
    public final ObjectName getTLSConfigName() {
        return tlsConfig.getTLSConfigName();
    }

    public final void setTLSConfigName(ObjectName tlsConfigName) {
        tlsConfig.setTLSConfigName(tlsConfigName);
    }

    public int getPort() {
        return hl7srv.getPort();
    }

    public void setPort(int port) {
        hl7srv.setPort(port);
    }

    public String getLocalAddress() {
        return hl7srv.getLocalAddress();
    }

    public void setLocalAddress(String localAddress) {
        hl7srv.setLocalAddress(localAddress);
    }

    public final String getNoopMessageTypes() {
        return StringUtils.toString(noopMessageTypes, ',');
    }

    public final void setNoopMessageTypes(String noopMessageTypes) {
        this.noopMessageTypes = StringUtils.split(noopMessageTypes, ',');
    }

    public String getProtocolName() {
        return protocol.toString();
    }

    public void setProtocolName(String protocolName) {
        this.protocol = MLLP_Protocol.valueOf(protocolName);
    }
    
    public final int getReceiveBufferSize() {
        return hl7srv.getReceiveBufferSize();        
    }
    
    public final void setReceiveBufferSize(int size) {
        hl7srv.setReceiveBufferSize(size);
    }

    public final int getSendBufferSize() {
        return hl7srv.getSendBufferSize();        
    }
    
    public final void setSendBufferSize(int size) {
        hl7srv.setSendBufferSize(size);
    }
        
    public final boolean isTcpNoDelay() {
        return hl7srv.isTcpNoDelay();
    }

    public final void setTcpNoDelay(boolean on) {
        hl7srv.setTcpNoDelay(on);
    }

    public int getMaxClients() {
        return hl7srv.getMaxClients();
    }

    public void setMaxClients(int newMaxClients) {
        hl7srv.setMaxClients(newMaxClients);
    }

    public int getNumClients() {
        return hl7srv.getNumClients();
    }

    public int getMaxIdleThreads() {
        return hl7srv.getMaxIdleThreads();
    }

    public int getNumIdleThreads() {
        return hl7srv.getNumIdleThreads();
    }

    public void setMaxIdleThreads(int max) {
        hl7srv.setMaxIdleThreads(max);
    }

    public final boolean isFileReceivedHL7AsXML() {
        return fileReceivedHL7AsXML;
    }

    public final void setFileReceivedHL7AsXML(boolean fileReceivedHL7AsXML) {
        this.fileReceivedHL7AsXML = fileReceivedHL7AsXML;
    }

    public final boolean isFileReceivedHL7() {
        return fileReceivedHL7;
    }

    public final void setFileReceivedHL7(boolean fileReceivedHL7) {
        this.fileReceivedHL7 = fileReceivedHL7;
    }

    public final boolean isFileReceivedHL7OnError() {
        return fileReceivedHL7OnError;
    }

    public final void setFileReceivedHL7OnError(boolean fileReceivedHL7) {
        this.fileReceivedHL7OnError = fileReceivedHL7;
    }

    public final String getErrorLogDirectory() {
        return errLogDirPath;
    }

    public final void setErrorLogDirectory(String errLogDirPath) {
        this.errLogDirPath = errLogDirPath;
        this.errLogDir = FileUtils.toFile(errLogDirPath);
    }

    public final boolean isSuppressErrorResponse() {
        return suppressErrorResponse;
    }

    public final void setSuppressErrorResponse(boolean suppressErrorResponse) {
        this.suppressErrorResponse = suppressErrorResponse;
    }

    public final int getSoTimeout() {
        return soTimeout;
    }

    public final void setSoTimeout(int soTimeout) {
        this.soTimeout = soTimeout;
    }

    public final boolean isSendNotification() {
        return sendNotification;
    }

    public final void setSendNotification(boolean sendNotification) {
        this.sendNotification = sendNotification;
    }

    public final int getNumberOfReceivedMessages() {
        return numberOfReceivedMessages;
    }

    public void registerService(String messageType, HL7Service service) {
        if (service != null)
            serviceRegistry.put(messageType, service);
        else
            serviceRegistry.remove(messageType);
    }

    protected void startService() throws Exception {
        logDir = new File(ServerConfigLocator.locate().getServerHomeDir(),
                "log");
        hl7srv.addHandshakeFailedListener(tlsConfig.handshakeFailedListener());
        hl7srv.addHandshakeCompletedListener(tlsConfig.handshakeCompletedListener());
        hl7srv.setServerSocketFactory(tlsConfig.serverSocketFactory(protocol
                .getCipherSuites()));
        hl7srv.start();
    }

    protected void stopService() throws Exception {
        hl7srv.stop();
    }

    public void ack(Document document, ContentHandler hl7out, HL7Exception hl7ex) {
        try {
            File ackXslFile = FileUtils.toExistingFile(ackXslPath);
            Transformer t = templates.getTemplates(ackXslFile).newTransformer();
            if (hl7ex != null) {
                t.setParameter("AcknowledgementCode", hl7ex
                        .getAcknowledgementCode());
                t.setParameter("TextMessage", hl7ex.getMessage());
            }
            t.transform(new DocumentSource(document), new SAXResult(hl7out));
        } catch (Exception e) {
            log.error("Failed to acknowlege message", e);
        }
    }

    private HL7Service getService(MSH msh) throws HL7Exception {
        String messageType = msh.messageType + '^' + msh.triggerEvent;
        HL7Service service = (HL7Service) serviceRegistry.get(messageType);
        if (service == null) {
            if (Arrays.asList(noopMessageTypes).indexOf(messageType) == -1)
                throw new HL7Exception("AR", "Unsupported message type: "
                        + messageType.replace('^', '_'));
        }
        return service;
    }

    // Server.Handler Implementation-------------------------------
    public void handle(Socket s) throws IOException {
        if (soTimeout > 0) {
            s.setSoTimeout(soTimeout);
        }
        MLLPDriver mllpDriver = new MLLPDriver(s.getInputStream(), 
                new BufferedOutputStream(s.getOutputStream()), false);
        InputStream mllpIn = mllpDriver.getInputStream();
        XMLReader xmlReader = new HL7XMLReader();
        XMLWriter xmlWriter = new HL7XMLWriter(new OutputStreamWriter(
        		mllpDriver.getOutputStream(), charsetName));
        ContentHandler hl7out = xmlWriter.getContentHandler();
        SAXContentHandler hl7in = new SAXContentHandler();
        xmlReader.setContentHandler(hl7in);
        byte[] bb = new byte[1024];
        while (mllpDriver.hasMoreInput()) {
            int msglen = 0;
            int read = 0;
            int msgNo = ++numberOfReceivedMessages;
            if (log.isDebugEnabled()) {
                log.debug("Receiving message #" + msgNo +
                                " from " + s);
            }
            do {
                msglen += read;
                if (msglen == bb.length) {
                    bb = realloc(bb, bb.length, bb.length * 2);
                }
                read = mllpIn.read(bb, msglen, bb.length - msglen);
            } while (read > 0);
            if (log.isDebugEnabled()) {
                log.debug("Received message  #" + msgNo + "of" + msglen +
                                " bytes from " + s);
            }
            if (fileReceivedHL7) {
                fileReceivedHL7(bb, msglen, new File(logDir, 
                        new DecimalFormat("'hl7-'000000'.hl7'").format(msgNo)));
            }
            try {
                try {
                    ByteArrayInputStream bbin = new ByteArrayInputStream(bb, 0,
                            msglen);
                    InputSource in = new InputSource(new InputStreamReader(
                            bbin, charsetName));
                    xmlReader.parse(in);
                    Document msg = hl7in.getDocument();
                    log.info("Received HL7 message:");
                    logMessage(msg);
                    if (fileReceivedHL7AsXML) {
                        fileReceivedHL7AsXML(msg, new File(logDir, 
                                new DecimalFormat("'hl7-'000000'.xml'").format(msgNo)));
                    }
                    MSH msh = new MSH(msg);
                    HL7Service service = getService(msh);
                    if (service == null || service.process(msh, msg, hl7out)) {
                        ack(msg, hl7out, null);
                    }
                    if (sendNotification) {
                        sendNotification(makeNotification(realloc(bb, msglen,msglen),
                                msg));
                    }
                } catch (SAXException e) {
                    throw new HL7Exception("AE", "Failed to parse message ", e);
                }
            } catch (HL7Exception e) {
                if (fileReceivedHL7OnError) {
                    fileReceivedHL7(bb, msglen, new File(errLogDir, 
                            new DecimalFormat("'hl7-'000000'.hl7'").format(msgNo)));
                }
                log.warn("Processing HL7 failed:", e);
                mllpDriver.discardPendingOutput();
                ack(hl7in.getDocument(), hl7out, suppressErrorResponse ? null : e);
            }
            if (log.isDebugEnabled()) {
                log.debug("Sending response message #" + msgNo +
                                " to " + s);
            }
            mllpDriver.turn();
            if (log.isDebugEnabled()) {
                log.debug("Sent response message #" + msgNo +
                                " to " + s);
            }
        }
    }

    private void fileReceivedHL7(byte[] bb, int msglen, File logfile) {
        if (logfile.getParentFile().mkdirs()) {
            log.info("M-WRITE " + logfile.getParentFile());
        }
        try {
            OutputStream loghl7 = new BufferedOutputStream(
                    new FileOutputStream(logfile));
            loghl7.write(bb, 0, msglen);
            loghl7.close();
        } catch (IOException e) {
            log.warn(
                    "Failed to log received HL7 message to " + logfile,
                    e);
        }
    }

    private byte[] realloc(byte[] bb, int len, int newlen) {
        byte[] out = new byte[newlen];
        System.arraycopy(bb, 0, out, 0, len);
        return out;
    }

    private void fileReceivedHL7AsXML(Document msg, File f) {
        try {
            Transformer tr = TransformerFactory.newInstance().newTransformer();
            tr.setOutputProperty(OutputKeys.INDENT, "yes");
            FileOutputStream out = new FileOutputStream(f);
            try {
                tr.transform(new DocumentSource(msg), new StreamResult(out));
            } finally {
                out.close();
            }
        } catch (Exception e) {
            log.warn("Failed to log HL7 to " + f, e);
        }
    }

    public void logMessage(Document document) {
        if (!log.isInfoEnabled())
            return;
        try {
            StringWriter out = new StringWriter();
            File logXslFile = FileUtils.toExistingFile(logXslPath);
            Transformer t = templates.getTemplates(logXslFile).newTransformer();
            t.transform(new DocumentSource(document), new StreamResult(out));
            log.info(out.toString());
        } catch (Exception e) {
            log.warn("Failed to log message", e);
        }
    }

    public boolean isSockedClosedByHandler() {
        return false;
    }

    private Notification makeNotification(byte[] hl7msg, Document msg) {
        long eventID = super.getNextNotificationSequenceNumber();
        Notification notif = new Notification(EVENT_TYPE, this, eventID);
        notif.setUserData(new Object[]{hl7msg, msg});
        return notif;
    }
}
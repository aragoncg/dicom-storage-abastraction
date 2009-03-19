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

import java.io.File;
import java.util.StringTokenizer;

import javax.management.ObjectName;
import javax.xml.transform.Transformer;
import javax.xml.transform.sax.SAXResult;

import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4chex.archive.mbean.TemplatesDelegate;
import org.dcm4chex.archive.util.FileUtils;
import org.dom4j.Document;
import org.dom4j.io.DocumentSource;
import org.jboss.system.ServiceMBeanSupport;

/**
 * @author gunter.zeilinger@tiani.com
 * @version Revision $Date: 2008-01-24 12:07:07 +0100 (Thu, 24 Jan 2008) $
 * @since 26.12.2004
 */

public abstract class AbstractHL7Service extends ServiceMBeanSupport implements
        HL7Service {

    protected TemplatesDelegate templates = new TemplatesDelegate(this);

    private ObjectName hl7ServerName;

    private String messageTypes;

    public final ObjectName getTemplatesServiceName() {
        return templates.getTemplatesServiceName();
    }

    public final void setTemplatesServiceName(ObjectName serviceName) {
        templates.setTemplatesServiceName(serviceName);
    }
    
    public final ObjectName getHL7ServerName() {
        return hl7ServerName;
    }

    public final void setHL7ServerName(ObjectName hl7ServerName) {
        this.hl7ServerName = hl7ServerName;
    }

    public final String getMessageTypes() {
        return messageTypes;
    }

    public final void setMessageTypes(String messageTypes) {
        if (getState() == STARTED)
            registerService(null);
        this.messageTypes = messageTypes;
        if (getState() == STARTED)
            registerService(this);
    }

    private void registerService(HL7Service service) {
        try {
            StringTokenizer stk = new StringTokenizer(messageTypes, ", ");
            while (stk.hasMoreTokens()) {
                server.invoke(hl7ServerName, "registerService", new Object[] {
                    stk.nextToken(), service }, new String[] {
                    String.class.getName(), HL7Service.class.getName() });
            }
        } catch (Exception e) {
            log.error("Failed to register HL7 service", e);
            throw new RuntimeException("Failed to register HL7 service", e);
        }
    }

    protected Dataset xslt(Document msg, String xslPath) throws Exception {
        Dataset ds = DcmObjectFactory.getInstance().newDataset();
        File pidXslFile = FileUtils.toExistingFile(xslPath);
        Transformer t = templates.getTemplates(pidXslFile).newTransformer();
        t.transform(new DocumentSource(msg), new SAXResult(ds
                .getSAXHandler2(null)));
        return ds;
    }

    protected void startService() throws Exception {
        registerService(this);
    }

    protected void stopService() throws Exception {
        registerService(null);
    }

    public void logDataset(String prompt, Dataset ds) {
        if (!log.isDebugEnabled()) return; 
        log.debug(prompt);
        log.debug( ds );
    }

}
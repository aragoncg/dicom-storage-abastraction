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
import java.util.Iterator;
import java.util.List;

import javax.xml.transform.Transformer;
import javax.xml.transform.sax.SAXResult;

import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.dict.Tags;
import org.dcm4che.util.Base64;
import org.dcm4che.util.UIDGenerator;
import org.dcm4chex.archive.util.FileUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.DocumentSource;
import org.xml.sax.ContentHandler;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision$ $Date$
 * @since Dec 27, 2007
 */
public class MDMService extends ORU_MDMService {

    public boolean process(MSH msh, Document msg, ContentHandler hl7out)
            throws HL7Exception {
        try
        {
            byte[] pdf = getPDF(msg);
            if (pdf == null) {
                log.warn("Ignore received " + msh.messageType + "^" +
                        msh.triggerEvent + " without encapsulated report");
                return true;
            }
            Dataset doc = DcmObjectFactory.getInstance().newDataset();
            File xslFile = FileUtils.toExistingFile(xslPath);
            Transformer t = templates.getTemplates(xslFile).newTransformer();
            t.transform(new DocumentSource(msg), new SAXResult(
                    doc.getSAXHandler2(null)));
            if (!doc.containsValue(Tags.StudyInstanceUID)) {
                log.info("No Study Instance UID in MDM - store report in new Study");
                doc.putUI(Tags.StudyInstanceUID,
                        UIDGenerator.getInstance().createUID());
            }
            doc.putOB(Tags.EncapsulatedDocument, pdf);
            storeSR(doc);
        }
        catch (Exception e)
        {
            throw new HL7Exception("AE", e.getMessage(), e);
        }      
        return true;
    }

    static String toString(Object el) {
        return el != null ? ((Element) el).getText() : "";
    }
    
    private byte[] getPDF(Document msg) {
        List obxs = msg.getRootElement().elements("OBX");
        for (Iterator iter = obxs.iterator(); iter.hasNext();) {
            Element obx = (Element) iter.next();
            List fds = obx.elements();
            if (fds.size() > 4) {
                List cmps = ((Element) fds.get(4)).elements();
                if (cmps.size() > 3 && "PDF".equals(toString(cmps.get(1)))) {
                    String obx2 = toString(fds.get(1));
                    if (!"ED".equals(obx2)) {
                        log.warn("Detect encapsulated report in OBX with OBX-2: '"
                                + obx2 + "' instead 'ED'");
                    }
                    String s = toString(cmps.remove(3));
                    return Base64.base64ToByteArray(s);
                }
           }
        }
        // hl7/OBX[field[2]='ED']/field[5]/component[4]
        // TODO Auto-generated method stub
        return null;
    }

}

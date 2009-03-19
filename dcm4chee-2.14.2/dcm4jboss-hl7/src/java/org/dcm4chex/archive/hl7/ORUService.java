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

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.dict.Tags;
import org.dcm4che.util.UIDGenerator;
import org.dcm4chex.archive.ejb.jdbc.QueryCmd;
import org.dom4j.Document;
import org.dom4j.Element;
import org.xml.sax.ContentHandler;

/**
 * @author gunter zeilinger(gunterze@gmail.com)
 * @version $Revision: 7902 $ $Date: 2008-11-03 14:57:39 +0100 (Mon, 03 Nov 2008) $
 * @since Jan 29, 2006
 *
 */
public class ORUService extends ORU_MDMService
{
    private static final int DEFAULT_STATUS_FIELD_NR = 10;//Default: Field 11 is OBSERV RESULT STATUS
    private static final String NO_RESULT_STATUS = "NO_OBSERV_RESULT_STATUS";
    private static final String NO_OBX = "NO_OBX";
    private HashSet obxIgnoreStati = new HashSet();
    private int obxStatusFieldNr = DEFAULT_STATUS_FIELD_NR;

    public void setObxIgnoreStati(String stati) {
        obxIgnoreStati.clear();
        if ( stati.equalsIgnoreCase("NONE")) return;
        int start = 0, end = stati.indexOf(':');
        if ( end != -1 ) {
            obxStatusFieldNr = Integer.parseInt(stati.substring(0,end));
            obxStatusFieldNr--;
            start = ++end;
        } else {
            obxStatusFieldNr = DEFAULT_STATUS_FIELD_NR;
        }
        for ( end = stati.indexOf(',', start) ; end != -1 ; start = ++end, end = stati.indexOf(',', start) ) {
            obxIgnoreStati.add(stati.substring(start, end));
        }
        obxIgnoreStati.add(stati.substring(start));
    }
    
    public String getObxIgnoreStati() {
        if (obxIgnoreStati.isEmpty()) return "NONE";
        StringBuffer sb = new StringBuffer();
        if (obxStatusFieldNr != 10) {
            sb.append((obxStatusFieldNr+1)).append(':');
        }
        Iterator iter = obxIgnoreStati.iterator();
        sb.append(iter.next());
        while ( iter.hasNext() ) {
            sb.append(',').append(iter.next());
        }
        return sb.toString();
    }
    
    public boolean process(MSH msh, Document msg, ContentHandler hl7out)
    throws HL7Exception {
        String status = getOBXStatus(msg);
        if ( obxIgnoreStati.contains(status) ) {
            log.info("Ignore ORU message with OBX status='"+status+"'! MSH:"+msh);
        } else {
            process(msg);
        }
        return true;
    }

    public void process(Document msg) throws HL7Exception {
        try {
            Dataset doc = xslt(msg, xslPath);
            addIUIDs(doc);
            storeSR(doc);
        } catch (Exception e) {
            throw new HL7Exception("AE", e.getMessage(), e);
        }
    }

    private String getOBXStatus(Document msg) {
        Element rootElement = msg.getRootElement();
        log.info("rootElement:"+rootElement);
        List obxs = rootElement.elements("OBX");
        log.info("obxs:"+obxs);
        if ( obxs.isEmpty()) return NO_OBX;
        List obxFields = ((Element) obxs.get(0)).elements("field");
        log.info("obxFields:"+obxFields);
        if ( obxFields.size() < obxStatusFieldNr) 
            return NO_RESULT_STATUS;
        log.info("obxFields.get(10)).getText():"+((Element) obxFields.get(obxStatusFieldNr)).getText());
        return ((Element) obxFields.get(obxStatusFieldNr)).getText();
    }
    
    private void addIUIDs(Dataset sr) {
        UIDGenerator uidgen = UIDGenerator.getInstance();
        if (!sr.containsValue(Tags.StudyInstanceUID)) {
            if (!addSUIDs(sr)) {
                sr.putUI(Tags.StudyInstanceUID, uidgen.createUID());
            }
        }
        sr.putUI(Tags.SeriesInstanceUID, uidgen.createUID());
        sr.putUI(Tags.SOPInstanceUID, uidgen.createUID());
        String cuid = sr.getString(Tags.SOPClassUID);
        DcmElement identicalDocumentsSeq = sr.get(Tags.IdenticalDocumentsSeq);
        if (identicalDocumentsSeq != null) {
            for (int i = 0, n = identicalDocumentsSeq.countItems(); i < n; i++)
            {
                Dataset studyItem = identicalDocumentsSeq.getItem(i);
                Dataset seriesItem = studyItem.putSQ(Tags.RefSeriesSeq).addNewItem();
                seriesItem.putUI(Tags.SeriesInstanceUID, uidgen.createUID());
                Dataset sopItem = seriesItem.putSQ(Tags.RefSOPSeq).addNewItem();
                sopItem.putUI(Tags.RefSOPInstanceUID, uidgen.createUID());
                sopItem.putUI(Tags.RefSOPClassUID, cuid);
            }
        }
    }

    private boolean addSUIDs(Dataset sr) {
        String accno = sr.getString(Tags.AccessionNumber);
        if (accno == null) {
            log.warn("Missing Accession Number in ORU - store report in new Study");
            return false;
        }
        Dataset keys = DcmObjectFactory.getInstance().newDataset();
        keys.putSH(Tags.AccessionNumber, accno);
        keys.putUI(Tags.StudyInstanceUID);
        QueryCmd query = null;
        try {
            query = QueryCmd.createStudyQuery(keys, false, true, false, null);
            query.execute();
            if (!query.next()) {
                log.warn("No Study with given Accession Number: " 
                        + accno + " - store report in new Study");
                return false;
            }
            copyStudyInstanceUID(query, sr);
            if (query.next()) {
                DcmElement sq = sr.putSQ(Tags.IdenticalDocumentsSeq);
                do {
                    copyStudyInstanceUID(query, sq.addNewItem());
                } while (query.next());
            }
            return true;
        } catch (SQLException e) {
            log.error("Query DB for Studies with Accession Number " + accno
                    + " failed - store report in new Study", e);
            sr.putSQ(Tags.IdenticalDocumentsSeq);
            return false;
        } finally {
            if (query != null)
                query.close();
        }
    }

    private void copyStudyInstanceUID(QueryCmd query, Dataset sr) 
        throws SQLException {
        sr.putUI(Tags.StudyInstanceUID, 
                query.getDataset().getString(Tags.StudyInstanceUID));
    }

}

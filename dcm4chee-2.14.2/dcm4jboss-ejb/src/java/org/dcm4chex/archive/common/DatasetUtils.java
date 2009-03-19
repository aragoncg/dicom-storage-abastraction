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

package org.dcm4chex.archive.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmEncodeParam;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.data.FileMetaInfo;
import org.dcm4che.dict.Tags;
import org.dcm4cheri.util.StringUtils;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 7175 $ $Date: 2008-09-25 16:13:47 +0200 (Thu, 25 Sep 2008) $
 * @since 04.02.2005
 * 
 */

public class DatasetUtils {

    public static void putRetrieveAET(Dataset ds, String iAETs, String eAET) {
        if (iAETs != null) {
            ds.putAE(Tags.RetrieveAET, StringUtils.split(eAET != null ? iAETs
                    + '\\' + eAET : iAETs, '\\'));
        } else {
            ds.putAE(Tags.RetrieveAET, eAET);
        }
    }

    public static Dataset fromByteArray(byte[] data) {
        return fromByteArray(data, null);
    }

    public static Dataset fromByteArray(byte[] data, Dataset ds) {
        if (data == null)
            return null;
        ByteArrayInputStream bin = new ByteArrayInputStream(data);
        if (ds == null)
            ds = DcmObjectFactory.getInstance().newDataset();
        try {
            ds.readFile(bin, null, -1);
            // reset File Meta Information for Serialisation
            ds.setFileMetaInfo(null);
        } catch (IOException e) {
            throw new IllegalArgumentException("" + e);
        }
        return ds;
    }

    public static byte[] toByteArray(Dataset ds) {
        if (ds == null)
            return null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream(ds
                .calcLength(DcmEncodeParam.EVR_LE));
        try {
            ds.writeDataset(bos, DcmEncodeParam.EVR_LE);
        } catch (IOException e) {
            throw new IllegalArgumentException("" + e);
        }
        return bos.toByteArray();
    }

    public static byte[] toByteArray(Dataset ds, String tsuid) {
        if (ds == null)
            return null;
        if (tsuid == null) {
            return toByteArray(ds);
        }
        FileMetaInfo fmi = DcmObjectFactory.getInstance().newFileMetaInfo();
        fmi.setPreamble(null);
        fmi.putUI(Tags.TransferSyntaxUID, tsuid);
        DcmEncodeParam encodeParam = DcmEncodeParam.valueOf(tsuid);
        ByteArrayOutputStream bos = new ByteArrayOutputStream(fmi.length()
                + ds.calcLength(encodeParam));
        FileMetaInfo prevfmi = ds.getFileMetaInfo();
        ds.setFileMetaInfo(fmi);
        try {
            ds.writeFile(bos, encodeParam);
        } catch (IOException e) {
            throw new IllegalArgumentException("" + e);
        } finally {
            ds.setFileMetaInfo(prevfmi);
        }
        return bos.toByteArray();
    }

    private static SAXParser getSAXParser() {
        try {
            return SAXParserFactory.newInstance().newSAXParser();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Dataset fromXML(InputSource is) throws SAXException,
            IOException {
        Dataset ds = DcmObjectFactory.getInstance().newDataset();
        getSAXParser().parse(is, ds.getSAXHandler2(null));
        return ds;
    }

    public static Dataset fromXML(InputStream is) throws SAXException,
            IOException {
        return fromXML(new InputSource(is));
    }

    public static Dataset fromXML(Reader r) throws SAXException, IOException {
        return fromXML(new InputSource(r));
    }

    public static Dataset fromXML(String s) throws SAXException, IOException {
        return fromXML(new StringReader(s));
    }

    private DatasetUtils() {
    } // no instance
}

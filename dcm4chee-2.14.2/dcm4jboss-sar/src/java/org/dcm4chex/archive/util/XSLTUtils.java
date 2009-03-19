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

package org.dcm4chex.archive.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.dcm4che.data.Dataset;
import org.dcm4che.dict.DictionaryFactory;
import org.dcm4che.dict.TagDictionary;
import org.dcm4che.net.Association;
import org.dcm4che.util.DAFormat;
import org.dcm4che.util.TMFormat;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 6803 $ $Date: 2008-08-18 12:47:27 +0200 (Mon, 18 Aug 2008) $
 * @since Dec 5, 2005
 */
public class XSLTUtils {

    public static final SAXTransformerFactory transformerFactory =
            (SAXTransformerFactory) TransformerFactory.newInstance();

    public static void xslt(Dataset ds, Templates tpl, Association a,
            Dataset out) throws TransformerConfigurationException, IOException {
        xslt(ds, getTransformerHandler(tpl, a), out);
    }

    public static TransformerHandler getTransformerHandler(Templates tpl,
            Association a) throws TransformerConfigurationException {
        TransformerHandler th = transformerFactory.newTransformerHandler(tpl);
        setDateParameters(th);
        if (a != null) {
            setAETParameters(th, a);
        }
        return th;
    }

    public static void xslt(Dataset ds, TransformerHandler th, Dataset out)
            throws IOException {
        th.setResult(new SAXResult(out.getSAXHandler2(null)));
        ds.writeDataset2(th, null, null, 64, null);
    }

    public static void xslt(Dataset ds, Templates tpl, Dataset out)
            throws TransformerConfigurationException, IOException {
        xslt(ds, tpl, null, out);        
    }
    
    public static void setDateParameters(TransformerHandler th) {
        Date now = new Date();
        Transformer t = th.getTransformer();
        t.setParameter("date", new DAFormat().format(now ));
        t.setParameter("time", new TMFormat().format(now));
    }

    public static void setAETParameters(TransformerHandler th, Association a) {
        Date now = new Date();
        Transformer t = th.getTransformer();
        t.setParameter("calling", a.getCallingAET());
        t.setParameter("called", a.getCalledAET());
    }

    public static void writeTo(Dataset ds, File f)
            throws TransformerConfigurationException, IOException {
        TransformerHandler th = transformerFactory.newTransformerHandler();
        th.getTransformer().setOutputProperty(OutputKeys.INDENT, "yes");
        FileOutputStream out = new FileOutputStream(f);
        TagDictionary dict = DictionaryFactory.getInstance().getDefaultTagDictionary();
        try {
            th.setResult(new StreamResult(out));
            ds.writeDataset2(th, dict, null, 64, null);		
        } finally {
            out.close();
        }
    }

}

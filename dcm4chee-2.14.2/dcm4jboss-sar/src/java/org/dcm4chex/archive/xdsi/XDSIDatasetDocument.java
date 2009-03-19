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
package org.dcm4chex.archive.xdsi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;

import javax.activation.DataHandler;

import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.dict.Tags;
import org.dcm4che.dict.UIDs;

/**
 * @author franz.willer@gwi-ag.com
 * @version $Revision: 7193 $ $Date: 2008-09-26 11:18:41 +0200 (Fri, 26 Sep 2008) $
 * @since Feb 15, 2006
 */
public class XDSIDatasetDocument extends XDSIDocument {

    private Dataset ds;
    private DataHandler dh;

    public XDSIDatasetDocument( Dataset ds, String mimeType, String docID ) {
        super(docID, mimeType);
        this.ds = ds;
        dh = new DatasetDataHandler(ds);
    }

    /**
     * @return Returns the docFile.
     */
    public Dataset getDataset() {
        return ds;
    }

    public DataHandler getDataHandler() {
        return dh;
    }
    /**
     * @return Returns the uid.
     */
    public String getUniqueID() {
        return ds.getString(Tags.SOPInstanceUID);
    }
    public String toString() {
        return ds+"|"+getMimeType()+"|"+getDocumentID();
    }


    /**
     * @author franz.willer
     *
     * TODO To change the template for this generated type comment go to
     * Window - Preferences - Java - Code Style - Code Templates
     */
    public class DatasetDataHandler extends DataHandler {

        /**
         * @param ds
         */
        public DatasetDataHandler(Dataset ds) {
            super(ds,"application/dicom");
        }

        public void writeTo( OutputStream out ) throws IOException {
            if ( ds.getFileMetaInfo() == null ) {
                ds.setFileMetaInfo(DcmObjectFactory.getInstance().newFileMetaInfo(ds, UIDs.ExplicitVRLittleEndian));
            }
            ds.writeFile(out,null);
        }

        /** we actually kind of break the JAF model, because we don't have underlying javax.activation.DataSource
         *  objects.  if we did, we wouldn't need to override this method. */
        public InputStream getInputStream() throws IOException {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            writeTo(bos);
            return new ByteArrayInputStream(bos.toByteArray());		    
        }
    }

}

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
 * Portions created by the Initial Developer are Copyright (C) 2005
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

package org.dcm4chex.archive.hsm;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.dict.Tags;
import org.dcm4chex.archive.common.BaseJmsOrder;
import org.dcm4chex.archive.ejb.jdbc.FileInfo;
import org.dcm4chex.archive.ejb.jdbc.RetrieveCmd;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 7626 $ $Date: 2008-10-17 13:54:48 +0200 (Fri, 17 Oct 2008) $
 * @since Nov 9, 2005
 */
public class FileCopyOrder extends BaseJmsOrder {

    private static final long serialVersionUID = 3258409538737550129L;

    protected List<FileInfo> fileInfos = null;

    protected final String dstFsPath;

    protected final String retrieveAETs;

    protected Dataset ian = null;

    public FileCopyOrder(Dataset ian, String dstFsPath, String retrieveAETs) {
        this.ian = ian;
        this.dstFsPath = dstFsPath;
        this.retrieveAETs = retrieveAETs;
    }

    public List<FileInfo> getFileInfos() throws Exception {
        if (fileInfos == null)
            convertFromIAN();

        return fileInfos;
    }

    /**
     * Convert to fileInfos
     * 
     * @throws Exception
     */
    protected void convertFromIAN() throws Exception {
        Dataset refSeriesSeq = ian.getItem(Tags.RefSeriesSeq);
        DcmElement refSOPSeq = refSeriesSeq.get(Tags.RefSOPSeq);
        FileInfo[][] aa = RetrieveCmd.create(refSOPSeq).getFileInfos();
        fileInfos = new ArrayList<FileInfo>(aa.length);
        for (FileInfo[] a : aa) {
            for (FileInfo fi : a) {
                if (isLocalRetrieveAET(fi.fileRetrieveAET)) {
                    fileInfos.add(fi);
                    break;
                }
            }
        }
    }

    private boolean isLocalRetrieveAET(String aet) {
        int pos = retrieveAETs.indexOf(aet);
        int end = pos + aet.length();
        return pos != -1 && (pos == 0 || retrieveAETs.charAt(pos-1) == '\\')
            && (end == retrieveAETs.length() || retrieveAETs.charAt(end) == '\\');
    }

    public final String getDestinationFileSystemPath() {
        return dstFsPath;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(super.toString());
        sb.append("\tRetrieveAETs: ").append(retrieveAETs).append("\n");
        sb.append("\tDestination: ").append(dstFsPath).append("\n");
        if (fileInfos != null) {
            sb.append("\n\tSource files: \n");
            for (Iterator iter = fileInfos.iterator(); iter.hasNext();) {
                FileInfo fi = (FileInfo) iter.next();
                sb.append("\t\t").append(((FileInfo) fi).basedir).append(",")
                        .append(((FileInfo) fi).fileID).append("\n");
            }
        } else if (ian != null) {
            sb.append("\n\tIAN Dataset: \n");
            StringWriter sw = new StringWriter();
            try {
                ian.dumpDataset(sw, null);
                sb.append(sw.toString());
            } catch (Exception e) {
                sb.append("Failed to dump dataset due to: " + e.getMessage());
            }
        }
        sb.append("\n");
        return sb.toString();
    }
}

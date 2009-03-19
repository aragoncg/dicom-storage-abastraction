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

package org.dcm4chex.archive.dcm.qrscp;

import java.sql.SQLException;

import javax.security.auth.Subject;

import org.dcm4che.data.Command;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.dict.Status;
import org.dcm4che.dict.Tags;
import org.dcm4che.net.ActiveAssociation;
import org.dcm4che.net.Association;
import org.dcm4che.net.DcmServiceException;
import org.dcm4che.net.Dimse;
import org.dcm4che.net.DimseListener;
import org.dcm4chex.archive.ejb.jdbc.QueryCmd;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 7902 $ $Date: 2006-09-04 14:27:57 +0200 (Mon, 04 Sep
 *          2006) $
 * @since Jan 26, 2006
 */
class VMFFindScp extends FindScp {

    public VMFFindScp(QueryRetrieveScpService service) {
        super(service, true);
    }

    protected MultiDimseRsp newMultiCFindRsp(Dataset rqData,
            boolean hideWithoutIssuerOfPID, Subject subject)
            throws SQLException {
        if (!"IMAGE".equals(rqData.getString(Tags.QueryRetrieveLevel)))
            return super.newMultiCFindRsp(rqData, hideWithoutIssuerOfPID, subject);
        final String studyIUID = rqData.getString(Tags.StudyInstanceUID);
        final String[] seriesIUIDs = rqData.getStrings(Tags.SeriesInstanceUID);
        if (seriesIUIDs == null || seriesIUIDs.length == 0)
            throw new IllegalArgumentException("Missing Series Instance UID");
        return new VMFMultiCFindRsp(studyIUID, seriesIUIDs,
                hideWithoutIssuerOfPID, subject);
    }

    private class VMFMultiCFindRsp implements MultiDimseRsp {
        private int next = 0;

        private boolean canceled = false;

        private final Dataset keys;

        private final String[] seriesIUIDs;

        private final boolean hideWithoutIssuerOfPID;

        private final Subject subject;

        public VMFMultiCFindRsp(String studyIUID, String[] seriesIUIDs,
                boolean hideWithoutIssuerOfPID, Subject subject) {
            keys = DcmObjectFactory.getInstance().newDataset();
            keys.putUI(Tags.StudyInstanceUID, studyIUID);
            this.seriesIUIDs = seriesIUIDs;
            this.hideWithoutIssuerOfPID = hideWithoutIssuerOfPID;
            this.subject = subject;
        }

        public DimseListener getCancelListener() {
            return new DimseListener() {

                public void dimseReceived(Association assoc, Dimse dimse) {
                    canceled = true;
                }
            };
        }

        public Dataset next(ActiveAssociation assoc, Dimse rq, Command rspCmd)
                throws DcmServiceException {
            rspCmd.putUS(Tags.Status, Status.Cancel);
            while (next < seriesIUIDs.length) {
                if (canceled)
                    return null;
                keys.putUI(Tags.SeriesInstanceUID, seriesIUIDs[next++]);
                try {
                    QueryCmd queryCmd = QueryCmd.createInstanceQuery(keys,
                            false, service.isNoMatchForNoValue(),
                            hideWithoutIssuerOfPID, subject);
                    try {
                        queryCmd.execute();
                        if (!queryCmd.next())
                            continue;
                        final Dataset dataset = queryCmd.getDataset();
                        VMFBuilder builder = new VMFBuilder(service, dataset,
                                service.getVMFConfig(dataset
                                        .getString(Tags.SOPClassUID)));
                        // builder.addFrame(dataset);
                        while (queryCmd.next()) {
                            if (canceled)
                                return null;
                            builder.addFrame(queryCmd.getDataset());
                        }
                        if (canceled)
                            return null;
                        rspCmd.putUS(Tags.Status, Status.Pending);
                        return builder.getResult();
                    } finally {
                        queryCmd.close();
                    }
                } catch (Exception e) {
                    throw new DcmServiceException(Status.UnableToProcess, e);
                }
            }
            rspCmd.putUS(Tags.Status, Status.Success);
            return null;
        }

        public void release() {
            // TODO Auto-generated method stub

        }

    }
}

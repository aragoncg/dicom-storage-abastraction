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

package org.dcm4chex.archive.dcm.mwlscp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.transform.Templates;

import org.dcm4che.data.Command;
import org.dcm4che.data.Dataset;
import org.dcm4che.dict.Status;
import org.dcm4che.dict.Tags;
import org.dcm4che.net.ActiveAssociation;
import org.dcm4che.net.Association;
import org.dcm4che.net.DcmServiceBase;
import org.dcm4che.net.DcmServiceException;
import org.dcm4che.net.Dimse;
import org.dcm4che.net.DimseListener;
import org.jboss.logging.Logger;

/**
 * @author Gunter.Zeilinger@tiani.com
 * @version $Revision: 6804 $ $Date: 2008-08-18 13:54:19 +0200 (Mon, 18 Aug 2008) $
 */
public class MWLFindScp extends DcmServiceBase {
    private static final String QUERY_XSL = "mwl-cfindrq.xsl";
    private static final String RESULT_XSL = "mwl-cfindrsp.xsl";
    private static final String QUERY_XML = "-mwl-cfindrq.xml";
    private static final String RESULT_XML = "-mwl-cfindrsp.xml";

    private final MWLFindScpService service;
    private final Logger log;

    public MWLFindScp(MWLFindScpService service) {
        this.service = service;
        this.log = service.getLog();
    }

    protected MultiDimseRsp doCFind(ActiveAssociation assoc, Dimse rq,
            Command rspCmd) throws IOException, DcmServiceException {
        Association a = assoc.getAssociation();
        Dataset rqData = rq.getDataset();
        log.debug("Identifier:\n");
        log.debug(rqData);
        service.logDIMSE(a, QUERY_XML, rqData);
        service.logDicomQuery(a, rq.getCommand().getAffectedSOPClassUID(),
                rqData);
        Dataset coerce = service.getCoercionAttributesFor(a, QUERY_XSL, rqData);
        if (coerce != null) {
            service.coerceAttributes(rqData, coerce);
        }

        List l = new ArrayList();
        boolean forceLocal = !(service.isUseProxy() && service
                .checkMWLScuConfig());
        try {
            int pendingStatus = service.findMWLEntries(rqData, l, forceLocal);
            return new MultiCFindRsp(l, pendingStatus);
        } catch (Exception e) {
            log.error("Forwarding request to proxy failed!", e);
            throw new DcmServiceException(Status.ProcessingFailure, e);
        }
    }

    private class MultiCFindRsp implements MultiDimseRsp {
        private final Iterator iterResults;
        private boolean canceled = false;
        private final int pendingStatus;
        private int count = 0;
        private Templates coerceTpl;

        public MultiCFindRsp(List results, int pendingStatus) {
            iterResults = results.iterator();
            this.pendingStatus = service.isCheckMatchingKeySupported() ? pendingStatus
                    : 0xff00;
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
            if (canceled) {
                rspCmd.putUS(Tags.Status, Status.Cancel);
                return null;
            }
            try {
                if (!iterResults.hasNext()) {
                    rspCmd.putUS(Tags.Status, Status.Success);
                    return null;
                }
                Association a = assoc.getAssociation();
                rspCmd.putUS(Tags.Status, pendingStatus);
                Dataset rspData = (Dataset) iterResults.next();
                log.debug("Identifier:\n");
                log.debug(rspData);
                service.logDIMSE(a, RESULT_XML, rspData);
                if (count++ == 0) {
                    coerceTpl = service.getCoercionTemplates(a, RESULT_XSL);
                }
                Dataset coerce = service.getCoercionAttributesFor(a,
                        RESULT_XSL, rspData, coerceTpl);
                if (coerce != null) {
                    service.coerceAttributes(rspData, coerce);
                }
                return rspData;
            } catch (Exception e) {
                log.error("Process MWL C-FIND failed:", e);
                throw new DcmServiceException(Status.ProcessingFailure, e);
            }
        }

        public void release() {
        }
    }
}

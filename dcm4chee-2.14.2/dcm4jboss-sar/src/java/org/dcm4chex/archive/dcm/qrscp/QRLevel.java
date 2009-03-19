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
 * Agfa HealthCare.
 * Portions created by the Initial Developer are Copyright (C) 2006-2008
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * See listed authors below.
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

import org.dcm4che.data.Dataset;
import org.dcm4che.dict.Status;
import org.dcm4che.dict.Tags;
import org.dcm4che.net.DcmServiceException;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision$ $Date$
 * @since May 6, 2008
 */
enum QRLevel {
    PATIENT, STUDY, SERIES, IMAGE;

    private static final int[] UID_TAGS = {
        Tags.PatientID,
        Tags.StudyInstanceUID,
        Tags.SeriesInstanceUID,
        Tags.SOPInstanceUID
    };

    void checkSOPClass(String cuid, String studyRoot, String patientStudyOnly)
            throws DcmServiceException {
        if (this == PATIENT) {
            if (cuid.equals(studyRoot)) {
                throw new DcmServiceException(
                        Status.IdentifierDoesNotMatchSOPClass,
                        "Cannot use Query Retrieve Level PATIENT with Study Root IM");
            }
        } else if (this != STUDY) {
            if (cuid.equals(patientStudyOnly)) {
                throw new DcmServiceException(
                        Status.IdentifierDoesNotMatchSOPClass,
                        "Cannot use Query Retrieve Level " + this
                                + " with Patient Study Only IM");
            }
        }
    }

    void checkRetrieveRQ(Dataset rqData) throws DcmServiceException {
        for (int level = 0, levelOffset = -ordinal(); level < UID_TAGS.length;
                level++, levelOffset++) {
            int uidTag = UID_TAGS[level];
            String[] uids =  rqData.getStrings(uidTag);
            if (levelOffset > 0) {
                if (uids != null) {
                    throw new DcmServiceException(
                            Status.IdentifierDoesNotMatchSOPClass,
                            "Illegal Unique Key Attribute "
                            + Tags.toString(uidTag) + " in " + this
                            + " Level Retrieve RQ");
                }
            } else {
                if (levelOffset == 0) {
                    if (uids == null || uids.length == 0) {
                        throw new DcmServiceException(
                                Status.IdentifierDoesNotMatchSOPClass,
                                "Missing Unique Key Attribute "
                                + Tags.toString(uidTag) + " in " + this
                                + " Level Retrieve RQ");
                    }
                }
                if (levelOffset < 0 || level == 0) {
                    if (uids != null && uids.length > 1) {
                        throw new DcmServiceException(
                                Status.IdentifierDoesNotMatchSOPClass,
                                "Illegal List of UIDs in Unique Key Attribute "
                                + Tags.toString(uidTag) + " in " + this
                                + " Level Retrieve RQ");
                    }
                }
            }
        }
    }

    static QRLevel toQRLevel(Dataset rqData) throws DcmServiceException {
        String qrLevel = rqData.getString(Tags.QueryRetrieveLevel);
        try {
            return QRLevel.valueOf(qrLevel);
        } catch (NullPointerException e) {
            throw new DcmServiceException(
                    Status.IdentifierDoesNotMatchSOPClass,
                    "Missing Query Retrieve Level");
        } catch (IllegalArgumentException e) {
            throw new DcmServiceException(
                    Status.IdentifierDoesNotMatchSOPClass,
                    "Invalid Retrieve Level " + qrLevel);
        }
    }
}

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

package org.dcm4chex.archive.dcm.movescu;

import java.io.Serializable;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 2994 $ $Date: 2006-11-30 16:46:51 +0100 (Thu, 30 Nov 2006) $
 * @since 26.08.2004
 *
 */
public final class MoveOrder implements Serializable {

	private static final long serialVersionUID = 3617856386927702068L;

    private String retrieveAET;

    private String moveDestination;

    private String patientId;

    private String[] studyIuids;

    private String[] seriesIuids;

    private String[] sopIuids;

    private int priority;

    private int failureCount;

    public MoveOrder(String retrieveAET, String moveDestination, int priority,
            String patientId, String[] studyIuids, String[] seriesIuids,
            String[] sopIuids) {
        this.priority = priority;
        this.retrieveAET = retrieveAET;
        this.moveDestination = moveDestination;
        this.patientId = patientId;
        this.studyIuids = studyIuids;
        this.seriesIuids = seriesIuids;
        this.sopIuids = sopIuids;
    }

    public MoveOrder(String retrieveAET, String moveDestination, int priority,
			String patientId, String studyIuid, String seriesIuid,
			String[] sopIuids) {
		this(retrieveAET, moveDestination, priority, patientId,
				studyIuid != null ? new String[] { studyIuid } : null,
				seriesIuid != null ? new String[] { seriesIuid } : null,
				sopIuids);
	}
	
    public MoveOrder(String retrieveAET, String moveDestination, int priority,
			String patientId, String studyIuid, String[] seriesIuids) {
		this(retrieveAET, moveDestination, priority, patientId,
				studyIuid != null ? new String[] { studyIuid } : null,
				seriesIuids, null);
	}

    public MoveOrder(String retrieveAET, String moveDestination, int priority,
			String patientId, String studyIuid) {
		this(retrieveAET, moveDestination, priority, patientId,
				studyIuid, null, null);
	}
	
	
    public MoveOrder(String retrieveAET, String moveDestination, int priority,
			String patientId, String[] studyIuids) {
		this(retrieveAET, moveDestination, priority, patientId, studyIuids, null, null);
	}
	
    public String toString() {
        StringBuffer sb = new StringBuffer("MoveOrder[");
        if (sopIuids != null) {
            sb.append(sopIuids.length).append(" Instances, dest=");
        } else if (seriesIuids != null) {
            sb.append(seriesIuids.length).append(" Series, dest=");
        } else if (studyIuids != null) {
            sb.append(studyIuids.length).append(" Studies, dest=");
        } else {
            sb.append("1 Patient, dest=");
        }
        sb.append(moveDestination);
        sb.append(", qrscp=").append(retrieveAET);
        sb.append(", priority=").append(priority);
        sb.append(", failureCount=").append(failureCount);
        sb.append("]");
        return sb.toString();
    }

    public final String getQueryRetrieveLevel() {
        return sopIuids != null ? "IMAGE" : seriesIuids != null ? "SERIES"
                : studyIuids != null ? "STUDY" : "PATIENT";
    }

    public final int getPriority() {
        return priority;
    }

    public final void setPriority(int priority) {
        this.priority = priority;
    }

    public final int getFailureCount() {
        return failureCount;
    }

    public final void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }

    public final String getMoveDestination() {
        return moveDestination;
    }

    public final void setMoveDestination(String moveDestination) {
        this.moveDestination = moveDestination;
    }

    public final String getPatientId() {
        return patientId;
    }

    public final void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public final String getRetrieveAET() {
        return retrieveAET;
    }

    public final void setRetrieveAET(String retrieveAET) {
        this.retrieveAET = retrieveAET;
    }

    public final String[] getSeriesIuids() {
        return seriesIuids;
    }

    public final void setSeriesIuids(String[] seriesIuids) {
        this.seriesIuids = seriesIuids;
    }

    public final String[] getSopIuids() {
        return sopIuids;
    }

    public final void setSopIuids(String[] sopIuids) {
        this.sopIuids = sopIuids;
    }

    public final String[] getStudyIuids() {
        return studyIuids;
    }

    public final void setStudyIuids(String[] studyIuids) {
        this.studyIuids = studyIuids;
    }

}
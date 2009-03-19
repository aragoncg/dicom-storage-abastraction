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

package org.dcm4chex.archive.dcm.stymgt;

import java.io.Serializable;

import org.dcm4che.data.Command;
import org.dcm4che.data.Dataset;

public class StudyMgtOrder implements Serializable {
	
	private static final long serialVersionUID = 3258417226779603505L;

	private final String callingAET;

	private final String calledAET;
	
	private final int cmdField;
	
	private final int actionTypeID;

	private final String iuid;

	private final Dataset ds;

	private int failureCount;

	private Exception exception;

	public StudyMgtOrder(String callingAET, String calledAET,
			int cmdField, int actionID, String iuid, Dataset dataset){
		this.callingAET = callingAET;
		this.calledAET = calledAET;
		this.cmdField = cmdField;
		this.actionTypeID = actionID;
		this.iuid = iuid;
		this.ds = dataset;
	}

	public final String getCalledAET() {
		return calledAET;
	}

	public final String getCallingAET() {
		return callingAET;
	}
	
	public final int getActionTypeID() {
		return actionTypeID;
	}

	public final int getCommandField() {
		return cmdField;
	}

	public final String getSOPInstanceUID() {
		return iuid;
	}
	
	public final Dataset getDataset() {
		return ds;
	}
	
    public final int getFailureCount() {
        return failureCount;
    }

    public final void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }

	public Exception getException() {
		return exception;
	}

	public void setException(Exception exception) {
		this.exception = exception;
	}

    public String toString() {
        return "StudyMgtOrder[" + cmdFieldAsString()
        		+ ", iuid=" + iuid
        		+ ", failureCount=" + failureCount 
                + ", exception=" + exception
                + "]";
    }

	private String cmdFieldAsString() {
		return commandAsString(cmdField, actionTypeID);		
	}
	
	public static String commandAsString(int cmdField, int actionTypeID) {
	      switch (cmdField) {
	         case Command.N_SET_RQ:
	            return "N_SET_RQ";
	         case Command.N_ACTION_RQ:
	            return "N_ACTION_RQ(" + actionTypeID + ")";
	         case Command.N_CREATE_RQ:
	            return "N_CREATE_RQ";
	         case Command.N_DELETE_RQ:
	            return "N_DELETE_RQ";
	      }
		  return Integer.toHexString(cmdField).toUpperCase();
	}
}

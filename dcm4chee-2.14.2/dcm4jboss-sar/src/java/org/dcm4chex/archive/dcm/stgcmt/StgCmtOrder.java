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

package org.dcm4chex.archive.dcm.stgcmt;

import java.io.Serializable;

import org.dcm4che.data.Dataset;

/**
 * @author <a href="mailto:gunter@tiani.com">Gunter Zeilinger</a>
 * @version $Revision: 2010 $ $Date: 2005-10-06 21:55:27 +0200 (Thu, 06 Oct 2005) $
 * @since Jan 5, 2005
 */
class StgCmtOrder implements Serializable {

    private static final long serialVersionUID = 3256437014860936248L;

	private final String callingAET;

    private final String calledAET;

    private final Dataset actionInfo;

    private final boolean scpRole;

    private int failureCount;

    public StgCmtOrder(String callingAET, String calledAET, Dataset actionInfo,
            boolean scpRole) {
        this.callingAET = callingAET;
        this.calledAET = calledAET;
        this.actionInfo = actionInfo;
        this.scpRole = scpRole;
    }

    public final int getFailureCount() {
        return failureCount;
    }

    public final void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }

    public final Dataset getActionInfo() {
        return actionInfo;
    }
    
    public final String getCalledAET() {
        return calledAET;
    }
    
    public final String getCallingAET() {
        return callingAET;
    }
    
    public final boolean isScpRole() {
        return scpRole;
    }
    
    public String toString() {
        return "StgCmtOrder[calling=" + callingAET + ", called=" + calledAET
        	+ ", role=" + (scpRole ? "SCP, failures=" : "SCU, failures=")
        	+ failureCount + "]";
    }
}

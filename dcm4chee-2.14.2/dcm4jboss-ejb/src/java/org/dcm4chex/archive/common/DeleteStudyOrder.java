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

package org.dcm4chex.archive.common;

import java.io.Serializable;

import org.dcm4chex.archive.common.BaseJmsOrder;

/**
 * JMS order for purging a study
 * 
 * @author fang.yang@agfa.com
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision$ $Date$
 * @since Jun 1, 2006
 * 
 */
public class DeleteStudyOrder extends BaseJmsOrder implements Serializable {

    private static final long serialVersionUID = 2395940827585137279L;

    private final long sofPk;
    private final long studyPk;
    private final long fsPk;
    private final long accessTime;

    public DeleteStudyOrder(long sofPk, long studyPk, long fsPk,
            long accessTime) {
        this.sofPk = sofPk;
        this.studyPk = studyPk;
        this.fsPk = fsPk;
        this.accessTime = accessTime;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(super.toString());
        sb.append("\tStudyOnFS PK: ").append(sofPk).append("\n");
        sb.append("\tStudy PK: ").append(studyPk).append("\n");
        sb.append("\tFileSystem PK: ").append(fsPk).append("\n");
        return sb.toString();
    }

    public long getSoFsPk() {
        return sofPk;
    }

    public long getFsPk() {
        return fsPk;
    }

    public long getStudyPk() {
        return studyPk;
    }

    public long getAccessTime() {
        return accessTime;
    }
}

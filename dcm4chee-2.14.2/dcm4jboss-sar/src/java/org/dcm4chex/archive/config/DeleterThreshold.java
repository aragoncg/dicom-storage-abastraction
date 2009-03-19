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
 * Portions created by the Initial Developer are Copyright (C) 2006
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
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


package org.dcm4chex.archive.config;

import org.dcm4chex.archive.util.FileUtils;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 3048 $ $Date: 2007-01-05 19:21:04 +0100 (Fri, 05 Jan 2007) $
 * @since Jun 6, 2006
 */
public abstract class DeleterThreshold implements Comparable {

    protected int startHour;
    private DeleterThreshold(int startHour) {
        if (startHour < 0 || startHour > 23) {
            throw new IllegalArgumentException();
        }
        this.startHour = startHour;
    }

    public final int getStartHour() {
        return startHour;
    }
    
    public int compareTo(Object o) {
        return startHour - ((DeleterThreshold) o).startHour;
    }
    
    public abstract long getFreeSize(long expectedDataVolumePerDay);
    
    public static class SizeBased extends DeleterThreshold {
        private long freeSize;
        SizeBased(int startHour, long freeSize) {
            super(startHour);
            if (freeSize <= 0) {
                throw new IllegalArgumentException();
            }
            this.freeSize = freeSize;
        }
        
        public final long getFreeSize() {
            return freeSize;
        }
        
        public String toString() {
            return "" + startHour + ":" +  FileUtils.formatSize(freeSize);
        }

        public long getFreeSize(long expectedDataVolumePerDay) {
            return freeSize;
        }
    }
    
    public static class TimeBased extends DeleterThreshold {
        private int freeHours;
        TimeBased(int startHour, int freeHours) {
            super(startHour);
            if (freeHours <= 0) {
                throw new IllegalArgumentException();
            }
            this.freeHours = freeHours;
        }
        
        public final int getFreeHours() {
            return freeHours;
        }

        public String toString() {
            return "" + startHour + ":" +  freeHours + "h";
        }

        public long getFreeSize(long expectedDataVolumePerDay) {            
            return expectedDataVolumePerDay * freeHours / 24;
        }
    }
}

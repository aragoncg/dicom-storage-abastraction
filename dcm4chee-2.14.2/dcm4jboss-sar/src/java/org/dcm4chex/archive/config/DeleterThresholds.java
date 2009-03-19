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

import java.util.Calendar;
import java.util.StringTokenizer;

import org.dcm4chex.archive.util.FileUtils;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 2518 $ $Date: 2006-06-07 18:29:43 +0200 (Wed, 07 Jun 2006) $
 * @since Jun 6, 2006
 */
public class DeleterThresholds {
    private final DeleterThreshold[] thresholds;
    public DeleterThresholds(String spec, boolean timeBased) {
        StringTokenizer stk = new StringTokenizer(spec, ":;, \t\r\n");
        int n = stk.countTokens();
        if (n == 0 || (n & 1) != 0) {
            throw new IllegalArgumentException(spec);
        }
        thresholds = new DeleterThreshold[n/2];
        int hour;
        String sizeOrHours;
        int last;
        try {
            for (int i = 0; i < thresholds.length; i++) {
                hour = Integer.parseInt(stk.nextToken());
                sizeOrHours = stk.nextToken();
                last = sizeOrHours.length() - 1;
                if (sizeOrHours.charAt(last) == 'h' ) {
                    if (!timeBased) {
                        throw new IllegalArgumentException();
                    }
                    thresholds[i] = (DeleterThreshold) new DeleterThreshold.TimeBased(
                                hour, Integer.parseInt(sizeOrHours.substring(0, last)));
                } else {
                    thresholds[i] = (DeleterThreshold) new DeleterThreshold.SizeBased(
                                hour, FileUtils.parseSize(sizeOrHours, 0));
                }
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(spec);
        }
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < thresholds.length; i++) {
            sb.append(thresholds[i]).append(';');
        }
        return sb.substring(0, sb.length()-1);
    }
    
    public DeleterThreshold getDeleterThreshold(Calendar now) {
        if (thresholds.length == 1) {
            return thresholds[0];
        }
        int hourOfDay = now.get(Calendar.HOUR_OF_DAY);
        if (hourOfDay  > thresholds[0].getStartHour()) {            
            for (int i = 1; i < thresholds.length; i++) {
                if (hourOfDay < thresholds[i].getStartHour()) {
                    return thresholds[i-1];
                }
            }
        }
        return thresholds[thresholds.length-1];
    }
}

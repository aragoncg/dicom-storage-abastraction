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

package org.dcm4chex.archive.config;

import java.util.StringTokenizer;

/**
 * 
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 2010 $ $Date: 2005-10-06 21:55:27 +0200 (Thu, 06 Oct 2005) $
 * @since 17.12.2003
 */
public final class RetryIntervalls {

    private static final long MS_PER_MIN = 60000L;

    private static final long MS_PER_HOUR = 60 * MS_PER_MIN;

    private static final long MS_PER_DAY = 24 * MS_PER_HOUR;

    private static final long MS_PER_WEEK = 7 * MS_PER_DAY;
    
    private final int[] counts;

    private final long[] intervalls;

    public RetryIntervalls() {
        counts = new int[0];
        intervalls = new long[0];
    }

    public RetryIntervalls(String text) {
        try {
            StringTokenizer stk = new StringTokenizer(text, ", \t\n\r");
            counts = new int[stk.countTokens()];
            intervalls = new long[counts.length];
            for (int i = 0; i < counts.length; i++) {
                init(i, stk.nextToken());
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(text);
        }
    }

    private void init(int i, String item) {
        final int x = item.indexOf('x');
        intervalls[i] = parseInterval(x != -1 ? item.substring(x + 1)
                : item);
        counts[i] = x != -1 ? Math.max(1, Integer
                .parseInt(item.substring(0, x))) : 1;
    }

    public String toString() {
        if (counts.length == 0) { return ""; }
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < counts.length; i++) {
            sb.append(counts[i]).append('x');
            formatInterval(intervalls[i], sb).append(',');
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }
    
    public static String formatIntervalZeroAsNever(long ms) {
        return ms == 0L ? "NEVER" : formatInterval(ms);
    }

    public static String formatInterval(long ms) {
        return formatInterval(ms, new StringBuffer()).toString();
    }

    private static StringBuffer formatInterval(long ms, StringBuffer sb) {
        if (ms % MS_PER_WEEK == 0)
            sb.append(ms / MS_PER_WEEK).append("w");
        else if (ms % MS_PER_DAY == 0)
            sb.append(ms / MS_PER_DAY).append("d");
        else if (ms % MS_PER_HOUR == 0)
            sb.append(ms / MS_PER_HOUR).append("h");
        else if (ms % MS_PER_MIN == 0)
            sb.append(ms / MS_PER_MIN).append("m");
        else
            sb.append(ms / 1000).append("s");
        return sb;
    }

    public static long parseIntervalOrNever(String text) {
        return ("NEVER".equalsIgnoreCase(text)) ? 0L
                : parseInterval(text);
    }
    
    public static long parseInterval(String text) {
        int len = text.length();
        long ms = Long.parseLong(text.substring(0, len - 1));
        switch (text.charAt(len - 1)) {
        case 'w':
            ms *= 7;
        case 'd':
            ms *= 24;
        case 'h':
            ms *= 60;
        case 'm':
            ms *= 60;
        case 's':
            ms *= 1000;
            break;
            default:
                throw new IllegalArgumentException("interval: " + text);
        }
        return ms;
    }

    public long getIntervall(int failureCount) {
        int countDown = failureCount;
        for (int i = 0; i < counts.length; ++i) {
            if ((countDown -= counts[i]) <= 0) { return intervalls[i]; }
        }
        return -1L;
    }

}
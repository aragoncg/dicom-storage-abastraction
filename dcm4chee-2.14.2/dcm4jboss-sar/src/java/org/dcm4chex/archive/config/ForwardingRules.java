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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import org.dcm4cheri.util.StringUtils;

/**
 * @author gunter.zeilinter@tiani.com
 * @version $Revision: 3300 $ $Date: 2007-04-30 14:37:00 +0200 (Mon, 30 Apr 2007) $
 * @since 15.05.2004
 *
 */
public class ForwardingRules {

    static final String NONE = "NONE";

    static final String[] EMPTY = {};

    private final ArrayList list = new ArrayList();

    public static final class Entry {

        final Condition condition;

        final String[] forwardAETs;

        Entry(Condition condition, String[] forwardAETs) {
            this.condition = condition;
            this.forwardAETs = forwardAETs;
        }
    }

    public ForwardingRules(String s) {
        StringTokenizer stk = new StringTokenizer(s, "\r\n;");
        while (stk.hasMoreTokens()) {
            String tk = stk.nextToken().trim();
            if (tk.length() == 0) continue;
            try {
                int endCond = tk.indexOf(']') + 1;
                Condition cond = new Condition(tk.substring(0, endCond));
                String second = tk.substring(endCond);
                String[] aets = (second.length() == 0 || NONE
                        .equalsIgnoreCase(second)) ? EMPTY : StringUtils.split(
                        second, ',');
                for (int i = 0; i < aets.length; i++) {
                    checkAET(aets[i]);
                }
                list.add(new Entry(cond, aets));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(tk);
            }
        }
    }

    private static void checkAET(String s) {
        int delim = s.lastIndexOf('!');
        if (delim == -1) return;
        int hypen = s.lastIndexOf('-');
        int start = Integer.parseInt(s.substring(delim+1, hypen));
        int end = Integer.parseInt(s.substring(hypen+1));
        if (start < 0 || end > 24)
            throw new IllegalArgumentException();
    }

    public static String toAET(String s) {
        int delim = s.lastIndexOf('!');
        return delim == -1 ? s : s.substring(0,delim);
    }

    public static long toScheduledTime(String s) {
        int delim = s.lastIndexOf('!');
        return (delim == -1) ? 0L : afterBusinessHours(
                Calendar.getInstance(), s.substring(delim+1));
    }

    public static long afterBusinessHours(Calendar cal, String businessHours) {
        int hypen = businessHours.lastIndexOf('-');
        int notAfterHour = Integer.parseInt(businessHours.substring(0, hypen));
        int notBeforeHour = Integer.parseInt(businessHours.substring(hypen+1));
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        boolean sameDay = notAfterHour <= notBeforeHour;
        boolean delay = sameDay 
            ? hour >= notAfterHour && hour < notBeforeHour
            : hour >= notAfterHour || hour < notBeforeHour;
        if (!delay)
            return 0L;
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.HOUR_OF_DAY, notBeforeHour);
        if (!sameDay && hour >= notAfterHour)
            cal.add(Calendar.DAY_OF_MONTH, 1);
        return cal.getTimeInMillis();        
    }
    
    public String[] getForwardDestinationsFor(Map param) {
    	ArrayList l = new ArrayList(); 
        for (Iterator it = list.iterator(); it.hasNext();) {
            Entry e = (Entry) it.next();
            if (e.condition.isTrueFor(param) && e.forwardAETs.length > 0 ) l.addAll( Arrays.asList( e.forwardAETs ) );
        }
        if ( l.isEmpty() )
        	return EMPTY;
        else
        	return (String[]) l.toArray( new String[l.size()]);
    }
    
    public String toString() {
        final String newline = System.getProperty("line.separator", "\n");
        if (list.isEmpty()) return newline;
        StringBuffer sb = new StringBuffer();
        for (Iterator it = list.iterator(); it.hasNext();) {
            Entry e = (Entry) it.next();
            e.condition.toStringBuffer(sb);
            if (e.forwardAETs.length == 0) {
                sb.append(NONE);
            } else {
                for (int i = 0; i < e.forwardAETs.length; ++i)
                    sb.append(e.forwardAETs[i]).append(',');
                sb.setLength(sb.length() - 1);
            }
            sb.append(newline);
        }
        return sb.toString();
    }
}

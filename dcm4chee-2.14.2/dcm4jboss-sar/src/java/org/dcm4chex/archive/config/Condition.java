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

import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

/**
 * @author gunter.zeilinter@tiani.com
 * @version $Revision: 3175 $ $Date: 2007-03-05 18:51:38 +0100 (Mon, 05 Mar 2007) $
 * @since 13.06.2004
 *
 */
public class Condition {

	private static final int EXPECT_KEY = 0;

    private static final int EXPECT_EQUAL = 1;

    private static final int EXPECT_VALUE = 2;

    private static final int EXPECT_DELIM = 3;

    private static final String DELIM = "=|,]";

    private static boolean isDelim(String s) {
        return s.length() == 1 && DELIM.indexOf(s.charAt(0)) != -1;
    }

	private LinkedHashMap map = new LinkedHashMap();

    public Condition(String spec) {
        if (!spec.startsWith("[") || !spec.endsWith("]") || spec.length() == 2)
                return;
        StringTokenizer stk = new StringTokenizer(spec.substring(1), DELIM,
                true);
        String tk;
        String key = null;
        int state = EXPECT_KEY;
        for (;;) {
            try {
                tk = stk.nextToken();
            } catch (NoSuchElementException e) {
                throw new IllegalArgumentException(spec);
            }
            switch (state) {
            case EXPECT_KEY:
                if (isDelim(tk)) throw new IllegalArgumentException(spec);
                key = tk;
                state = EXPECT_EQUAL;
                break;
            case EXPECT_EQUAL:
                if (!"=".equals(tk)) throw new IllegalArgumentException(spec);
                state = EXPECT_VALUE;
                break;
            case EXPECT_VALUE:
                if (isDelim(tk)) throw new IllegalArgumentException(spec);
                put(key, tk);
                state = EXPECT_DELIM;
                break;
            case EXPECT_DELIM:
                if ("|".equals(tk))
                    state = EXPECT_VALUE;
                else if (",".equals(tk))
                    state = EXPECT_KEY;
                else if ("]".equals(tk)) return;
            }
        }
    }

    private void put(String key, String val) {
        LinkedHashSet set = (LinkedHashSet) map.get(key);
        if (set == null) {
            set = new LinkedHashSet();
            map.put(key, set);
        }
        set.add(val);
    }

    public boolean isAlwaysTrue() {
        return map.isEmpty();
    }

    public boolean isTrueFor(Map param) {
        Iterator it = param.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            String key = (String) entry.getKey();
            String[] val = (String[]) entry.getValue();
            LinkedHashSet set = (LinkedHashSet) map.get(key);
            if (set != null && !containsAny(set, val)) return false;
            LinkedHashSet antiset = (LinkedHashSet) map.get(key + '!');
            if (antiset != null && containsAny(antiset, val)) return false;
        }
        return true;
    }

    private boolean containsAny(LinkedHashSet set, String[] val) {
        if (val != null)
	        for (int i = 0; i < val.length; i++)
	            if (set.contains(val[i])) return true;
        return false;
    }

    public String toString() {
        return toStringBuffer(new StringBuffer()).toString();
    }

    public StringBuffer toStringBuffer(StringBuffer sb) {
        if (map.isEmpty()) return sb;
        sb.append('[');
        Iterator it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            sb.append(entry.getKey()).append('=');
            LinkedHashSet set = (LinkedHashSet) entry.getValue();
            Iterator values = set.iterator();
            while (values.hasNext())
                sb.append(values.next()).append('|');
            sb.setCharAt(sb.length() - 1, ',');
        }
        sb.setCharAt(sb.length() - 1, ']');
        return sb;
    }
}

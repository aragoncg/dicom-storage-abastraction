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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import org.dcm4che.data.Dataset;
import org.dcm4che.dict.Tags;
import org.dcm4che.dict.UIDs;
import org.dcm4che.net.Association;

/**
 * @author gunter.zeilinter@tiani.com
 * @version $Revision: 2234 $ $Date: 2006-01-19 03:04:44 +0100 (Thu, 19 Jan 2006) $
 * @since 11.06.2004
 *
 */
public class CompressionRules {

    static final int NONE = 0;

    static final int J2LL = 1;

    static final int JLSL = 2;

    static final int J2KR = 3;

    static final String[] CODES = { "NONE", "JPLL", "JLSL", "J2KR"};

    static final String[] TSUIDS = { null, UIDs.JPEGLossless,
            UIDs.JPEGLSLossless, UIDs.JPEG2000Lossless,};

    private final ArrayList list = new ArrayList();

    private static final class Entry {

        final Condition condition;

        final int compression;

        Entry(Condition condition, int compression) {
            this.condition = condition;
            this.compression = compression;
        }
    }

    public CompressionRules(String s) {
        StringTokenizer stk = new StringTokenizer(s, "\r\n;");
        while (stk.hasMoreTokens()) {
            String tk = stk.nextToken().trim();
            if (tk.length() == 0) continue;
            try {
                int endCond = tk.indexOf(']') + 1;
                Condition cond = new Condition(tk.substring(0, endCond));
                int compression = Math.max(0, Arrays.asList(CODES).indexOf(
                        tk.substring(endCond)));
                list.add(new Entry(cond, compression));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(tk);
            }
        }
    }

    public String getTransferSyntaxFor(Association assoc, Dataset ds) {
        Map param = new HashMap();
        param.put("calling", new String[]{assoc.getCallingAET()});
		param.put("called", new String[]{assoc.getCalledAET()});
        if (ds != null) {
			putIntoIfNotNull(param, "cuid", ds, Tags.SOPClassUID);
			putIntoIfNotNull(param, "pmi", ds, Tags.PhotometricInterpretation);
			putIntoIfNotNull(param, "imgtype", ds, Tags.ImageType);
        }
        for (Iterator it = list.iterator(); it.hasNext();) {
            Entry e = (Entry) it.next();
            if (e.condition.isTrueFor(param)) return TSUIDS[e.compression];
        }
        return null;
    }

    private void putIntoIfNotNull(Map param, String key, Dataset ds, int tag) {
		String[] val = ds.getStrings(tag);
		if (val != null && val.length != 0) {
			param.put(key, val);
		}
	}

	public String toString() {
	    final String newline = System.getProperty("line.separator", "\n");
        if (list.isEmpty()) return newline;
        StringBuffer sb = new StringBuffer();
        for (Iterator it = list.iterator(); it.hasNext();) {
            Entry e = (Entry) it.next();
            e.condition.toStringBuffer(sb);
            sb.append(CODES[e.compression]);
            sb.append(newline);
        }
        return sb.toString();
    }
}

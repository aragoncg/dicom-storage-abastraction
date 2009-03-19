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
package org.dcm4chex.archive.util;

import org.dcm4cheri.util.StringUtils;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision$ $Date$
 * @since Oct 15, 2008
 */
public class AETs {
    public static String update(String oldAETs, String oldAET,
            String newAET) {
        if (oldAETs != null) {
            String[] aets = StringUtils.split(oldAETs, '\\');
            for (int i = 0; i < aets.length; i++) {
                if (aets[i].equals(oldAET)) {
                    aets[i] = newAET;
                    return StringUtils.toString(aets, '\\');
                }
            }
        }
        return oldAETs;
    }

    public static String common(String aets1, String aets2) {
        if (aets1.equals(aets2))
            return aets1;
        String[] a1 = StringUtils.split(aets1, '\\');
        String[] a2 = StringUtils.split(aets2, '\\');
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < a1.length; i++)
            for (int j = 0; j < a2.length; j++)
                if (a1[i].equals(a2[j]))
                    sb.append(a1[i]).append('\\');
        int l = sb.length();
        if (l == 0)
            return null;
        sb.setLength(l-1);
        return sb.toString();
    }
}

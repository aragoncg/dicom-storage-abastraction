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
 * Agfa-Gevaert Group.
 * Portions created by the Initial Developer are Copyright (C) 2003-2005
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * See @authors listed below.
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

package org.dcm4chex.archive.hl7;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.Element;
import org.regenstrief.xhl7.HL7XMLLiterate;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision: 5631 $ $Date: 2008-01-03 14:13:58 +0100 (Thu, 03 Jan 2008) $
 * @since Jun 20, 2007
 */
class PID {
    
    private final ArrayList pids = new ArrayList();

    public PID(Document msg) {
        Element pid = msg.getRootElement().element("PID");
        if (pid == null)
            throw new IllegalArgumentException("Missing PID Segment");
        List pidfds = pid.elements(HL7XMLLiterate.TAG_FIELD);
        if (pidfds.size() < 3)
        	throw new IllegalArgumentException("Missing PID-3 Field");
        Element pidfd = (Element) pidfds.get(2);
        pids.add(toPID(pidfd));
        List furtherPids = pidfd.elements(HL7XMLLiterate.TAG_REPEAT);
        for (Iterator iter = furtherPids.iterator(); iter.hasNext();) {
            pids.add(toPID((Element) iter.next()));            
        }
    }
    
    public List getPatientIDs() {
        return Collections.unmodifiableList(pids);
    }

    public int countPatientIDs() {
        return pids.size();
    }
    
    static String[] toPID(Element pidfd) {
        List comps = pidfd.elements(HL7XMLLiterate.TAG_COMPONENT);
        if (comps.size() < 3) {
        	throw new IllegalArgumentException("Missing Authority in PID-3");        	
        }
		Element authority = (Element) comps.get(2);
        List authorityUID = authority.elements(HL7XMLLiterate.TAG_SUBCOMPONENT);
        String[] pid = new String[2 + authorityUID.size()];
        pid[0] = pidfd.getText();
        pid[1] = authority.getText();
        for (int i = 2; i < pid.length; i++) {
            pid[i] = ((Element) authorityUID.get(i-2)).getText();
        }
        return pid;
    }

    public static boolean isEmpty(Document msg) {
        Element pid = msg.getRootElement().element("PID");
        if (pid == null)
                return true;
        List pidfds = pid.elements(HL7XMLLiterate.TAG_FIELD);
        if (pidfds.size() < 3)
                return true;
        Element pidfd = (Element) pidfds.get(2);
        String pid3 = pidfd.getText();
        return pid3 == null || pid3.length() == 0;
    }

}

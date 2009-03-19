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

package org.dcm4chex.archive.hl7;

import java.util.List;

import org.dom4j.Document;
import org.dom4j.Element;
import org.regenstrief.xhl7.HL7XMLLiterate;


public class MSH {

    public final String sendingApplication;

    public final String sendingFacility;

    public final String receivingApplication;

    public final String receivingFacility;

    public final String messageType;

    public final String triggerEvent;

	public final String messageControlID;

    public final String processingID;

    public final String versionID;

    public final String characterSet;

    public MSH(Document msg) {
        Element msh = msg.getRootElement().element("MSH");
        if (msh == null)
                throw new IllegalArgumentException("Missing MSH Segment");
        List fields = msh.elements(HL7XMLLiterate.TAG_FIELD);
        this.sendingApplication = toString(fields.get(0));
        this.sendingFacility = toString(fields.get(1));
        this.receivingApplication = toString(fields.get(2));
        this.receivingFacility = toString(fields.get(3));
        Element el = (Element) fields.get(6);
        this.messageType = toString(el);
        this.triggerEvent = toString(el != null ? el
                .element(HL7XMLLiterate.TAG_COMPONENT) : null);
        this.messageControlID = toString(fields.get(7));
        this.processingID = toString(fields.get(8));
        this.versionID = toString(fields.get(9));
        this.characterSet = fields.size() > 15 ? toString(fields.get(15)) : null;
    }

    private static String toString(Object el) {
        return el != null ? ((Element) el).getText() : "";
    }
    
    public String toString() {
    	return messageType + "^" + triggerEvent + "["
    		+  sendingApplication + '@' + sendingFacility
    		+ "->" + receivingApplication + '@' + receivingFacility
    		+ ", pid=" +  processingID + ", vers=" +  versionID
    		+ ", charset=" +  characterSet + "]";
    }
}
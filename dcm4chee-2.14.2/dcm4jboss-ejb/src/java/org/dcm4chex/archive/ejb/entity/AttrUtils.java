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
 * Java(TM), hosted at http://sourceforge.net/projects/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * Agfa Healthcare.
 * Portions created by the Initial Developer are Copyright (C) 2006
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

package org.dcm4chex.archive.ejb.entity;

import java.util.Arrays;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.data.DcmValueException;
import org.dcm4che.data.SpecificCharacterSet;
import org.dcm4che.dict.Tags;
import org.dcm4che.dict.VRs;
import org.dcm4che.net.DcmServiceException;
import org.dcm4chex.archive.ejb.conf.AttributeFilter;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Id: AttrUtils.java 3387 2007-06-15 11:13:57Z gunterze $
 * @since Jul 6, 2006
 */
class AttrUtils {

    private static final int FORBIDDEN_ATTRIBUTE_COERCION = 0xCB00;
    
    public static boolean coerceAttributes(Dataset oldAttrs, Dataset newAttrs,
            Dataset coercedElements, AttributeFilter filter, Logger log)
            throws DcmServiceException {
        boolean coercedIdentity = false;
        for (Iterator it = oldAttrs.iterator(); it.hasNext();) {
            DcmElement refEl = (DcmElement) it.next();
            if (refEl.isEmpty())
                continue;
            final int tag = refEl.tag();
            DcmElement el = newAttrs.get(tag);
            if (!equals(el,
                    newAttrs.getSpecificCharacterSet(),
                    refEl,
                    oldAttrs.getSpecificCharacterSet(),
                    coercedElements, log)) {
                if (filter != null && filter.isCoercionForbidden(tag)) {
                    throw new DcmServiceException(FORBIDDEN_ATTRIBUTE_COERCION,
                            "Storage would require forbidden Coercion of " 
                            + el + " to " + refEl);
                }
                log.warn("Coerce " + el + " to " + refEl);
                if (coercedElements != null) {
                    if (VRs.isLengthField16Bit(refEl.vr())) {
                        coercedElements.putXX(tag, refEl
                                .getByteBuffer());
                    } else {
                        coercedElements.putXX(tag);
                    }
                }
                coercedIdentity = true;
            }
        }
        return coercedIdentity;                
    }

    private static boolean equals(DcmElement el, SpecificCharacterSet cs,
            DcmElement refEl, SpecificCharacterSet refCS,
            Dataset coercedElements, Logger log)
    throws DcmServiceException {
        final int vr = refEl.vr();
        if (el == null || el.vr() != vr) {
            return false;
        }
        if (vr == VRs.OW || vr == VRs.OB || vr == VRs.UN) {
            // no check implemented!
            return true;
        }
        if (vr == VRs.SQ) {
            int n = refEl.countItems();
            if (el.countItems() != n) {
                return false;
            }
            for (int i = 0; i < n; i++) {
                if (coerceAttributes(refEl.getItem(i), el.getItem(i), null,
                        null, log)) {
                    if (coercedElements != null) {
                        coercedElements.putSQ(el.tag());
                    }
                }
            }
        } else {
            try {
                if (vr == VRs.PN 
                        ? !Arrays.equals(refEl.getPersonNames(refCS),
                                el.getPersonNames(cs))
                        : !Arrays.equals(refEl.getStrings(refCS),
                                el.getStrings(cs))) {
                    return false; 
                }
            } catch (DcmValueException e) {
                log.warn("Failure during coercion of " + el, e);
            }
        }
        return true;
    }
    
    public static boolean mergeAttributes(Dataset oldAttrs, Dataset newAttrs,
            Logger log) {
        boolean updateEntity = false;
        for (Iterator it = newAttrs.iterator(); it.hasNext();) {
            DcmElement dsEl = (DcmElement) it.next();
            final int tag = dsEl.tag();
            final int vr = dsEl.vr();
            final int numItems = dsEl.countItems();
            if (Tags.isPrivate(tag) || dsEl.isEmpty())
                continue;            
            DcmElement refEl = oldAttrs.get(tag);
            if (dsEl.hasItems()) {
                // only update empty sequences or with equal number of items
                if (refEl == null || refEl.isEmpty())
                {
                   log.info("Update stored objects with additional element/value from new received object- " + dsEl);
                   refEl = oldAttrs.putSQ(tag);
                   for (int i = 0; i < numItems; i++)
                         refEl.addItem(dsEl.getItem());
                    updateEntity = true;
                } else if (refEl.countItems() == numItems) {
                    for (int i = 0; i < numItems; i++)
                        if (mergeAttributes(refEl.getItem(), dsEl.getItem(), log))
                            updateEntity = true;
                }
            } else if (refEl == null || refEl.isEmpty())
            {
                log.info("Update stored objects with additional element/value from new received object - " + dsEl);
                if (dsEl.hasDataFragments()) {
                      
                    refEl = oldAttrs.putXXsq(tag, vr);
                      for (int i = 0; i < numItems; i++)
                          refEl.addDataFragment(dsEl.getDataFragment(i));
                } else {
                    oldAttrs.putXX(tag, vr, dsEl.getByteBuffer());
                }
                updateEntity = true;
            }
        }
        return updateEntity;
    }

    public static boolean updateAttributes(Dataset oldAttrs, Dataset newAttrs,
            Logger log) {
        boolean updateEntity = false;
        for (Iterator it = newAttrs.iterator(); it.hasNext();) {
            DcmElement dsEl = (DcmElement) it.next();
            final int tag = dsEl.tag();
            DcmElement refEl = oldAttrs.get(tag);
            if (dsEl.equals(refEl)) {
                continue;
            }
            if (refEl == null || refEl.isEmpty()) {
                log.info("Add " + dsEl);
            } else {
                log.info("Change " + refEl + " to " + dsEl);                
            }
            final int vr = dsEl.vr();
            final int numItems = dsEl.countItems();
            if (dsEl.isEmpty()) {
                oldAttrs.putXX(tag, vr);
            } else if (dsEl.hasItems()) {
                refEl = oldAttrs.putSQ(tag);
                for (int i = 0; i < numItems; i++) {
                    refEl.addItem(dsEl.getItem());
                }
            } else if (dsEl.hasDataFragments()) {
                refEl = oldAttrs.putXXsq(tag, vr);
                for (int i = 0; i < numItems; i++) {
                    refEl.addDataFragment(dsEl.getDataFragment(i));
                }
            } else {
                oldAttrs.putXX(tag, vr, dsEl.getByteBuffer());
            }
            updateEntity = true;
        }
        return updateEntity;
    }
}

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

package org.dcm4chex.archive.ejb.conf;

import java.util.ArrayList;
import java.util.Arrays;

import javax.xml.parsers.SAXParserFactory;

import org.dcm4che.dict.Tags;
import org.dcm4che.dict.VRs;
import org.dcm4chex.archive.exceptions.ConfigurationException;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Id: AttributeFilterLoader.java 5458 2007-11-19 11:47:35Z gunterze $
 * @since Jul 4, 2006
 */
class AttributeFilterLoader extends DefaultHandler {
    private static final int[] INST_SUPPL_TAGS = { Tags.RetrieveAET,
            Tags.InstanceAvailability, Tags.StorageMediaFileSetID,
            Tags.StorageMediaFileSetUID };
    private final ArrayList tagList = new ArrayList();
    private final ArrayList vrList = new ArrayList();
    private final ArrayList noCoerceList = new ArrayList();
    private final ArrayList fieldTagList = new ArrayList();
    private final ArrayList fieldList = new ArrayList();
    // private String cuid;
    private AttributeFilter filter;

    public static void loadFrom(String url) throws ConfigurationException {
        AttributeFilterLoader h = new AttributeFilterLoader();
        try {
            SAXParserFactory.newInstance().newSAXParser().parse(url, h);
        } catch (Exception e) {
            throw new ConfigurationException(
                    "Failed to load attribute filter from " + url, e);
        }
    }

    public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException {
        if (qName.equals("attr")) {
            String tag = attributes.getValue("tag");
            if (tag != null) {
                tagList.add(tag);
                String field = attributes.getValue("field");
                if (field != null) {
                    fieldTagList.add(tag);
                    fieldList.add(field);
                }
                if ("false".equalsIgnoreCase(attributes.getValue("coerce")))
                    noCoerceList.add(tag);
            } else {
                String vr = attributes.getValue("vr");
                if (vr != null) {
                    vrList.add(vr);
                }
            }
        } else if (qName.equals("instance")) {
            String cuid = attributes.getValue("cuid");
            if (AttributeFilter.instanceFilters.containsKey(cuid)) {
                throw new SAXException(
                        "more than one instance element with cuid=" + cuid);
            }
            AttributeFilter.instanceFilters.put(cuid,
                    filter = makeFilter(attributes));
        } else if (qName.equals("series")) {
            if (AttributeFilter.seriesFilter != null) {
                throw new SAXException("more than one series element");
            }
            AttributeFilter.seriesFilter = filter = makeFilter(attributes);
        } else if (qName.equals("study")) {
            if (AttributeFilter.studyFilter != null) {
                throw new SAXException("more than one study element");
            }
            AttributeFilter.studyFilter = filter = makeFilter(attributes);
        } else if (qName.equals("patient")) {
            if (AttributeFilter.patientFilter != null) {
                throw new SAXException("more than one patient element");
            }
            AttributeFilter.patientFilter = filter = makeFilter(attributes);
        }
    }

    private AttributeFilter makeFilter(Attributes attributes) {
        String strategy = attributes.getValue("update-strategy");
        return new AttributeFilter(attributes.getValue("tsuid"), 
                "true".equalsIgnoreCase(attributes.getValue("exclude")),
                "true".equalsIgnoreCase(attributes.getValue("excludePrivate")),
                strategy.startsWith("overwrite"), strategy.endsWith("merge"));
    }

    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        if (qName.equals("attr")) {
            return;
        }
        boolean inst = qName.equals("instance");
        if (inst || qName.equals("series") || qName.equals("study")
                || qName.equals("patient")) {
            int[] tags = parseInts(tagList);
            int[] vrs = parseVRs(vrList);
            if (inst && filter.isExclude()) {
                if (AttributeFilter.patientFilter == null) {
                    throw new SAXException(
                            "missing patient before instance element");
                }
                if (AttributeFilter.studyFilter == null) {
                    throw new SAXException(
                            "missing study before instance element");
                }
                if (AttributeFilter.seriesFilter == null) {
                    throw new SAXException(
                            "missing series before instance element");
                }
                tags = merge(INST_SUPPL_TAGS, AttributeFilter.patientFilter
                        .getTags(), AttributeFilter.studyFilter.getTags(),
                        AttributeFilter.seriesFilter.getTags(), tags);
                vrs = merge(new int[] {}, AttributeFilter.patientFilter
                        .getVRs(), AttributeFilter.studyFilter.getVRs(),
                        AttributeFilter.seriesFilter.getVRs(), vrs);
            }
            filter.setTags(tags);
            filter.setFieldTags(parseInts(fieldTagList));
            filter.setFields((String[]) fieldList.toArray(new String[] {}));
            filter.setNoCoercion(parseInts(noCoerceList));
            filter.setVRs(vrs);
            tagList.clear();
            fieldTagList.clear();
            fieldList.clear();
            noCoerceList.clear();
            vrList.clear();
            filter = null;
        }
    }

    private int[] merge(int[] a, int[] b, int[] c, int[] d, int[] e) {
        int[] dst = new int[a.length + b.length + c.length + d.length
                + e.length];
        System.arraycopy(a, 0, dst, 0, a.length);
        System.arraycopy(b, 0, dst, a.length, b.length);
        System.arraycopy(c, 0, dst, a.length + b.length, c.length);
        System.arraycopy(d, 0, dst, a.length + b.length + c.length, d.length);
        System.arraycopy(e, 0, dst, a.length + b.length + c.length + d.length,
                e.length);
        Arrays.sort(dst);
        return dst;
    }

    private static int[] parseInts(ArrayList list) {
        int[] array = new int[list.size()];
        for (int i = 0; i < array.length; i++) {
            array[i] = Integer.parseInt((String) list.get(i), 16);
        }
        Arrays.sort(array);
        return array;
    }

    private static int[] parseVRs(ArrayList list) {
        int[] array = new int[list.size()];
        for (int i = 0; i < array.length; i++) {
            array[i] = VRs.valueOf((String) list.get(i));
        }
        return array;
    }

    public void endDocument() throws SAXException {
        if (AttributeFilter.patientFilter == null) {
            throw new SAXException("missing patient element");
        }
        if (AttributeFilter.studyFilter == null) {
            throw new SAXException("missing study element");
        }
        if (AttributeFilter.seriesFilter == null) {
            throw new SAXException("missing series element");
        }
        if (AttributeFilter.instanceFilters.get(null) == null) {
            throw new SAXException("missing instance element");
        }
    }

}

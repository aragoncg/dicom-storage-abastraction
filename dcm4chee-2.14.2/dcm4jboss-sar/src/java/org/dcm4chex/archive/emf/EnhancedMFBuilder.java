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

package org.dcm4chex.archive.emf;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;

import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.data.DcmParser;
import org.dcm4che.data.DcmParserFactory;
import org.dcm4che.data.DcmValueException;
import org.dcm4che.data.FileFormat;
import org.dcm4che.dict.Tags;
import org.dcm4che.dict.UIDs;
import org.dcm4che.util.UIDGenerator;
import org.jboss.logging.Logger;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision$ $Date$
 * @since Nov 27, 2007
 */
class EnhancedMFBuilder {

    private static final int[] FG_SEQ_TAGS = {
        Tags.SharedFunctionalGroupsSeq, Tags.PerFrameFunctionalGroupsSeq };
    private static final Dataset ALTERNATE_SOP_CLASS_INSTANCE;
    static {
        ALTERNATE_SOP_CLASS_INSTANCE =
                DcmObjectFactory.getInstance().newDataset();
        ALTERNATE_SOP_CLASS_INSTANCE.putLO(Tags.CodeValue, "121326");
        ALTERNATE_SOP_CLASS_INSTANCE.putSH(Tags.CodingSchemeDesignator, "DCM");
        ALTERNATE_SOP_CLASS_INSTANCE.putLO(Tags.CodeMeaning,
                "Alternate SOP Class instance");
    }
    private final Logger log;
    private final Dataset filter;
    private final Dataset fgFilters;
    private final Dataset dataset;
    private final Dataset sharedFGs;
    private final int frameTypeTag;
    private final boolean noPixelData;
    private final boolean deflate;
    private final long[] pixelDataOffsets;
    private final int[] pixelDataLengths;
    private File f0;
    private int pixelDataVR;
    private int pixelDataLength;
    private int curFrame = 0;
    private String tsuid;

    private String[] imageType;
    private Date acquisitionDatetime;
    private Date contentDatetime;
    private String instanceNumber;


    public EnhancedMFBuilder(UpgradeToEnhancedMFService service,
            Dataset filter, int frameTypeTag, int numFrames) {
        this.log = service.getLog();
        this.noPixelData = service.isNoPixelData();
        this.deflate = service.isDeflate();
        this.filter = filter;
        this.fgFilters = filter.getItem(Tags.SharedFunctionalGroupsSeq);
        this.dataset = DcmObjectFactory.getInstance().newDataset();
        this.sharedFGs = dataset.putSQ(Tags.SharedFunctionalGroupsSeq).addNewItem();
        this.frameTypeTag = frameTypeTag;
        this.pixelDataOffsets = new long[numFrames];
        this.pixelDataLengths = new int[numFrames];
     }

    public final int getPixelDataVR() {
        return pixelDataVR;
    }

    public int getPixelDataLength() {
        return pixelDataLength == -1 ? -1 
                : pixelDataLength * pixelDataOffsets.length;
    }

    public long getPixelDataOffset(int i) {
        return pixelDataOffsets[i];
    }

    public int getPixelDataLength(int i) {
        return pixelDataLengths[i];
    }

    public void add(File f) throws UpgradeToEnhancedMFException, IOException {
        if (curFrame == pixelDataOffsets.length) {
            throw new IllegalStateException("curFrame: " + curFrame
                    + " == numFrame: " + pixelDataOffsets.length);
        }
        if (log.isDebugEnabled()) {
            log.debug("M-READ " + f);
        }
        Dataset source = DcmObjectFactory.getInstance().newDataset();
        BufferedInputStream bis = new BufferedInputStream(
                new FileInputStream(f));
        DcmParser parser = DcmParserFactory.getInstance().newDcmParser(bis);
        try {
            parser.setDcmHandler(source.getDcmHandler());
            parser.parseDcmFile(FileFormat.DICOM_FILE, Tags.PixelData);
            if (parser.getReadTag() != Tags.PixelData) {
                throw new UpgradeToEnhancedMFException("No Pixel Data in " + f);
            }
            if (curFrame == 0) {
                if (log.isDebugEnabled()) {
                    log.debug("Create new Enhanced MF from " + f);
                }
                tsuid = source.getFileMetaInfo().getTransferSyntaxUID();
                pixelDataVR = parser.getReadVR();
                pixelDataLength = parser.getReadLength();
                init(source);
                f0 = f;
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Merge Functional Groups to Enhanced MF from " + f);
                }
                if (pixelDataVR != parser.getReadVR()) {
                    throw new UpgradeToEnhancedMFException("VR of Pixel Data in "
                            + f + " differs from " + f0);
                }
                if (pixelDataLength != parser.getReadLength()) {
                    throw new UpgradeToEnhancedMFException("Pixel Data Length in "
                            + f + " differs from " + f0);
                }
                if (!noPixelData && !tsuid.equals(source.getFileMetaInfo()
                                                .getTransferSyntaxUID())) {
                    throw new UpgradeToEnhancedMFException("Transfer Syntax of "
                            + f + " differs from " + f0);
                }
                if (!dataset.exclude(FG_SEQ_TAGS).equals(source.subSet(filter))) {
                    throw new UpgradeToEnhancedMFException(
                            "Non Functional Groups elements of "  + f 
                            + " differs from " + f0);
                }
                mergeFunctionalGroups(dataset, source);
            }
            if (pixelDataLength == -1) {
                // skip frame offset table
                parser.parseHeader();
                if (parser.getReadLength() != 0) {
                    throw new UpgradeToEnhancedMFException(
                            "Non-empty Frame Offset table in " + f);
                }
                parser.parseHeader();
            }
            pixelDataOffsets[curFrame] = parser.getStreamPosition();
            pixelDataLengths[curFrame] = parser.getReadLength();
        } finally {
            try { bis.close(); } catch (IOException ignore) {}
        }
        ++curFrame;
    }

    private void init(Dataset source) {
        dataset.putAll(source.subSet(filter));
        for (Iterator it = fgFilters.iterator(); it.hasNext();) {
            DcmElement el = (DcmElement) it.next();
            Dataset fgFilter = el.getItem();
            Dataset fg = source.subSet(fgFilter);
            if (!fg.isEmpty()) {
                sharedFGs.putSQ(el.tag()).addNewItem().putAll(fg);
            }
        }
        sharedFGs.putSQ(frameTypeTag).addNewItem().putCS(Tags.FrameType,
                imageType = source.getStrings(Tags.ImageType));
        Dataset perFrameFGs = dataset.putSQ(
                Tags.PerFrameFunctionalGroupsSeq).addNewItem();
        addReferencedImageFG(perFrameFGs, source);
        addFrameContentFG(perFrameFGs, source);
    }

    private void addReferencedImageFG(Dataset perFrameFGs, Dataset source) {
        DcmElement refImageSeq = perFrameFGs.putSQ(Tags.RefImageSeq);
        Dataset refSource = refImageSeq.addNewItem();
        refSource.putUI(Tags.RefSOPClassUID,
                source.getString(Tags.SOPClassUID));
        refSource.putUI(Tags.RefSOPInstanceUID,
                source.getString(Tags.SOPInstanceUID));
        refSource.putSQ(Tags.PurposeOfReferenceCodeSeq)
                .addItem(ALTERNATE_SOP_CLASS_INSTANCE);
        
        DcmElement srcRefImageSeq = source.get(Tags.RefImageSeq);
        if (srcRefImageSeq != null) {
            for (int i = 0, n = srcRefImageSeq.countItems(); i < n; i++) {
                refImageSeq.addItem(srcRefImageSeq.getItem(i));
            }
        }
    }

    private void addFrameContentFG(Dataset perFrameFGs, Dataset source) {
        Dataset fg = perFrameFGs.putSQ(Tags.FrameContentSeq).addNewItem();
        Date d = source.getDate(Tags.AcquisitionDatetime);
        if (d == null) {
            d = source.getDateTime(Tags.AcquisitionDate, Tags.AcquisitionTime);
        }
        if (d != null) {
            fg.putDT(Tags.FrameAcquisitionDatetime, d);
            if (acquisitionDatetime == null
                    || acquisitionDatetime.compareTo(d) > 0) {
                acquisitionDatetime = d;
            }
        }
        String s;
        if ((s = source.getString(Tags.AcquisitionNumber)) != null) {
            try {
                fg.putUS(Tags.FrameAcquisitionNumber, Integer.parseInt(s));
            } catch (NumberFormatException e) {
                log.info("Ignore non-numeric Acquisition Number: " + s
                        + " of Instance: "
                        + source.getString(Tags.SOPInstanceUID));
            }
        }
        if ((s = source.getString(Tags.ImageComments)) != null) {
            fg.putLT(Tags.FrameComments, s);
        }
        if ((s = source.getString(Tags.InstanceCreationDate)) != null) {
            fg.putDA(Tags.InstanceCreationDate, s);
        }
        if ((s = source.getString(Tags.InstanceCreationTime)) != null) {
            fg.putTM(Tags.InstanceCreationTime, s);
        }
        if ((s = source.getString(Tags.ContentDate)) != null) {
            fg.putDA(Tags.ContentDate, s);
        }
        if ((s = source.getString(Tags.ContentTime)) != null) {
            fg.putTM(Tags.ContentTime, s);
        }
        if ((s = source.getString(Tags.InstanceNumber)) != null) {
            fg.putIS(Tags.InstanceNumber, s);
        }
        d = source.getDateTime(Tags.ContentDate, Tags.ContentTime);
        if (d != null) {
            if (contentDatetime == null
                    || contentDatetime.compareTo(d) > 0) {
                contentDatetime = d;
                instanceNumber = s;
            }
        }
    }

    private void mergeFunctionalGroups(Dataset dataset, Dataset source) {
        DcmElement perFrameFGSeq =
                dataset.get(Tags.PerFrameFunctionalGroupsSeq);
        Dataset perFrameFGs = perFrameFGSeq.addNewItem();
        addReferencedImageFG(perFrameFGs, source);
        addFrameContentFG(perFrameFGs, source);
        int[] noLongerSharedFGTags = {};
        for (Iterator it = fgFilters.iterator(); it.hasNext();) {
            DcmElement el = (DcmElement) it.next();
            int fgTag = el.tag();
            Dataset fgFilter = el.getItem();
            Dataset fg = source.subSet(fgFilter);
            Dataset sharedFG = sharedFGs.getItem(fgTag);
            if (!fg.equals(sharedFG)) {
                if (sharedFG != null) {
                    noLongerSharedFGTags = append(noLongerSharedFGTags, fgTag);
                }
                if (!fg.isEmpty()) {
                    perFrameFGs.putSQ(fgTag).addNewItem().putAll(fg);
                }
            }
        }
        String[] frameType = source.getStrings(Tags.ImageType);
        if (!Arrays.equals(this.imageType, frameType)
                && sharedFGs.contains(frameTypeTag)) {
            noLongerSharedFGTags = append(noLongerSharedFGTags, frameTypeTag);
        }
        if (noLongerSharedFGTags.length != 0) {
            Dataset noLongerSharedFGs = sharedFGs.subSet(noLongerSharedFGTags);
            for (int i = 0, n = perFrameFGSeq.countItems() - 1; i < n; i++) {
                perFrameFGSeq.getItem(i).putAll(noLongerSharedFGs);
            }
            noLongerSharedFGs.clear();
        }
        if (!sharedFGs.contains(frameTypeTag)) {
            perFrameFGs.putSQ(frameTypeTag).addNewItem()
                    .putCS(Tags.FrameType, frameType);
        }
        this.imageType = mergeImageType(this.imageType, frameType);
    }

    private int[] append(int[] a, int i) {
        int[] tmp = new int[a.length + 1];
        System.arraycopy(a, 0, tmp, 0, a.length);
        tmp[a.length] = i;
        return tmp;
    }

    public Dataset build() throws UpgradeToEnhancedMFException {
        if (curFrame < pixelDataOffsets.length) {
            throw new IllegalStateException("curFrame: " + curFrame
                    + " < numFrame: " + pixelDataOffsets.length);
        }
        dataset.putCS(Tags.ImageType, imageType);
        dataset.putCS(Tags.InstanceNumber, instanceNumber);
        dataset.putDA(Tags.ContentDate, contentDatetime);
        dataset.putTM(Tags.ContentTime, contentDatetime);
        dataset.putDT(Tags.AcquisitionDatetime, acquisitionDatetime);
        dataset.putIS(Tags.NumberOfFrames, pixelDataOffsets.length);
        UIDGenerator uidGen = UIDGenerator.getInstance();
        if (!dataset.containsValue(Tags.SeriesInstanceUID)) {
            dataset.putUI(Tags.SeriesInstanceUID, uidGen.createUID());
        }
        dataset.putUI(Tags.SOPClassUID, filter.getString(Tags.SOPClassUID));
        dataset.putUI(Tags.SOPInstanceUID, uidGen.createUID());
        try {
            dataset.setFileMetaInfo(
                    DcmObjectFactory.getInstance().newFileMetaInfo(dataset,
                            noPixelData
                                    ? (deflate ? UIDs.NoPixelDataDeflate
                                               : UIDs.NoPixelData)
                                    : tsuid));
        } catch (DcmValueException e) {
            throw new UpgradeToEnhancedMFException(e);
        }
        return dataset;
    }

    private String[] mergeImageType(String[] t1, String[] t2) {
        if (t1.length < t2.length) {
            String[] tmp = t1;
            t1 = t2;
            t2 = tmp;
        }
        for (int i = 0; i < t1.length; i++) {
            if (!(i < t2.length && t1[i].equals(t2[i]))) {
                t1[i] = "MIXED";
            }
        }
        return t1;
    }
}

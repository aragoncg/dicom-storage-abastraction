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
package org.dcm4chex.archive.codec;

import java.awt.color.ColorSpace;
import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.dcm4chex.archive.util.FileUtils;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision$ $Date$
 * @since Jun 3, 2008
 */
class BufferedImagePool {

    static final Logger log = Logger.getLogger(BufferedImagePool.class);

    private static final int[] GRAY_BAND_OFFSETS = { 0 };
    private static final int[] RGB_BAND_OFFSETS = { 0, 1, 2 };

    private List<BufferedImage> pool = new ArrayList<BufferedImage>();
    private int maxSize = 10;
    private long maxMemory = FileUtils.MEGA;
    private long poolMemory = 0;
    private float borrowOrCreateCount = 0.f;
    private float borrowCount = 0.f;

    public synchronized BufferedImage borrowOrCreateBufferedImage(int rows,
            int columns, int bitsUsed, int samples, int planarConfiguration,
            int dataType) {
        borrowOrCreateCount++;
        for (int index = pool.size(); --index >= 0;) {
            BufferedImage bi = pool.get(index);
            ColorModel cm = bi.getColorModel();
            WritableRaster raster = bi.getRaster();
            DataBuffer db = raster.getDataBuffer();
            if (raster.getHeight() == rows
                    && raster.getWidth() == columns
                    && raster.getNumBands() == samples
                    && cm.getComponentSize(0) == bitsUsed
                    && db.getNumBanks() == (planarConfiguration != 0 ? samples
                            : 1) && db.getDataType() == dataType) {
                pool.remove(index);
                poolMemory -= sizeOf(bi);
                borrowCount++;
                log("borrow", bi);
                return bi;
            }
        }
        WritableRaster r = Raster.createWritableRaster(getSampleModel(
                rows, columns, samples, planarConfiguration, dataType), null);
        BufferedImage bi = new BufferedImage(
                getColorModel(bitsUsed, samples, dataType), r, false, null);
        log("create", bi);
        return bi;
    }

    private static void log(String msg, BufferedImage bi) {
        if (log.isDebugEnabled()) {
            ColorModel cm = bi.getColorModel();
            WritableRaster raster = bi.getRaster();
            log.debug(msg + " BufferedImage@"
                    + Integer.toHexString(bi.hashCode()) 
                    + " " + raster.getWidth() + "x" + raster.getHeight()
                    + "x" + raster.getNumBands() + " "
                    + DataBuffer.getDataTypeSize(raster.getTransferType())
                    + "(" + cm.getComponentSize(0) + ") bits");
        }
    }

    private static SampleModel getSampleModel(int rows, int columns,
            int samples, int planarConfiguration, int dataType) {
        return (planarConfiguration == 0) ? new PixelInterleavedSampleModel(
                dataType, columns, rows, samples, columns * samples,
                samples == 1 ? GRAY_BAND_OFFSETS : RGB_BAND_OFFSETS)
                : new BandedSampleModel(dataType, columns, rows, samples);
    }

    private static ColorModel getColorModel(int bitsUsed, int samples,
            int dataType) {
        return (samples == 3) ? new ComponentColorModel(ColorSpace
                .getInstance(ColorSpace.CS_sRGB), new int[] { bitsUsed,
                bitsUsed, bitsUsed }, false, false, ColorModel.OPAQUE, dataType)
                : new ComponentColorModel(ColorSpace
                        .getInstance(ColorSpace.CS_GRAY),
                        new int[] { bitsUsed }, false, false,
                        ColorModel.OPAQUE, dataType);
    }

    private static int sizeOf(BufferedImage bi) {
        WritableRaster raster = bi.getRaster();
        int biSize = raster.getWidth() * raster.getHeight()
                * raster.getNumBands() 
                * (DataBuffer.getDataTypeSize(raster.getTransferType()) >> 3);
        return biSize;
    }
    
    public synchronized void returnBufferedImage(BufferedImage bi) {
        long biSize;
        if (maxSize == 0 || (biSize = sizeOf(bi)) > maxMemory) {
            log("trash", bi);
            return;
        }
        pool.add(bi);
        poolMemory += biSize;
        log("return", bi);
        resize();
    }

    private void resize() {
        BufferedImage bi;
        while (pool.size() > maxSize || poolMemory > maxMemory) {
            bi = pool.remove(0);
            poolMemory -= sizeOf(bi);
            log("trash", bi);
        }
    }

    public final int getMaxSize() {
        return maxSize;
    }

    public final void setMaxSize(int maxSize) {
        if (maxSize < 0) {
            throw new IllegalArgumentException("maxSize: " + maxSize);
        }
        this.maxSize = maxSize;
        resize();
    }

    public final long getMaxMemory() {
        return maxMemory;
    }

    public final void setMaxMemory(long maxMemory) {
        if (maxMemory < 0) {
            throw new IllegalArgumentException("maxMemory: " + maxMemory);
        }
        this.maxMemory = maxMemory;
    }

    public final int getPoolSize() {
        return pool.size();
    }

    public final long getPoolMemory() {
        return poolMemory;
    }

    public float getHitRate() {
        return borrowCount / borrowOrCreateCount;
    }

    public void resetHitRate() {
        borrowOrCreateCount = borrowCount = 0.f;
    }
}

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

package org.dcm4chex.archive.codec;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;

import org.apache.log4j.Logger;
import org.dcm4che.data.Dataset;
import org.dcm4che.dict.Tags;
import org.dcm4che.dict.UIDs;

import EDU.oswego.cs.dl.util.concurrent.FIFOSemaphore;
import EDU.oswego.cs.dl.util.concurrent.Semaphore;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 6386 $ $Date: 2006-05-15 11:59:33 +0200 (Mon, 15 May
 *          2006) $
 * @since 14.03.2005
 * 
 */

public abstract class CodecCmd {

    static final Logger log = Logger.getLogger(CodecCmd.class);

    static final String YBR_RCT = "YBR_RCT";

    static final String JPEG2000 = "jpeg2000";

    static final String JPEG = "jpeg";

    static final String JPEG_LOSSLESS = "JPEG-LOSSLESS";

    static final String JPEG_LS = "JPEG-LS";

    static int maxConcurrentCodec = 1;

    static Semaphore codecSemaphore = new FIFOSemaphore(maxConcurrentCodec);

    static BufferedImagePool biPool = new BufferedImagePool();

    protected final int samples;

    protected final int frames;

    protected final int rows;

    protected final int columns;

    protected final int planarConfiguration;

    protected final int bitsAllocated;

    protected final int bitsStored;

    protected final int pixelRepresentation;

    protected final int frameLength;

    protected final int bitsUsed;

    protected final String tsuid;

    protected final int dataType;

    protected CodecCmd(Dataset ds, String tsuid) {
        this.samples = ds.getInt(Tags.SamplesPerPixel, 1);
        this.frames = ds.getInt(Tags.NumberOfFrames, 1);
        this.rows = ds.getInt(Tags.Rows, 1);
        this.columns = ds.getInt(Tags.Columns, 1);
        this.bitsAllocated = ds.getInt(Tags.BitsAllocated, 8);
        this.bitsStored = ds.getInt(Tags.BitsStored, bitsAllocated);
        this.bitsUsed = bitsAllocated;
        this.pixelRepresentation = ds.getInt(Tags.PixelRepresentation, 0);
        this.planarConfiguration = ds.getInt(Tags.PlanarConfiguration, 0);
        this.frameLength = rows * columns * samples * bitsAllocated / 8;
        this.tsuid = tsuid;
        switch (bitsAllocated) {
        case 8:
            this.dataType = DataBuffer.TYPE_BYTE;
            break;
        case 16:
            this.dataType = (pixelRepresentation != 0 
                    && (UIDs.JPEG2000Lossless.equals(tsuid)
                            || UIDs.JPEG2000Lossy.equals(tsuid)))
                                    ? DataBuffer.TYPE_SHORT
                                    : DataBuffer.TYPE_USHORT;
            break;
        default:
            throw new IllegalArgumentException("bits allocated:"
                    + bitsAllocated);
        }
    }

    public static void setMaxConcurrentCodec(int maxConcurrentCodec) {
        codecSemaphore = new FIFOSemaphore(maxConcurrentCodec);
        CodecCmd.maxConcurrentCodec = maxConcurrentCodec;
    }

    public static int getMaxConcurrentCodec() {
        return maxConcurrentCodec;
    }

    public static int getMaxBufferedImagePoolSize() {
        return biPool.getMaxSize();
    }

    public static void setMaxBufferedImagePoolSize(int maxSize) {
        biPool.setMaxSize(maxSize);
    }

    public static int getCurrentBufferedImagePoolSize() {
        return biPool.getPoolSize();
    }

    public static long getMaxBufferedImagePoolMemory() {
        return biPool.getMaxMemory();
    }

    public static void setMaxBufferedImagePoolMemory(long maxMemory) {
        biPool.setMaxMemory(maxMemory);
    }

    public static long getCurrentBufferedImagePoolMemory() {
        return biPool.getPoolMemory();
    }

    public static float getBufferedImagePoolHitRate() {
        return biPool.getHitRate();
    }

    public static void resetBufferedImagePoolHitRate() {
        biPool.resetHitRate();
    }

    public int getPixelDataLength() {
        return frames * frameLength;
    }

    protected BufferedImage getBufferedImage() {
        return biPool.borrowOrCreateBufferedImage(rows, columns, bitsUsed,
                samples, planarConfiguration, dataType);
    }

    protected void returnBufferedImage(BufferedImage bi) {
        biPool.returnBufferedImage(bi);
    }

}

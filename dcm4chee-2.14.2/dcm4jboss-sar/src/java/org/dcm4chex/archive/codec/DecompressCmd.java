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

import java.awt.image.BufferedImage;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.security.DigestOutputStream;
import java.security.MessageDigest;

import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;

import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmEncodeParam;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.data.DcmParser;
import org.dcm4che.data.DcmParserFactory;
import org.dcm4che.data.FileFormat;
import org.dcm4che.data.FileMetaInfo;
import org.dcm4che.dict.Tags;
import org.dcm4che.dict.UIDs;
import org.dcm4che.util.BufferedOutputStream;
import org.dcm4cheri.image.ImageReaderFactory;
import org.dcm4cheri.image.ItemParser;

import com.sun.media.imageio.stream.SegmentedImageInputStream;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @author Franz Willer <franz.willer@agfa.com>
 * @version $Revision: 6790 $ $Date: 2008-08-12 09:27:53 +0200 (Tue, 12 Aug 2008) $
 * @since 22.05.2004
 * 
 */
public class DecompressCmd extends CodecCmd {

    private static final String J2KIMAGE_READER =
        "com.sun.media.imageioimpl.plugins.jpeg2000.J2KImageReader";

    private final ItemParser itemParser;

    private final ImageInputStream iis;

    private SegmentedImageInputStream siis;

    private int[] simpleFrameList;

    public static byte[] decompressFile(File inFile, File outFile,
            String outTS, int pxdataVR, byte[] buffer) throws Exception {
        log.info("M-READ file:" + inFile);
        FileImageInputStream fiis = new FileImageInputStream(inFile);
        try {
            DcmParser parser = DcmParserFactory.getInstance()
                    .newDcmParser(fiis);
            DcmObjectFactory dof = DcmObjectFactory.getInstance();
            Dataset ds = dof.newDataset();
            parser.setDcmHandler(ds.getDcmHandler());
            parser.parseDcmFile(FileFormat.DICOM_FILE, Tags.PixelData);
            log.info("M-WRITE file:" + outFile);
            MessageDigest md = MessageDigest.getInstance("MD5");
            DigestOutputStream dos = new DigestOutputStream(
                    new FileOutputStream(outFile), md);
            BufferedOutputStream bos = new BufferedOutputStream(dos, buffer);
            try {
                DcmEncodeParam encParam = DcmEncodeParam.valueOf(outTS);
                DecompressCmd cmd = new DecompressCmd(ds,
                        getTransferSyntax(ds), parser);
                int len = cmd.getPixelDataLength();
                FileMetaInfo fmi = dof.newFileMetaInfo(ds, outTS);
                ds.setFileMetaInfo(fmi);
                ds.writeFile(bos, encParam);
                ds.writeHeader(bos, encParam, Tags.PixelData, pxdataVR,
                        (len + 1) & ~1);
                try {
                    cmd.decompress(encParam.byteOrder, bos);
                } catch (IOException e) {
                    throw e;
                } catch (Throwable e) {
                    throw new RuntimeException("Decompression failed:", e);
                }
                if ((len & 1) != 0)
                    bos.write(0);
                parser.parseDataset(parser.getDcmDecodeParam(), -1);
                ds.subSet(Tags.PixelData, -1).writeDataset(bos, encParam);
            } finally {
                bos.close();
            }
            return md.digest();
        } finally {
            try {
                fiis.close();
            } catch (IOException ignore) {
            }
        }
    }

    public static String getTransferSyntax(Dataset ds) {
        FileMetaInfo fmi = ds.getFileMetaInfo();
        return fmi != null ? fmi.getTransferSyntaxUID()
                : UIDs.ImplicitVRLittleEndian;
    }

    public DecompressCmd(Dataset ds, String tsuid, DcmParser parser)
            throws IOException {
        super(ds, tsuid);
        this.iis = parser.getImageInputStream();
        this.itemParser = new ItemParser(parser, frames, tsuid);
        this.siis = new SegmentedImageInputStream(iis, itemParser);
        if (samples == 3) {
            ds.putUS(Tags.PlanarConfiguration, 0);
            if (tsuid.equals(UIDs.JPEGBaseline)
                    || tsuid.equals(UIDs.JPEGExtended)) {
                ds.putCS(Tags.PhotometricInterpretation, "RGB");
            }
        }
    }

    public void decompress(ByteOrder byteOrder, OutputStream out)
            throws Exception {
        long t1;
        ImageReader reader = null;
        BufferedImage bi = null;
        boolean codecSemaphoreAquired = false;
        try {
            log.debug("acquire codec semaphore");
            codecSemaphore.acquire();
            codecSemaphoreAquired = true;
            log.info("start decompression of image: " + rows + "x" + columns
                    + "x" + frames);
            t1 = System.currentTimeMillis();
            ImageReaderFactory f = ImageReaderFactory.getInstance();
            reader = f.getReaderForTransferSyntax(tsuid);
            bi = getBufferedImage();
            for (int i = 0, n = getNumberOfFrames(); i < n; ++i) {
                int frame = simpleFrameList != null ? (simpleFrameList[i]-1) : i;
                log.debug("start decompression of frame #" + (frame + 1));
                itemParser.seekFrame(siis, frame);
                reader.setInput(siis);
                ImageReadParam param = reader.getDefaultReadParam();
                param.setDestination(bi);
                bi = reader.read(0, param);
                // workaround for Bug in J2KImageReader and
                // J2KImageReaderCodecLib.setInput()
                if (reader.getClass().getName().startsWith(J2KIMAGE_READER)) {
                    reader.dispose();
                    reader = i < n - 1 ? f
                            .getReaderForTransferSyntax(tsuid) : null;
                } else {
                    reader.reset();
                }
                write(bi.getRaster(), out, byteOrder);
            }
            itemParser.seekFooter();
        } finally {
            if (reader != null)
                reader.dispose();
            if (bi != null)
                biPool.returnBufferedImage(bi);
            if (codecSemaphoreAquired) {
                log.debug("release codec semaphore");
                codecSemaphore.release();
            }
        }
        long t2 = System.currentTimeMillis();
        log.info("finished decompression in " + (t2 - t1) + "ms.");
    }

    private void write(WritableRaster raster, OutputStream out,
            ByteOrder byteOrder) throws IOException {
        DataBuffer buffer = raster.getDataBuffer();
        final int stride = ((ComponentSampleModel) raster.getSampleModel())
                .getScanlineStride();
        final int h = raster.getHeight();
        final int w = raster.getWidth();
        final int b = raster.getNumBands();
        final int wb = w * b;
        switch (buffer.getDataType()) {
        case DataBuffer.TYPE_BYTE:
            for (int i = 0; i < h; ++i)
                out.write(((DataBufferByte) buffer).getData(), i * stride, wb);
            break;
        case DataBuffer.TYPE_USHORT:
            if (byteOrder == ByteOrder.LITTLE_ENDIAN)
                for (int i = 0; i < h; ++i)
                    writeShortLE(((DataBufferUShort) buffer).getData(), i
                            * stride, wb, out);
            else
                for (int i = 0; i < h; ++i)
                    writeShortBE(((DataBufferUShort) buffer).getData(), i
                            * stride, wb, out);
            break;
        case DataBuffer.TYPE_SHORT:
            if (byteOrder == ByteOrder.LITTLE_ENDIAN)
                for (int i = 0; i < h; ++i)
                    writeShortLE(((DataBufferShort) buffer).getData(), i
                            * stride, wb, out);
            else
                for (int i = 0; i < h; ++i)
                    writeShortBE(((DataBufferShort) buffer).getData(), i
                            * stride, wb, out);
            break;
        default:
            throw new RuntimeException(buffer.getClass().getName()
                    + " not supported");
        }
    }

    private void writeShortLE(short[] data, int off, int len, OutputStream out)
            throws IOException {
        for (int i = off, end = off + len; i < end; i++) {
            final short px = data[i];
            out.write(px & 0xff);
            out.write((px >>> 8) & 0xff);
        }
    }

    private void writeShortBE(short[] data, int off, int len, OutputStream out)
            throws IOException {
        for (int i = off, end = off + len; i < end; i++) {
            final short px = data[i];
            out.write((px >>> 8) & 0xff);
            out.write(px & 0xff);
        }
    }

    public void setSimpleFrameList(int[] simpleFrameList) {
        this.simpleFrameList = simpleFrameList;
    }

    public int getPixelDataLength() {
        return getNumberOfFrames() * frameLength;
    }

    private int getNumberOfFrames() {
        return simpleFrameList != null ? simpleFrameList.length : frames;
    }
}

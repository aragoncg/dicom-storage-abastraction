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
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.WritableRaster;
import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.security.DigestOutputStream;
import java.security.MessageDigest;

import javax.imageio.IIOImage;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmDecodeParam;
import org.dcm4che.data.DcmEncodeParam;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.data.DcmParser;
import org.dcm4che.data.DcmParserFactory;
import org.dcm4che.data.FileFormat;
import org.dcm4che.data.FileMetaInfo;
import org.dcm4che.dict.Tags;
import org.dcm4che.dict.UIDs;
import org.dcm4che.dict.VRs;
import org.dcm4che.util.BufferedOutputStream;
import org.dcm4cheri.image.ImageWriterFactory;

import com.sun.media.imageio.plugins.jpeg2000.J2KImageWriteParam;

/**
 * @author gunter.zeilinter@tiani.com
 * @version $Revision: 8038 $ $Date: 2008-11-11 12:07:06 +0100 (Tue, 11 Nov 2008) $
 * @since 11.06.2004
 *
 */
public abstract class CompressCmd extends CodecCmd {

    private static final byte[] ITEM_TAG = { (byte) 0xfe, (byte) 0xff,
            (byte) 0x00, (byte) 0xe0};

    private static class Jpeg2000 extends CompressCmd {

        public Jpeg2000(Dataset ds, String tsuid) {
            super(ds, tsuid);
        }

        public void coerceDataset(Dataset ds) {
            if (samples == 3) {
                ds.putUS(Tags.PlanarConfiguration, 0);
                ds.putCS(Tags.PhotometricInterpretation, YBR_RCT);
            }
        }

        protected void initWriteParam(ImageWriteParam param) {
            if (param instanceof J2KImageWriteParam) {
                J2KImageWriteParam j2KwParam = (J2KImageWriteParam) param;
                j2KwParam.setWriteCodeStreamOnly(true);
            }
        }
    }

    private static class JpegLossless extends CompressCmd {

        public JpegLossless(Dataset ds, String tsuid) {
            super(ds, tsuid);
        }

        public void coerceDataset(Dataset ds) {
            if (samples == 3) {
                ds.putUS(Tags.PlanarConfiguration, 0);
            }
        }

        protected void initWriteParam(ImageWriteParam param) {
            param.setCompressionType(JPEG_LOSSLESS);
        }
    };

    private static class JpegLS extends CompressCmd {

        public JpegLS(Dataset ds, String tsuid) {
            super(ds, tsuid);
        }

        public void coerceDataset(Dataset ds) {
            if (samples == 3) {
                ds.putUS(Tags.PlanarConfiguration, 0);
            }

        }

        protected void initWriteParam(ImageWriteParam param) {
            param.setCompressionType(JPEG_LS);
        }
    }
    
    public static byte[] compressFile(File inFile, File outFile, String tsuid,
    		int[] pxdataVR, byte[] buffer)
    		throws Exception {
        log.info("M-READ file:" + inFile);
    	InputStream in = new BufferedInputStream(new FileInputStream(inFile));
    	try {
    		DcmParser p = DcmParserFactory.getInstance().newDcmParser(in);
    		final DcmObjectFactory of = DcmObjectFactory.getInstance();
			Dataset ds = of.newDataset();
    		p.setDcmHandler(ds.getDcmHandler());
    		p.parseDcmFile(FileFormat.DICOM_FILE, Tags.PixelData);
    		if (pxdataVR != null && pxdataVR.length != 0)
    			pxdataVR[0] = p.getReadVR();
    		FileMetaInfo fmi = of.newFileMetaInfo(ds, tsuid);
    		ds.setFileMetaInfo(fmi);
            log.info("M-WRITE file:" + outFile);
            MessageDigest md = MessageDigest.getInstance("MD5");
            DigestOutputStream dos = new DigestOutputStream(new FileOutputStream(outFile), md);
            BufferedOutputStream bos = new BufferedOutputStream(dos, buffer);
            try {
                DcmDecodeParam decParam = p.getDcmDecodeParam();
    			DcmEncodeParam encParam = DcmEncodeParam.valueOf(tsuid);
                CompressCmd compressCmd = CompressCmd.createCompressCmd(ds, tsuid);
                compressCmd.coerceDataset(ds);
                ds.writeFile(bos, encParam);
                ds.writeHeader(bos, encParam, Tags.PixelData, VRs.OB, -1);
                int read = compressCmd.compress(decParam.byteOrder, in, bos);
                ds.writeHeader(bos, encParam, Tags.SeqDelimitationItem,
                        VRs.NONE, 0);
                skipFully(in, p.getReadLength() - read);
                p.parseDataset(decParam, -1);
                ds.subSet(Tags.PixelData, -1).writeDataset(bos, encParam);
            } finally {
                bos.close();
            }
            return md.digest();
    	} finally {
    		in.close();
    	}
    }

    private static void skipFully(InputStream in, int n) throws IOException {
        int remaining = n;
        int skipped = 0;
        while (remaining > 0) {
            if ((skipped = (int) in.skip(remaining)) == 0) {
                throw new EOFException();
            }
            remaining -= skipped;
        }
    }

    public static CompressCmd createCompressCmd(Dataset ds, String tsuid) {
    	if (UIDs.JPEG2000Lossless.equals(tsuid)) {
                return new Jpeg2000(ds, tsuid);
        }
        if (UIDs.JPEGLSLossless.equals(tsuid)) {
            return new JpegLS(ds, tsuid);
        }
        if (UIDs.JPEGLossless.equals(tsuid)
                || UIDs.JPEGLossless14.equals(tsuid)) {
                return new JpegLossless(ds, tsuid);
        }
        throw new IllegalArgumentException("tsuid:" + tsuid);
    }

    protected CompressCmd(Dataset ds, String tsuid) {
        super(ds, tsuid);
    }

    public abstract void coerceDataset(Dataset ds);

    protected abstract void initWriteParam(ImageWriteParam param);

    public int compress(ByteOrder byteOrder, InputStream in, OutputStream out)
            throws Exception {
        long t1;
        ImageWriter w = null;
        BufferedImage bi = null;
        boolean codecSemaphoreAquired = false;
        long end = 0;
        try {
            log.debug("acquire codec semaphore");
            codecSemaphore.acquire();
            codecSemaphoreAquired = true;
            log.info("start compression of image: " + rows + "x" + columns
                    + "x" + frames);
            t1 = System.currentTimeMillis();
            ImageOutputStream ios = new MemoryCacheImageOutputStream(out);
            ios.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            ImageWriterFactory f = ImageWriterFactory.getInstance();
            w = f.getWriterForTransferSyntax(tsuid);
            ImageWriteParam wParam = w.getDefaultWriteParam();
            initWriteParam(wParam);
            bi = getBufferedImage();
            WritableRaster raster = bi.getRaster();
            DataBuffer db = raster.getDataBuffer();
            ios.write(ITEM_TAG);
            ios.writeInt(0);
            for (int i = 0; i < frames; ++i) {
                log.debug("start compression of frame #" + (i + 1));
                ios.write(ITEM_TAG);
                long mark = ios.getStreamPosition();
                ios.writeInt(0);
                switch (dataType) {
                case DataBuffer.TYPE_BYTE:
                    read(in, ((DataBufferByte) db).getBankData());
                    break;
                case DataBuffer.TYPE_SHORT:
                    read(byteOrder, in, ((DataBufferShort) db).getBankData());
                    break;
                case DataBuffer.TYPE_USHORT:
                    read(byteOrder, in, ((DataBufferUShort) db).getBankData());
                    break;
                default:
                    throw new RuntimeException("dataType:" + db.getDataType());
                }
                w.setOutput(ios);
                w.write(null, new IIOImage(bi, null, null), wParam);
                end = ios.getStreamPosition();
                if ((end & 1) != 0) {
                    ios.write(0);
                    ++end;
                }
                ios.seek(mark);
                ios.writeInt((int) (end - mark - 4));
                ios.seek(end);
                ios.flush();
            }
        } finally {
            if (w != null) w.dispose();
            if (bi != null) biPool.returnBufferedImage(bi);
            if (codecSemaphoreAquired) {
                log.debug("release codec semaphore");
                codecSemaphore.release();
            }
        }
        long t2 = System.currentTimeMillis();
        int pixelDataLength = frameLength * frames;
        log.info("finished compression " + ((float) pixelDataLength / end)
                + " : 1 in " + (t2 - t1) + "ms.");
        return pixelDataLength;
    }

    private void read(ByteOrder byteOrder, InputStream in, short[][] data)
            throws IOException {
        if (byteOrder == ByteOrder.LITTLE_ENDIAN)
            readLE(in, data);
        else
            readBE(in, data);
    }

    private void readLE(InputStream in, short[][] data) throws IOException {
        int lo, hi;
        for (int i = 0; i < data.length; i++) {
            short[] bank = data[i];
            for (int j = 0; j < bank.length; j++) {
                lo = in.read();
                hi = in.read();
                if ((lo | hi) < 0) throw new EOFException();
                bank[j] = (short) ((lo & 0xff) + (hi << 8));
            }
        }
    }

    private void readBE(InputStream in, short[][] data) throws IOException {
        int lo, hi;
        for (int i = 0; i < data.length; i++) {
            short[] bank = data[i];
            for (int j = 0; j < bank.length; j++) {
                hi = in.read();
                lo = in.read();
                if ((lo | hi) < 0) throw new EOFException();
                bank[j] = (short) ((lo & 0xff) + (hi << 8));
            }
        }
    }

    private void read(InputStream in, byte[][] data) throws IOException {
    	int read;
        for (int i = 0; i < data.length; i++) {
            byte[] bank = data[i];
            for (int toread = bank.length; toread > 0;) {
                read = in.read(bank, bank.length - toread, toread);
                if ( read == -1 ) throw new EOFException("Length of pixel matrix is too short!");
                toread -= read;
            }
        }
    }
}

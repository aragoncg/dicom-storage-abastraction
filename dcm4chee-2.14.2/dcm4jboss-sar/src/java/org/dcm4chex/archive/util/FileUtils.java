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

package org.dcm4chex.archive.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;

import org.apache.log4j.Logger;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.data.DcmParser;
import org.dcm4che.data.DcmParserFactory;
import org.dcm4che.data.FileFormat;
import org.dcm4che.dict.Tags;
import org.dcm4chex.archive.factory.ScpFactoryUtil;
import org.jboss.system.server.ServerConfigLocator;


/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 8039 $ $Date: 2008-11-11 12:15:34 +0100 (Tue, 11 Nov 2008) $
 * @since 19.09.2004
 *
 */
public class FileUtils {

    protected static final Logger log = Logger.getLogger(FileUtils.class);

    private static final int BUFFER_SIZE = 512;
    
	public static final long MEGA = 1000000L;

    public static final long GIGA = 1000000000L;

    public static final long MAX_TIMES_CREATE_FILE = 10;
    
    public static final long INTERVAL_CREATE_FILE = 250; //ms

    private static char[] HEX_DIGIT = { '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

	public static String toHex(int val) {
	    char[] ch8 = new char[8];
	    for (int i = 8; --i >= 0; val >>= 4) {
	        ch8[i] = HEX_DIGIT[val & 0xf];
	    }
	    return String.valueOf(ch8);
	}
    
	/**
	 * Create a new file based on hash code, including the directory structure if necessary. 
	 * In case of collision, we will reset hashcode and try again.
	 * 
	 * @param dir the directory where the new file will be created
	 * @param hash the initial hash value
	 * @return the file created successfully
	 * @throws Exception
	 */
    public static File createNewFile(File dir, int hash) throws Exception {
		File f = null;
		boolean success = false;
		Exception lastException = null;
		for(int i = 0; i < MAX_TIMES_CREATE_FILE && !success; i++) {					
			// In UNIX, mkdirs and createNewFile can throw IOException 
			// instead of nicely returning false
			try
			{				
		        if (!dir.exists()) {
		            success = dir.mkdirs();
		            if(!success)
		            	throw new IOException("Directory creation failed: " + dir.getCanonicalPath());
		        }

				// Try to construct a new file name everytime
				f = new File(dir, toHex(hash++));

				success = f.createNewFile();	
				
				if(i > 0)
					log.info("file: " + dir.getCanonicalPath() 
							+ " created successfully after retries: " + i);
				
				if(success)
					return f;
				else
					i--; // if not success, we should not increment i which is for exceptional retry
			}
			catch(Exception e)
			{
	        	if(lastException == null )
	        		log.warn("failed to create file: " + dir.getCanonicalPath()
	        				+ " - retry: " + (i+1) + " of " + MAX_TIMES_CREATE_FILE 
	        				+ ". Will retry again.", e);
	        	else
	        		log.warn("failed to create file: " + dir.getCanonicalPath() 
	        				+ " - got the same exception as above - retry: "
	        				+ (i+1) + " of " + MAX_TIMES_CREATE_FILE + ". Will retry again" );	        	
	        	lastException = e;

				success = false;
								
				// Maybe other thread is trying to do the same thing
				// Let's wait for INTERVAL_CREATE_FILE ms - this rarely happens
				try {
					Thread.sleep(INTERVAL_CREATE_FILE);
				} catch (InterruptedException e1) {
				} 
			}
		}
		// Run out of retries, throw original exception
		throw lastException;
    }
	
    public static String slashify(File f) {
        return f.getPath().replace(File.separatorChar, '/');
    }
    
    public static File resolve(File f) {
        if (f.isAbsolute()) return f;
        File serverHomeDir = ServerConfigLocator.locate().getServerHomeDir();
        return new File(serverHomeDir, f.getPath());
    }
    
    public static File toFile(String unixPath) {
        return resolve(new File(unixPath.replace('/', File.separatorChar)));
    }

    public static File toExistingFile(String unixPath) throws FileNotFoundException {
        File f = toFile(unixPath);
        if (!f.isFile()) {
            throw new FileNotFoundException(f.toString());
        }
        return f;
    }
    
    public static File toFile(String unixDirPath, String unixFilePath) {
        return resolve(new File(unixDirPath.replace('/', File.separatorChar),
                unixFilePath.replace('/', File.separatorChar)));
    }
    
    public static String formatSize(long size) {
        return (size == -1) ? "UNKOWN"
                : (size < GIGA) ? ((float) size / MEGA) + "MB"
                                : ((float) size / GIGA) + "GB";
    }

    public static long parseSize(String s, long minSize) {
        long u;
        if (s.endsWith("GB"))
            u = GIGA;
        else if (s.endsWith("MB"))
            u = MEGA;
        else
            throw new IllegalArgumentException(s);
        try {
            long size = (long) (Float.parseFloat(s.substring(0, s.length() - 2)) * u);
            if (size >= minSize)
                return size;
        } catch (IllegalArgumentException e) {
        }
        throw new IllegalArgumentException(s);
    }
    
    public static boolean equalsPixelData(File f1, File f2)
    		throws IOException {
    	InputStream in1 = new BufferedInputStream(new FileInputStream(f1));
    	try {
    		InputStream in2 = new BufferedInputStream(new FileInputStream(f2));
    		try {    			
                Dataset attrs = DcmObjectFactory.getInstance().newDataset();
    	    	DcmParserFactory pf = DcmParserFactory.getInstance();
				DcmParser p1 = pf.newDcmParser(in1);
    	    	DcmParser p2 = pf.newDcmParser(in2);
                p1.setDcmHandler(attrs.getDcmHandler());
    			p1.parseDcmFile(FileFormat.DICOM_FILE, Tags.PixelData);
    			p2.parseDcmFile(FileFormat.DICOM_FILE, Tags.PixelData);
                int samples = attrs.getInt(Tags.SamplesPerPixel, 1);
                int frames = attrs.getInt(Tags.NumberOfFrames, 1);
                int rows = attrs.getInt(Tags.Rows, 1);
                int columns = attrs.getInt(Tags.Columns, 1);
                int bitsAlloc = attrs.getInt(Tags.BitsAllocated, 8);
                int bitsStored = attrs.getInt(Tags.BitsStored, bitsAlloc);
                int frameLength = rows * columns * samples * bitsAlloc / 8;
                int pixelDataLength = frameLength * frames;
                if (pixelDataLength > p1.getReadLength()
                        || pixelDataLength > p2.getReadLength()) {
                    return false;
                }
                byte[] b1 = new byte[BUFFER_SIZE];
                byte[] b2 = new byte[BUFFER_SIZE];
                int[] mask = { 0xff, 0xff };
                int len, len2;
                if (bitsAlloc == 16 && bitsStored < 16) {
                    mask[p1.getDcmDecodeParam().byteOrder == ByteOrder.LITTLE_ENDIAN ? 1 : 0]
                            = 0xff >>> (16 - bitsStored);
                } 
                int pos = 0;
                while (pos < pixelDataLength) {
                    len = in1.read(b1, 0, Math.min(pixelDataLength - pos, BUFFER_SIZE));
                    if (len < 0) // EOF
                        return false;
                    int off = 0;
                    while (off < len) {
                        off += len2 = in2.read(b2, off, len - off);
                        if (len2 < 0) // EOF
                            return false;
                    }
                    for (int i=0; i<len; i++, pos++)
                        if (((b1[i] - b2[i]) & mask[pos & 1]) != 0)
                            return false;
                }
                return true;
    		} finally {
    			in2.close();
    		}
    	} finally {
    		in1.close();
    	}
    }
    
    /**
     * The following method is modified by YangLin@cn-arg.com 
     * on 03.04.2009.
     * Get file deleted according to Dicom image storage way.
     */
    public static boolean delete(File file, boolean deleteEmptyParents) {
        log.info("M-DELETE file: " + file);
        
        return ScpFactoryUtil.getScpFactory().
                              getFileDeleted(file, log, deleteEmptyParents);
    }
}

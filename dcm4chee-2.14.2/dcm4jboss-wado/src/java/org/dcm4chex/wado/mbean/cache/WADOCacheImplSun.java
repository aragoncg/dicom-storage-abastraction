/**
 * 
 */
package org.dcm4chex.wado.mbean.cache;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGEncodeParam;
import com.sun.image.codec.jpeg.JPEGImageEncoder;


/**
 * @author gunter
 *
 */
class WADOCacheImplSun extends WADOCacheImpl {

	public void setImageWriterClass(String imageWriterClass) {
		if (imageWriterClass.equals(JPEGImageEncoder.class.getName())) {
			this.imageWriterClass = imageWriterClass;
		} else {
			super.setImageWriterClass(imageWriterClass);
		}
	}

	protected void createJPEG(BufferedImage bi, File file, float quality) 
			throws IOException {
		if (imageWriterClass.equals(JPEGImageEncoder.class.getName())) {
		    OutputStream out = new BufferedOutputStream(
		    		new FileOutputStream(file));
		    try {
		    	JPEGImageEncoder enc = JPEGCodec.createJPEGEncoder(out);
		    	try {
		    		JPEGEncodeParam jep = JPEGCodec.getDefaultJPEGEncodeParam(bi);
		    		jep.setQuality(quality, true);
					enc.setJPEGEncodeParam(jep);
		    	} catch (Exception e) {
		    		log.warn("Failed to set JPEG Encode Parameter: "
		    				+ e.getMessage()
		    				+  ". Use JPEG Encoder default quality");
		    	}
				enc.encode(bi);
		    } finally {
		    	out.close();
		    }	        	
		} else {
			super.createJPEG(bi, file, quality);
		}
	}
}

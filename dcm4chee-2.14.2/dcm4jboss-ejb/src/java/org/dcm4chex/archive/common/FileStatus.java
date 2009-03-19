/******************************************
 *                                        *
 *  dcm4che: A OpenSource DICOM Toolkit   *
 *                                        *
 *  Distributable under LGPL license.     *
 *  See terms of license at gnu.org.      *
 *                                        *
 ******************************************/

package org.dcm4chex.archive.common;

import java.util.Arrays;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 7713 $ $Date: 2008-10-22 17:26:37 +0200 (Wed, 22 Oct 2008) $
 * @since Nov 23, 2005
 */
public class FileStatus {

    private static final String[] ENUM = { 
    	"QUERY_HSM_FAILED",
    	"MD5_CHECK_FAILED",
    	"VERIFY_COMPRESS_FAILED", 
    	"COMPRESS_FAILED",
    	"DEFAULT", 
    	"TO_ARCHIVE",
    	"ARCHIVED",
        "COMPRESSING", 
    };

    public static final int QUERY_HSM_FAILED = -4;
    public static final int MD5_CHECK_FAILED = -3;
    public static final int VERIFY_COMPRESS_FAILED = -2;
    public static final int COMPRESS_FAILED = -1;
    public static final int DEFAULT = 0;    
    public static final int TO_ARCHIVE = 1;    
    public static final int ARCHIVED = 2;
    public static final int COMPRESSING = 3;
    
    private static final int OFFSET = -QUERY_HSM_FAILED;

    public static final String toString(int value) {
        return ENUM[OFFSET + value];
    }

    public static final int toInt(String s) {        
        final int index = Arrays.asList(ENUM).indexOf(s);
        if (index == -1)
            throw new IllegalArgumentException(s);
        return index - OFFSET;
    }
}

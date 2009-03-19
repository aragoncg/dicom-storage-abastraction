package org.dcm4chex.archive.perf;

import java.util.Arrays;

/**
 * DICOM performance counters
 * 
 * @author Fang Yang (fang.yang@agfa.com)
 * @version $Id: PerfCounterEnum.java 2955 2006-11-15 22:02:59Z fangatagfa $
 * @since Nov 13, 2006
 */
public class PerfCounterEnum {
	private static final String[] ENUM = { 
		// C_STORE_SCP
    	"C_STORE_SCP_OBJ_IN",
    	"C_STORE_SCP_OBJ_STORE",
    	"C_STORE_SCP_OBJ_REGISTER_DB",
    	
    	// C_FIND_SCP
    	"C_FIND_SCP_QUERY_DB",
    	"C_FIND_SCP_RESP_OUT",
    	
    	// C_MOVE_SCP
    	"C_MOVE_SCP_QUERY_DB",
    	
    	// C_STORE_SCU
    	"C_STORE_SCU_OBJ_OUT",
    	
    	// C_GET
    	// ...
    };

    public static final int C_STORE_SCP_OBJ_IN = 1;
    public static final int C_STORE_SCP_OBJ_STORE = 2;
    public static final int C_STORE_SCP_OBJ_REGISTER_DB = 3;
    
    public static final int C_FIND_SCP_QUERY_DB = 4;
    public static final int C_FIND_SCP_RESP_OUT = 5;
    
    public static final int C_MOVE_SCP_QUERY_DB = 6;
    
    public static final int C_STORE_SCU_OBJ_OUT = 7;
    
    public static final String toString(int value) {
       return ENUM[++value];
    }

    public static final int toInt(String s) {
    	final int index = Arrays.asList(ENUM).indexOf(s);
        if (index == -1)
            throw new IllegalArgumentException(s);
        return index;
    }
}

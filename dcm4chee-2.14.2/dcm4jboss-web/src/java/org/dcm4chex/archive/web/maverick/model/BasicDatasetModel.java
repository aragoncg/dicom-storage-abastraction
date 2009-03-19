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

package org.dcm4chex.archive.web.maverick.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.dict.Tags;
import org.dcm4che.util.DAFormat;
import org.dcm4cheri.util.StringUtils;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 3202 $ $Date: 2007-03-14 10:50:14 +0100 (Wed, 14 Mar 2007) $
 * @since 05.10.2004
 *
 */
public class BasicDatasetModel {

    public static final String DATE_FORMAT = "yyyy/MM/dd";

    public static final String DATETIME_FORMAT = "yyyy/MM/dd HH:mm:ss";

    private static final int DATETIME_FORMAT_LEN = DATETIME_FORMAT.length();

    private static DcmObjectFactory dof = DcmObjectFactory.getInstance();

    protected Dataset ds;
    
    protected BasicDatasetModel() {
        this.ds = dof.newDataset();
    }

    protected BasicDatasetModel(Dataset ds) {
        if (ds == null) throw new NullPointerException();
        this.ds = ds;
    }
    

    public String getSpecificCharacterSet() {
        return ds.getString(Tags.SpecificCharacterSet);
    }

    public void setSpecificCharacterSet(String value) {
        ds.putCS(Tags.SpecificCharacterSet, value);
    }

    public final Dataset toDataset() {
        return ds;
    }

    protected String getDate(int dateTag) {
        try {
	        final Date d = ds.getDate(dateTag);
	        return d == null ? null : new SimpleDateFormat(DATE_FORMAT).format(d);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    protected void setDate(int dateTag, String s) {
        if (s == null || s.length() == 0) {
            ds.putDA(dateTag);
        } else {
            try {
                Date date = new SimpleDateFormat(DATE_FORMAT).parse(s);
                ds.putDA(dateTag, date);
            } catch (ParseException e) {
                throw new IllegalArgumentException(s);
            }
        }
    }

    protected String getDateTime(int dateTag, int timeTag) {
        try {
	        final Date d = ds.getDateTime(dateTag, timeTag);
	        if (d == null) return null;
	        String s = new SimpleDateFormat(DATETIME_FORMAT).format(d);
	        return s;
	    } catch (IllegalArgumentException e) {
	        return null;
	    }
    }

    protected void setDateTime(int dateTag, int timeTag, String s) {
        int l;
        if (s == null || (l = s.length()) == 0) {
            ds.putDA(dateTag);
            ds.putTM(timeTag);
        } else {
            final int l1 = Math.min(l, DATETIME_FORMAT_LEN);
            final String f = DATETIME_FORMAT.substring(0, l1);
            try {
                Date date = new SimpleDateFormat(f).parse(s.substring(0, l1));
                ds.putDA(dateTag, date);
                if (l <= 13)
                    ds.putTM(timeTag);
                else
                    ds.putTM(timeTag, date);
            } catch (ParseException e) {
                throw new IllegalArgumentException(s);
            }
        }
    }

    protected String getDateRange(Dataset dsItem, int dateTag) {
        if ( dsItem == null ) dsItem = this.ds;
        try {
	        Date[] range = dsItem.getDateRange(dateTag);
	        if (range == null || range.length == 0)
	            return null;
	        SimpleDateFormat f = new SimpleDateFormat(DATE_FORMAT);
	        StringBuffer sb = new StringBuffer();
	        if (range[0] != null)
	            sb.append(f.format(range[0]));
	        sb.append('-');
	        if (range[1] != null)
	            sb.append(f.format(range[1]));
	        return sb.toString();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    protected void setDateRange(Dataset dsItem, int dateTag, String s) {
        if ( dsItem == null ) dsItem = this.ds;
        if (s == null || s.length() == 0) {
            dsItem.putDA(dateTag);
        } else {            
            String[] s2 = StringUtils.split(s, '-');
            DAFormat f = new DAFormat();
            String range = toDA(s2[0], false, f) + '-' + toDA(s2[s2.length-1], true, f);
            dsItem.putDA(dateTag, range);
	    }
	
	}

    private String toDA(String s, boolean end, DAFormat f) {
        if (s.length() == 0)
            return "";
        Calendar cal = Calendar.getInstance();
        cal.clear();
        String[] s3 = StringUtils.split(s, '/');
        if ( ( s3.length > 0 ) && ( s3[0].length() > 4) ) {
        	throw new NumberFormatException(s);
        }
        cal.set(Calendar.YEAR, Integer.parseInt(s3[0]));
        if (s3.length > 1 && s3[1].length() > 0) {
        	if ( s3[1].length() > 2 )
        	{
        		throw new NumberFormatException(s);
        	}
            cal.set(Calendar.MONTH, Integer.parseInt(s3[1])-1);
            if (s3.length > 2 && s3[2].length() > 0) {
            	if ( s3[2].length() > 2 )
            	{
            		throw new NumberFormatException(s);
            	}
               cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(s3[2]));
            } else {
                if (end) {
                    cal.add(Calendar.MONTH, 1);
                    cal.add(Calendar.DATE, -1);
                }
            }
        } else {
            if (end) {
                cal.add(Calendar.YEAR, 1);            
                cal.add(Calendar.DATE, -1);
            }
        }
        return f.format(cal.getTime());
    }
    
    public static String getDateString(Date d) {
        return new SimpleDateFormat(DATE_FORMAT).format(d);
    }
    public static String getDateString(long timeInMillis) {
        return getDateString(new Date(timeInMillis));
    }
}
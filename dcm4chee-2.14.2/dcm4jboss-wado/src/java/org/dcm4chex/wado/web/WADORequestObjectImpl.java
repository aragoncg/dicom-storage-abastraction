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

package org.dcm4chex.wado.web;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;

import org.dcm4che.data.Dataset;
import org.dcm4che.dict.Tags;
import org.dcm4cheri.util.StringUtils;
import org.dcm4chex.wado.common.WADORequestObject;

/**
 * @author franz.willer
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public class WADORequestObjectImpl extends BasicRequestObjectImpl implements
        WADORequestObject {

    private static final String ERROR_INVALID_REGION_FORMAT =
            "Error: region parameter is invalid! Must be a comma separated list of 4 decimal strings.";

    private static final String ERROR_INVALID_REGION_OUT_OF_RANGE =
            "Error: region parameter is invalid! Coordinates must be in range [0..1].";

    private static final String ERROR_INVALID_REGION_DIMENSION =
            "Error: region parameter is invalid! Width and height of specified region must be > 0.";

    private static final String ERROR_NULL_WINDOW_WIDTH =
            "Error: windowWidth parameter is invalid! Must specify a value.";

    private static final String ERROR_NULL_WINDOW_CENTER =
            "Error: windowCenter parameter is invalid! Must specify a value.";

    private static final String ERROR_INVALID_WINDOW_WIDTH_TYPE =
            "Error: windowWidth parameter is invalid! Must be a decimal string.";

    private static final String ERROR_INVALID_WINDOW_CENTER_TYPE =
            "Error: windowCenter parameter is invalid! Must be a decimal string.";

    private static final String ERROR_INVALID_WINDOW_WIDTH_VALUE =
            "Error: windowWidth parameter is invalid! Width must be > 0.";

    private static final String ERROR_INVALID_IMAGE_QUALITY_TYPE =
            "Error: imageQuality parameter is invalid! Must be a integer string.";

    private static final String ERROR_INVALID_IMAGE_QUALITY_VALUE =
            "Error: imageQuality parameter is invalid! Quality must be in range [1..100].";

    private String studyUID;

    private String seriesUID;

    private String instanceUID;

    private String rows;

    private String columns;

    private String frameNumber;

    private String transferSyntax;

    private String region;

    private String windowWidth;

    private String windowCenter;

    private String imageQuality;

    private List contentTypes;
    
    private Dataset objectInfo;

    /**
     * Creates a WADORequestObjectImpl instance configured with http request.
     * 
     * @param request
     *                The http request.
     */
    public WADORequestObjectImpl(HttpServletRequest request) {
        super(request);
        studyUID = request.getParameter("studyUID");
        seriesUID = request.getParameter("seriesUID");
        instanceUID = request.getParameter("objectUID");
        // optional parameters - implemented
        String contentType = request.getParameter("contentType");

        rows = request.getParameter("rows");
        columns = request.getParameter("columns");
        frameNumber = request.getParameter("frameNumber");
        transferSyntax = request.getParameter("transferSyntax");
        contentTypes = _string2List(contentType, ",");
        region = request.getParameter("region");
        windowWidth = request.getParameter("windowWidth");
        windowCenter = request.getParameter("windowCenter");
        imageQuality = request.getParameter("imageQuality");
    }

    /**
     * Returns the value of studyUID request parameter.
     * 
     * @see org.dcm4chex.wado.common.WADORequestObject#getStudyUID()
     * 
     * @return the studyUID.
     */
    public String getStudyUID() {
        return objectInfo == null ? studyUID : objectInfo.getString(Tags.StudyInstanceUID);
    }

    /**
     * Returns the value of seriesUID request parameter.
     * 
     * @see org.dcm4chex.wado.common.WADORequestObject#getSeriesUID()
     * 
     * @return the seriesUID.
     */
    public String getSeriesUID() {
        return objectInfo == null ? seriesUID : objectInfo.getString(Tags.SeriesInstanceUID);
    }

    /**
     * Returns the value of objectUID request parameter.
     * 
     * @see org.dcm4chex.wado.common.WADORequestObject#getObjectUID()
     * 
     * @return the objectUID
     */
    public String getObjectUID() {
        return instanceUID;
    }

    /**
     * Returns the value of rows request parameter.
     * 
     * @see org.dcm4chex.wado.common.WADORequestObject#getRows()
     * 
     * @return the rows parameter (integer String)
     */
    public String getRows() {
        return rows;
    }

    /**
     * Returns the value of columns request parameter.
     * 
     * @see org.dcm4chex.wado.common.WADORequestObject#getColumns()
     * 
     * @return the columns parameter (integer String)
     */
    public String getColumns() {
        return columns;
    }

    /**
     * Returns the value of frameNumber request parameter.
     * 
     * @see org.dcm4chex.wado.common.WADORequestObject#getFrameNumber()
     * 
     * @return the frameNumber (integer String)
     */
    public String getFrameNumber() {
        return frameNumber;
    }

    /**
     * Returns the list of requested content types from the contentType request
     * parameter.
     * <p>
     * The contentType param has one ore more content types seperated by ','
     * character.
     * 
     * @see org.dcm4chex.wado.common.WADORequestObject#getContentTypes()
     * 
     * @return A list of requested content types
     */
    public List getContentTypes() {
        return contentTypes;
    }

    /**
     * Returns the transferSyntax parameter.
     * 
     * @return Returns the transferSyntax.
     */
    public String getTransferSyntax() {
        return transferSyntax;
    }

    /**
     * @return Returns the value of the region parameter.
     */
    public String getRegion() {
        return region;
    }

    /**
     * @return Returns the value of the windowWidth parameter.
     */
    public String getWindowWidth() {
        return windowWidth;
    }

    /**
     * @return Returns the value of the windowCenter parameter.
     */
    public String getWindowCenter() {
        return windowCenter;
    }

    /**
     * @return Returns the value of the imageQuality parameter.
     */
    public String getImageQuality() {
        return imageQuality;
    }

    /**
     * Checks this request object and returns an error code.
     * <p>
     * <DL>
     * <DT>Following checks:</DT>
     * <DD> requestType must be "WADO"</DD>
     * <DD> studyUID, seriesUID and objectUID must be set</DD>
     * <DD> if rows is set: check if it is parseable to int</DD>
     * <DD> if columns is set: check if it is parseable to int</DD>
     * <DD> if frameNumber is set: check if it is parseable to int</DD>
     * </DL>
     * 
     * @return OK if it is a valid WADO request or an error code.
     */
    public int checkRequest() {
        if (getRequestType() == null
                || !"WADO".equalsIgnoreCase(getRequestType())
                || studyUID == null || seriesUID == null || instanceUID == null) {
            setErrorMsg("Not a WADO URL!");
            return INVALID_WADO_URL;
        }
        if (rows != null) {
            try {
                Integer.parseInt(rows);
            } catch (Exception x) {
                setErrorMsg("Error: rows parameter is invalid! Must be an integer string.");
                return INVALID_ROWS;
            }
        }
        if (columns != null) {
            try {
                Integer.parseInt(columns);
            } catch (Exception x) {
                setErrorMsg("Error: columns parameter is invalid! Must be an integer string.");
                return INVALID_COLUMNS;
            }
        }
        if (frameNumber != null) {
            try {
                Integer.parseInt(frameNumber);
            } catch (Exception x) {
                setErrorMsg("Error: frameNumber parameter is invalid! Must be an integer string.");
                return INVALID_FRAME_NUMBER;
            }
        }
        if (region != null) {
            try {
                checkRegion(region);
            } catch (IllegalArgumentException e) {
                setErrorMsg(e.getMessage());
                return INVALID_REGION;
            }
        }

        if (windowWidth != null || windowCenter != null) {
            try {
                checkWindowLevel(windowWidth, windowCenter);
            } catch (Exception x) {
                setErrorMsg(x.getMessage());
                return INVALID_WINDOW_LEVEL;
            }
        }

        if (imageQuality != null) {
            try {
                checkImageQuality(imageQuality);
            } catch (IllegalArgumentException e) {
                setErrorMsg(e.getMessage());
                return INVALID_IMAGE_QUALITY;
            }
        }

        setErrorMsg(null);
        return OK;
    }

/**
     * Checks that the region string's value is valid. Throws
     * <code>IllegalArgumentException</code> if it isn't.
     * 
     * @param region
     *                String representing a rectangular region of an image
     * 
     * @return void
     * 
     */
    private void checkRegion(String region) {
        String[] ss = StringUtils.split(region, ',');
        if (ss.length != 4) {
            throw new IllegalArgumentException(ERROR_INVALID_REGION_FORMAT);
        }
        double[] ds = new double[4];
        for (int i = 0; i < ds.length; i++) {
            try {
                ds[i] = Double.parseDouble(ss[i]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(ERROR_INVALID_REGION_FORMAT);
            }
            if (ds[i] < 0. || ds[i] > 1.) {
                throw new IllegalArgumentException(
                        ERROR_INVALID_REGION_OUT_OF_RANGE);
            }
        }
        if (!(ds[0] < ds[2] && ds[1] < ds[3])) {
            throw new IllegalArgumentException(ERROR_INVALID_REGION_DIMENSION);
        }
    }

    /**
     * Checks that the windowWidth & windowCenter values are valid. Throws
     * <code>IllegalArgumentException</code> if either isn't.
     * 
     * @param windowWidth
     *                The value of the windowWidth WADO parameter
     * @param windowCenter
     *                The value of the windowCenter WADO parameter
     * 
     * @return void
     * 
     */
    private void checkWindowLevel(String windowWidth, String windowCenter)
            throws IllegalArgumentException {
        if (windowWidth == null)
            throw new IllegalArgumentException(ERROR_NULL_WINDOW_WIDTH);

        if (windowCenter == null)
            throw new IllegalArgumentException(ERROR_NULL_WINDOW_CENTER);

        double width = -1;

        try {
            width = Double.parseDouble(windowWidth);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(ERROR_INVALID_WINDOW_WIDTH_TYPE);
        }

        if (width <= 0)
            throw new IllegalArgumentException(ERROR_INVALID_WINDOW_WIDTH_VALUE);

        try {
            Double.parseDouble(windowCenter);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(ERROR_INVALID_WINDOW_CENTER_TYPE);
        }
    }

    private void checkImageQuality(String imageQuality) {

        int quality = -1;

        try {
            quality = Integer.parseInt(imageQuality);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(ERROR_INVALID_IMAGE_QUALITY_TYPE);
        }

        if (quality <= 0 || quality > 100)
            throw new IllegalArgumentException(
                    ERROR_INVALID_IMAGE_QUALITY_VALUE);
    }

    /**
     * Seperate the given String with delim character and return a List of the
     * items.
     * 
     * @param s
     *                String with one or more items seperated with a character.
     * @param delim
     *                The delimiter charecter.
     * @return A List with the seperated items
     */
    private List _string2List(String s, String delim) {
        if (s == null)
            return null;
        StringTokenizer st = new StringTokenizer(s, delim);
        List l = new ArrayList();
        while (st.hasMoreTokens()) {
            l.add(st.nextToken().trim());
        }
        return l;
    }

    /**
     * Returns a short description of this request.
     * <p>
     * 
     * @return String representation of this request.
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("WADO request:");
        Iterator iter = paramMap.keySet().iterator();
        Object key;
        while (iter.hasNext()) {
            key = iter.next();
            sb.append("&").append(key).append("=").append(
                    ((String[]) paramMap.get(key))[0]);
        }
        return sb.toString();
    }

    public boolean isExcludePrivate() {
        return "no".equalsIgnoreCase(request.getParameter("privateTags"));
    }

    public String getSimpleFrameList() {
        return request.getParameter("simpleFrameList");
    }

    public String getCalculatedFrameList() {
        return request.getParameter("calculatedFrameList");
    }

    public Dataset getObjectInfo() {
        return objectInfo;
    }

    public void setObjectInfo(Dataset objectInfo) {
        this.objectInfo = objectInfo;
    }

}

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

package org.dcm4chex.wado.common;

import java.util.List;

import org.dcm4che.data.Dataset;

/**
 * @author franz.willer
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public interface WADORequestObject extends BasicRequestObject {

    public static final int OK = 0;

    public static final int INVALID_WADO_URL = -1;
    public static final int INVALID_ROWS = -2;
    public static final int INVALID_COLUMNS = -3;
    public static final int INVALID_FRAME_NUMBER = -4;
    public static final int INVALID_CONTENT_TYPE = -5;
    public static final int INVALID_TRANSFER_SYNTAX = -6;
    public static final int INVALID_REGION = -7;
    public static final int INVALID_WINDOW_LEVEL = -8;
    public static final int INVALID_IMAGE_QUALITY = -9;

    /**
     * Returns the studyUID parameter of the http request.
     * 
     * @return studyUID
     */
    String getStudyUID();

    /**
     * Returns the seriesUID parameter of the http request.
     * 
     * @return seriesUID
     */
    String getSeriesUID();

    /**
     * Returns the objectUID parameter of the http request.
     * 
     * @return objectUID
     */
    String getObjectUID();

    /**
     * Returns the rows parameter of the http request.
     * 
     * @return rows
     */
    String getRows();

    /**
     * Returns the columns parameter of the http request.
     * 
     * @return columns
     */
    String getColumns();

    /**
     * Returns the frameNumber parameter of the http request.
     * 
     * @return frame number as String
     */
    String getFrameNumber();

    /**
     * Returns a list of content types as defined via the contentType http
     * param.
     * 
     * @return requestType
     */
    List getContentTypes();

    /**
     * Returns the transferSyntax parameter of the http request.
     * 
     * @return transferSyntax
     */
    String getTransferSyntax();

    /**
     * @return Returns the region.
     */
    String getRegion();

    /**
     * @return Returns the value of the windowWidth parameter.
     */
    String getWindowWidth();

    /**
     * @return Returns the value of the windowCenter parameter.
     */
    String getWindowCenter();

    /**
     * @return Returns the value of the imageQuality parameter.
     */
    String getImageQuality();

    boolean isExcludePrivate();

    String getSimpleFrameList();

    String getCalculatedFrameList();
    
    void setObjectInfo(Dataset ds);
    Dataset getObjectInfo();

}

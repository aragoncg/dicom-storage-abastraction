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

package org.dcm4chex.rid.web;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.dcm4che.util.ISO8601DateFormat;
import org.dcm4chex.rid.common.RIDRequestObject;
import org.dcm4chex.rid.mbean.RIDSupport;

/**
 * @author franz.willer
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class RIDInfoRequestObject extends BasicRequestObjectImpl implements
RIDRequestObject {

    private String patientID;
    private String mostRecentResults;
    private String lowerDateTime;
    private String upperDateTime;
    private Map knownParams = new HashMap();

    private static List supportedInfoTypes = null;

    private static Logger log = Logger.getLogger( RIDInfoRequestObject.class.getName() );
    
    public RIDInfoRequestObject( HttpServletRequest request ) {
        super( request );
        patientID = request.getParameter( "patientID" );
        mostRecentResults = request.getParameter( "mostRecentResults" );
        lowerDateTime = request.getParameter( "lowerDateTime" );
        upperDateTime = request.getParameter( "upperDateTime" );
        knownParams.put( "requestType", getRequestType() );
        knownParams.put( "patientID", patientID );
        knownParams.put( "mostRecentResults", mostRecentResults );
        knownParams.put( "lowerDateTime", lowerDateTime );
        knownParams.put( "upperDateTime", upperDateTime );
    }
    /**
     * @return Returns the lowerDateTime.
     */
    public String getLowerDateTime() {
        return lowerDateTime;
    }
    /**
     * @return Returns the mostRecentResults.
     */
    public String getMostRecentResults() {
        return mostRecentResults;
    }
    /**
     * @return Returns the patientID.
     */
    public String getPatientID() {
        return patientID;
    }
    /**
     * @return Returns the upperDateTime.
     */
    public String getUpperDateTime() {
        return upperDateTime;
    }
    /**
     * Returns the value of a 'known' http request parameter.
     * <p>
     * <DL>
     * <DT>Following parameter are 'known' in this request object:</DT>
     * <DD>requestType, patientID, mostRecentResults, lowerDateTime, upperDateTime</DD>
     * </DL>
     * 
     * @param paraName Name of request parameter.
     * 
     * @return value of param or null if param is not set or not known.
     */
    public String getParam(String paraName) {
        return (String)knownParams.get( paraName );
    }

    /* (non-Javadoc)
     * @see org.dcm4chex.rid.common.BasicRequestObject#checkRequest()
     */
    public int checkRequest() {
        if ( getRequestType() == null || patientID == null || mostRecentResults == null ) 
            return RIDRequestObject.INVALID_RID_URL; //required param missing!

        if ( ! getRequestType().startsWith( "SUMMARY") && ! getRequestType().startsWith( "LIST") )
            return RIDRequestObject.INVALID_RID_URL;//RID Information requestmust start either with SUMMARY or LIST

        if ( ! getSupportedInformationTypes().contains( getRequestType() ) )
            return RIDRequestObject.RID_REQUEST_NOT_SUPPORTED;

        try {
            Integer.parseInt( this.mostRecentResults );
        } catch ( Exception x ) {
            return RIDRequestObject.INVALID_RID_URL;//not an integer string
        }

        if ( this.lowerDateTime != null ) {
            if ( lowerDateTime.trim().length() > 0 ) {
                try {
                    new ISO8601DateFormat().parse( lowerDateTime );			
                } catch ( Exception x ) {
                    return RIDRequestObject.INVALID_RID_URL;//invalid date/time string
                }
            } else {
                lowerDateTime = null;
            }
        }

        if ( this.upperDateTime != null ) {
            if ( upperDateTime.trim().length() > 0 ) {
                try {
                    new ISO8601DateFormat().parse( upperDateTime );			
                } catch ( Exception x ) {
                    return RIDRequestObject.INVALID_RID_URL;//invalid date/time string
                }
            } else {
                upperDateTime = null;
            }
        }

        return RIDRequestObject.SUMMERY_INFO;
    }
    /**
     * @return
     */
    public List getSupportedInformationTypes() {
        if ( supportedInfoTypes == null ) {
            supportedInfoTypes = new ArrayList();
            Field[] fields = RIDSupport.class.getFields();
            for ( Field f : fields ) {
                if ( f.getName().startsWith("SUMMARY") ) {
                    try {
                        supportedInfoTypes.add( f.get(null));
                        if (log.isDebugEnabled()) log.debug("----- "+f.get(null)+" added to SupportedInformationTypes!");
                    } catch (Exception e) {
                        if (log.isDebugEnabled()) log.error("Can not add "+f.getName()+" to SupportedInformationTypes!", e);
                    }
                }
            }

        }
        return supportedInfoTypes;
    }

}

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

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dcm4chex.rid.common.BasicRequestObject;
import org.dcm4chex.rid.common.RIDRequestObject;
import org.dcm4chex.rid.common.RIDResponseObject;

/**
 * @author franz.willer
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class RIDServlet extends HttpServlet {

    private static final int BUF_LEN = 65536;

    /**
     * Comment for <code>serialVersionUID</code>
     */
    private static final long serialVersionUID = 3258409538737483825L;
    private RIDServiceDelegate delegate;

    private static Logger log = Logger.getLogger( RIDServlet.class.getName() );


    /**
     * Initialize the RIDServiceDelegator.
     * <p>
     * Set the name of the MBean from servlet init param.
     */
    public void init() {
        delegate = new RIDServiceDelegate();
        delegate.getLogger().info("RIDServiceDelegate initialized");
        delegate.init( getServletConfig() );
    }

    /**
     * Handles the POST requset in the doGet method.
     * 
     * @param request 	The http request.
     * @param response	The http response.
     * @throws IOException
     */
    public void doPost( HttpServletRequest request, HttpServletResponse response ) throws IOException{
        doGet( request, response);
    }

    /**
     * Handles the GET requset.
     * 
     * @param request 	The http request.
     * @param response	The http response.
     * @throws IOException
     */
    public void doGet( HttpServletRequest request, HttpServletResponse response ) throws IOException{
        log.debug("RID URL:"+request.getRequestURI()+"?"+request.getQueryString());
        if ( request.getParameter("requestType") == null ) {
            handleMissingRequestType(request, response);
            return;
        }
        BasicRequestObject reqObj = RequestObjectFactory.getRequestObject( request );
        delegate.getLogger().info("doGet: reqObj:"+reqObj);
        int reqTypeCode = RIDRequestObject.INVALID_RID_URL;
        if ( reqObj != null ) {
            reqTypeCode = reqObj.checkRequest();
        }
        if ( reqTypeCode == RIDRequestObject.INVALID_RID_URL) {
            sendError( response, HttpServletResponse.SC_BAD_REQUEST, "Not a IHE RID URL!" );//missing or wrong request parameter
            return;
        }
        if ( reqTypeCode == RIDRequestObject.RID_REQUEST_NOT_SUPPORTED ) {
            sendError( response, HttpServletResponse.SC_NOT_FOUND, "This IHE RID request is not supported!" );
            return;
        } 
        RIDResponseObject resp = null; //use RIDResponseObject for encapsulate response.
        if ( reqTypeCode == RIDRequestObject.SUMMERY_INFO ) {
            resp = delegate.getRIDSummary( (RIDRequestObject) reqObj );
        } else if ( reqTypeCode == RIDRequestObject.DOCUMENT ) {
            resp = delegate.getRIDDocument( (RIDRequestObject) reqObj );
        } else {
            sendError( response, HttpServletResponse.SC_NOT_IMPLEMENTED, "This IHE RID request is not yet implemented!" );
            return;
        }
        if ( resp != null ) {
            int returnCode = resp.getReturnCode();
            delegate.getLogger().info("doGet: resp returnCode:"+returnCode);
            if ( returnCode == HttpServletResponse.SC_OK ) {
                sendResponse( response, resp );
            } else {
                sendError( response, returnCode, resp.getErrorMessage() );
            }
        } else {
            sendError( response, HttpServletResponse.SC_NOT_FOUND, "Not found!" );
        }
    }

    /**
     * @param request
     * @throws IOException
     */
    private void handleMissingRequestType(HttpServletRequest request, HttpServletResponse response ) throws IOException {
        if ("true".equals(this.getServletConfig().getInitParameter("allowShortURL") )) {
            StringBuffer sb = request.getRequestURL();
            sb.append("?requestType=").append(request.getParameter("RT"));
            sb.append("&documentUID=").append(request.getParameter("UID"));
            sb.append("&preferredContentType=").append(getFullContentType(request.getParameter("PCT")));
            log.info("redirect shortURL to "+sb);
            response.sendRedirect(sb.toString());
        }
    }

    /**
     * @param parameter
     * @return
     */
    private String getFullContentType(String parameter) {
        if ( parameter.indexOf('/') != -1 ) return parameter;
        if ( "pdf".equals(parameter) ) return "application/pdf";
        if ( "xml".equals(parameter) ) return "text/xml";
        if ( "dcm".equals(parameter) ) return "application/dicom";
        return parameter;
    }

    /**
     * Send an error response with given response code and message to the client.
     * <p>
     * It is recommended that this method is only called once per erequest!<br>
     * Otherwise a IllegalStateException will be thrown!
     * 
     * @param response	The HttpServletResponse of the request.
     * @param errCode	One of the response code defined in HttpServletResponse.
     * @param msg		A description for the reason to send the error.
     */
    private void sendError( HttpServletResponse response, int errCode, String msg ) {
        try {
            response.sendError( errCode, msg );
        } catch (IOException e) {
            //delegate.getLogger().error("Cant perform sendError( "+errCode+", "+msg+" )! reason:"+e.getMessage(), e );
        }
    }

    /**
     * Send the retrieved file to the client.
     * <p>
     * Sets the content type as defined in the RIDResponseObject object.
     * 
     * @param response
     * @param respObject
     */
    private void sendResponse( HttpServletResponse response, RIDResponseObject respObject ) {
        log.info("--- sendResponse started! :"+respObject);
        response.setHeader("Expires","0");//disables client side caching!!!
        try {
            if ( respObject != null ) {
                response.setContentType( respObject.getContentType() );
                long len = respObject.length();
                if ( len != -1 ) 
                    response.setContentLength((int)len);
                try {
                    delegate.getLogger().info("respObject execute");
                    //respObject.execute( System.out );
                    respObject.execute( response.getOutputStream() );
                    response.getOutputStream().close();
                } catch ( Exception e ) {
                    delegate.getLogger().error("Exception while writing RID response to client! reason:"+e.getMessage(), e );
                }

            }
        } catch ( Exception x ) {
            x.printStackTrace();
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, x.getMessage() );
        }
        log.info("--- sendResponse finished!");
    }


}

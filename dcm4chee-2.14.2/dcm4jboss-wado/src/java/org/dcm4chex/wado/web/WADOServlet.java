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

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dcm4chex.archive.util.RequestedFrameNumbersOutOfRangeException;
import org.dcm4chex.wado.common.BasicRequestObject;
import org.dcm4chex.wado.common.WADORequestObject;
import org.dcm4chex.wado.common.WADOResponseObject;

/**
 * @author franz.willer
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class WADOServlet extends HttpServlet {

	private static final int BUF_LEN = 65536;
	
	/** holds the WADOServiceDelegate instance */
    private static WADOServiceDelegate delegate;
	
    /** serialVersionUID because super class is serializable. */
	private static final long serialVersionUID = 3257008748022085682L;

	private static Logger log = Logger.getLogger( WADOServlet.class.getName() );
	
	/**
	 * Initialize the WADOServiceDelegator.
	 * <p>
	 * Set the name of the MBean from servlet init param.
	 */
	public void init() {
		delegate = new WADOServiceDelegate();
		delegate.init( getServletConfig() );
	}

	/**
	 * Handles the POST requset in the doGet method.
	 * 
	 * @param request 	The http request.
	 * @param response	The http response.
	 */
	public void doPost( HttpServletRequest request, HttpServletResponse response ){
		doGet( request, response);
	}

	/**
	 * Handles the GET requset.
	 * 
	 * @param request 	The http request.
	 * @param response	The http response.
	 */
	public void doGet( HttpServletRequest request, HttpServletResponse response ){
		log.debug("WADO URL:"+request.getRequestURI()+"?"+request.getQueryString());
		BasicRequestObject reqObject = RequestObjectFactory.getRequestObject( request );
		if ( reqObject == null || ! (reqObject instanceof WADORequestObject) ) {
			reqObject = RequestObjectFactory.getRequestObject( request );
			if ( reqObject == null ) {
				sendError( response, HttpServletResponse.SC_BAD_REQUEST, "Not A WADO URL" );
				return;
			}
		}
		int iErr = reqObject.checkRequest();
		if ( iErr < 0 ) {
			sendError( response, HttpServletResponse.SC_BAD_REQUEST, reqObject.getErrorMsg() );//required params missing or invalid!
			return;
		}
		WADOResponseObject respObject = delegate.getWADOObject( (WADORequestObject)reqObject );
		int returnCode = respObject.getReturnCode();
		if ( returnCode == HttpServletResponse.SC_OK ) {
			sendWADOFile( response, respObject );
		} else if ( returnCode == HttpServletResponse.SC_TEMPORARY_REDIRECT ) {
			try {
				response.sendRedirect( respObject.getErrorMessage() ); //error message contains redirect host.
			} catch (IOException e) {
				sendError( response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error: cant send redirect to client! Redirect to host "+respObject.getErrorMessage()+" failed!" );
			}
		} else {
			sendError( response, returnCode, respObject.getErrorMessage() );
		}
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
			log.error("Cant perform sendError( "+errCode+", "+msg+" )! reason:"+e.getMessage(), e );
		}
	}
	
	/**
	 * Send the retrieved file to the client.
	 * <p>
	 * Sets the content type as defined in the WADOResponseObject object.
	 * 
	 * @param response
	 * @param respObject
	 */
	private void sendWADOFile( HttpServletResponse response, WADOResponseObject respObject ) {
		response.setHeader("Expires","0");//disables client side caching!!!
		log.debug("sendResponse:"+respObject);
		try {
			if ( respObject != null ) {
				log.info("send WADO response: "+respObject.getContentType());
				response.setContentType( respObject.getContentType() );
				long len = respObject.length();
				if ( len != -1 ) 
					response.setContentLength((int)len);
				try {
					log.debug("respObject execute");
					respObject.execute( response.getOutputStream() );
					response.getOutputStream().close();
                                } catch ( RequestedFrameNumbersOutOfRangeException e ) {
                                        sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                                                "Error: Requested Frame Numbers Out of Range");
				} catch ( Exception e ) {
					log.error("Exception while writing WADO response to client! reason:"+e.getMessage(), e );
				}
				
			}
		} catch ( Exception x ) {
			x.printStackTrace();
			sendError(	response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, x.getMessage() );
		}
	}

}

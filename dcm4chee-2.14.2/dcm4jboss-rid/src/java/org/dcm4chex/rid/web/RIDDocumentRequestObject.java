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

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.dcm4chex.rid.common.RIDRequestObject;

/**
 * @author franz.willer
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class RIDDocumentRequestObject extends BasicRequestObjectImpl implements
		RIDRequestObject {

	private String documentUID;
	private String preferredContentType;
	private Map knownParams = new HashMap();
	
	public RIDDocumentRequestObject( HttpServletRequest request ) {
		super( request );
		documentUID = request.getParameter( "documentUID" );
		if ( documentUID == null && "DOCUMENT".equals(request.getParameter("RT"))) {
			documentUID = request.getParameter( "UID" );
			preferredContentType = request.getParameter( "PCT" );
		} else {
			preferredContentType = request.getParameter( "preferredContentType" );
		}
		
		knownParams.put("requestType", getRequestType() );
		knownParams.put("documentUID", documentUID);
		knownParams.put("preferredContentType", preferredContentType);
	}
	/**
	 * @return Returns the documentUID.
	 */
	public String getDocumentUID() {
		return documentUID;
	}
	/**
	 * @return Returns the preferredContentType.
	 */
	public String getPreferredContentType() {
		return preferredContentType;
	}
	
	/**
	 * Returns the value of a 'known' http request parameter.
	 * <p>
	 * <DL>
	 * <DT>Following parameter are 'known' in this request object:</DT>
	 * <DD>requestType, documentUID, preferredContentType</DD>
	 * </DL>
	 * 
	 * @param paraName Name of request parameter.
	 * 
	 * @return value of param or null if param is not set or not known.
	 */
	public String getParam(String paraName) {
		return (String) knownParams.get( paraName );
	}

	/* (non-Javadoc)
	 * @see org.dcm4chex.rid.common.BasicRequestObject#checkRequest()
	 */
	public int checkRequest() {
		if ( "DOCUMENT".equals( getRequestType() ) && documentUID != null && preferredContentType != null )
			return RIDRequestObject.DOCUMENT;
		else
			return RIDRequestObject.INVALID_RID_URL;
	}

}

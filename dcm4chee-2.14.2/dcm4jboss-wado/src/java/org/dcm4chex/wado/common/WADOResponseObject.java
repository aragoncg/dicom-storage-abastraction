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

import java.io.IOException;
import java.io.OutputStream;

import javax.xml.transform.TransformerConfigurationException;

import org.dcm4che.data.Dataset;
import org.xml.sax.SAXException;

/**
 * @author franz.willer
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public interface WADOResponseObject {
	
	/**
	 * The Command to execute this.
	 * 
	 * @return A command object to perform output process.
	 * @throws SAXException
	 * @throws TransformerConfigurationException
	 * @throws IOException
	 */
	void execute( OutputStream out ) throws TransformerConfigurationException, SAXException, IOException;
	
	/**
	 * Returns the contentLength or -1 (unknown) of this response. 
	 * @return
	 */
	long length();
	/**
	 * Returns the content type that should be set in the response.
	 * 
	 * @return a content type like 'image/jpeg'
	 */
	String getContentType();
	
	/**
	 * Returns the HTTP return code for the response.
	 * <p>
	 * This can be used to send an error back to the client.
	 * 
	 * @return An http return code.
	 */
	int getReturnCode();
	
	/**
	 * Returns an error message.
	 * <p>
	 * 
	 * @return An error message or null if return code is OK.
	 */
	String getErrorMessage();
    
    /**
     * Returns Dataset with patient info (if available)
     * 
     */
    Dataset getPatInfo();
}

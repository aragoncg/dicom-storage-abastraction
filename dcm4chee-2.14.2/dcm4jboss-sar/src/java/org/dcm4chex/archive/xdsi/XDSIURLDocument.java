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
package org.dcm4chex.archive.xdsi;

import java.net.URL;

import javax.activation.DataHandler;

/**
 * @author franz.willer@gwi-ag.com
 * @version $Revision: 3249 $ $Date: 2007-04-06 16:39:17 +0200 (Fri, 06 Apr 2007) $
 * @since Feb 15, 2006
 */
public class XDSIURLDocument extends XDSIDocument {

    private String uid;
	private URL url;
	
	public XDSIURLDocument( URL url, String mimeType, String docID, String uid ) {
        super(docID, mimeType);
		this.url = url;
        this.uid = uid;
	}
	
	/**
	 * @return Returns the docFile.
	 */
	public URL getURL() {
		return url;
	}
	
	public DataHandler getDataHandler() {
		return new DataHandler(url);
	}
    public String getUniqueID() {
        return uid;
    }
    
	public String toString() {
		return url+"|"+getMimeType()+"|"+getDocumentID();
	}
	
}

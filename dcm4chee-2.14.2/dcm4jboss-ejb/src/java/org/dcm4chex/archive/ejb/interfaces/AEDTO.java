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

package org.dcm4chex.archive.ejb.interfaces;

import java.io.Serializable;
import java.util.StringTokenizer;

/**
 * <description>
 *
 * @see <related>
 * @author  <a href="mailto:gunter@tiani.com">gunter zeilinger</a>
 * @version $Revision: 6844 $ $Date: 2008-08-22 14:23:10 +0200 (Fri, 22 Aug 2008) $
 * @since July 23, 2002
 *
 * <p><b>Revisions:</b>
 *
 * <p><b>yyyymmdd author:</b>
 * <ul>
 * <li> explicit fix description (no line numbers but methods) go
 *            beyond the cvs commit message
 * </ul>
 */
public class AEDTO implements Serializable {

    // Constants -----------------------------------------------------
    static final long serialVersionUID = 9128665077590256461L;
    static final String[] EMPTY_STRING_ARRAY = {
    };

    // Variables -----------------------------------------------------
    private final long pk;
    private String title;
    private final String hostname;
    private final int port;
    private final String cipherSuites;
    private final String issuer;
    private final String userID;
    private final String passwd;
    private final String fsGroupID;
    private final String desc;
    private final String wadoUrl;

    // Constructors --------------------------------------------------
    public AEDTO(
        long pk,
        String title,
        String hostname,
        int port,
        String cipherSuites,
        String issuer,
        String userID,
        String passwd,
        String fsGroupID,
        String desc, 
        String wadoUrl) {
        this.pk = pk;
        this.title = title;
        this.hostname = hostname;
        this.port = port;
        this.cipherSuites = cipherSuites;
        this.issuer = issuer;
        this.userID = userID;
        this.passwd = passwd;
        this.fsGroupID = fsGroupID;
        this.desc = desc;
        this.wadoUrl = wadoUrl;
    }

    /**
     * @return pk
     */
    public final long getPk() {
        return pk;
    }

    /** Getter for property title.
     * @return Value of property title.
     */
    public java.lang.String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }

    /** Getter for property host.
     * @return Value of property host.
     */
    public java.lang.String getHostName() {
        return hostname;
    }

    /** Getter for property port.
     * @return Value of property port.
     */
    public int getPort() {
        return port;
    }

    /** Getter for property cipherSuites.
     * @return Value of property cipherSuites.
     */
    public java.lang.String[] getCipherSuites() {
        if (cipherSuites == null || cipherSuites.length() == 0) {
            return EMPTY_STRING_ARRAY;
        }
        StringTokenizer stk = new StringTokenizer(cipherSuites, " ,");
        String[] retval = new String[stk.countTokens()];
        for (int i = 0; i < retval.length; ++i) {
            retval[i] = stk.nextToken();
        }
        return retval;
    }

    /** Getter for property cipherSuites.
     * @return Value of property cipherSuites.
     */
    public java.lang.String getCipherSuitesAsString() {
        return cipherSuites;
    }

    public boolean isTLS() {
        return cipherSuites != null && cipherSuites.length() != 0;
    }
    
    public String getIssuerOfPatientID() {
        return issuer;        
    }
    
    public String getUserID() {
        return userID;        
    }
    
    public String getPassword() {
        return passwd;        
    }
    
    public final String getFileSystemGroupID() {
        return fsGroupID;
    }
    
    public String getDescription() {
        return desc;        
    }
    
    public String getWadoUrl() {
        return wadoUrl;
    }

    public String toString() {
        return (isTLS() ? "dicom-tls://" : "dicom://")
            + title
            + '@'
            + hostname
            + ':'
            + port;
    }
}

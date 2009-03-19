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

package org.dcm4chex.rid.mbean;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.transform.TransformerConfigurationException;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

/**
 * @author franz.willer
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class RIDStreamResponseObjectImpl extends BasicRIDResponseObject {

    private InputStream stream;
    private static final int BUF_LEN = 65536;
    private long maxLen = -1L;

    private static Logger log = Logger.getLogger( RIDStreamResponseObjectImpl.class.getName() );
    
    public RIDStreamResponseObjectImpl( InputStream is, String contentType, int retCode, String errMsg ) {
        super(contentType,retCode,errMsg);
        this.stream = is;
    }
    public RIDStreamResponseObjectImpl( InputStream is, long maxLen, String contentType, int retCode, String errMsg ) {
        this( is, contentType,retCode,errMsg);
        this.maxLen = maxLen;
    }

    public long length() {
        return maxLen;
    }

    /* (non-Javadoc)
     * @see org.dcm4chex.rid.common.WADOResponseObject#getFile()
     */
    public void execute( OutputStream out ) throws TransformerConfigurationException, SAXException, IOException {
        if ( stream != null ) {
            InputStream in;
            if ( stream instanceof BufferedInputStream ) {
                in = stream;
            } else {
                in = new BufferedInputStream( stream, BUF_LEN );
            }
            byte[] buf = new byte[BUF_LEN];
            long remain = maxLen;
            boolean checkLen = maxLen > 0;
            try {
                int len = in.read( buf );
                while ( len > 0 ) {
                    if ( checkLen && len > remain ) {
                        len = (int)remain;
                        int idx = len; idx--;
                        if ( buf[idx] == 0) { //padding? 
                            log.debug( "PADDING! chang 0x0 to ' '!");
                            buf[idx] = ' ';
                        }
                        out.write( buf, 0, len );
                        if (log.isDebugEnabled()) log.debug("write last "+len+" bytes!");
                        break;
                    }
                    remain -= len;
                    if (log.isDebugEnabled()) log.debug("write junk of "+len+" bytes!");
                    out.write( buf, 0, len );
                    len = in.read( buf );
                }
            } catch ( IOException e ) {
                throw e;
            } finally {
                in.close();
                log.info("InputSTream closed!");
            }
        }
    }
}

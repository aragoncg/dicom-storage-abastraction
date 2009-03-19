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
 * ACG.
 * Portions created by the Initial Developer are Copyright (C) 2003-2005
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * Lin Yang <YangLin@cn-arg.com>
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.dcm4chex.archive.dcm.DBDataSource;
import org.dcm4chex.archive.dcm.qrscp.QrSCPDBImpl;
import org.dcm4chex.archive.util.FileDataSource;

/**
 * @author YangLin@cn-arg.com
 * @version 1.0
 * @since 02.03.2009
 * 
 * Override three methods in order to read content from database. 
 */
public class DBRIDSupport extends RIDSupport{
    
    private QrSCPDBImpl qri = null;
    
    private DBECGSupport ecgSupport = null;
    
    public DBRIDSupport(RIDService service) {
        super(service);
        qri = new QrSCPDBImpl();
    }
    
    protected InputStream createInputStream(File file) throws FileNotFoundException {
        InputStream is = null;       
        try {
            is = qri.getInputStream(file);
        } catch (Exception e) {
            throw new FileNotFoundException(e.toString());
        }  
        return is;    
    }
       
    protected FileDataSource createDataSource(MBeanServer server, 
             ObjectName fileSystemMgtName, String methodName, String docUID) throws Exception {
        DBDataSource dds = (DBDataSource) server.invoke(fileSystemMgtName, methodName, 
            new Object[] { docUID }, new String[] { String.class.getName() });
        dds.setQrSCPDBImpl(qri);
        return dds;
    }
   
    protected ECGSupport createECGSupport() {
        return getDBECGSupport();
    }
    
    private DBECGSupport getDBECGSupport() {
        if ( ecgSupport == null )
            ecgSupport = new DBECGSupport( this, qri );
        return ecgSupport;       
    }

}

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

package org.dcm4chex.archive.dcm;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;


/**
 * @author YangLin@cn-arg.com
 * @version 1.0
 * @since 01.04.2009
 * 
 * Configure the JDBC connection-related items according to some 
 * property file predefined.
 */
public class DBConUtil {
    
    public static boolean MANUAL_CON_FLAG;
    
    public static String CON_DRIVER;
    
    public static String CON_URL;
    
    public static String CON_USER;
    
    public static String CON_PW; 
    
    public static String DBSTORE_MARK;

    public static boolean saveToDB = readSaveOpt(); 
        

    private static boolean readSaveOpt() {
        boolean saveToDB = false;
        Properties props = new Properties();
        String key = "SaveTo";
        String defValue = "File";
        String filePath = (System.getProperty("user.home") + "//Dcm4chee//SaveOpt.properties")
                           .replace("//", File.separator);
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(filePath);
            props.load(fis);
            String value = props.getProperty(key).trim(); 
            if(!value.equals(defValue)) {
            	MANUAL_CON_FLAG = Boolean.parseBoolean(props.getProperty("Manual_Con_Flag").trim());
            	CON_DRIVER = props.getProperty("Con_Driver").trim();
            	CON_URL = props.getProperty("Con_Url").trim();
            	CON_USER = props.getProperty("Con_User").trim();
            	CON_PW = props.getProperty("Con_Password").trim();
            	DBSTORE_MARK = props.getProperty("DBStroe_Mark").trim();
                saveToDB = true;
            }          
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(fis != null)
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        return saveToDB;
    }

}

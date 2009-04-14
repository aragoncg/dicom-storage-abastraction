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

package org.dcm4chex.archive.factory;

import java.io.File;

import org.apache.log4j.Logger;
import org.dcm4che.data.Dataset;
import org.dcm4che.net.DataSource;
import org.dcm4chex.archive.dcm.DBDataSource;
import org.dcm4chex.archive.dcm.storescp.DBStoreScp;
import org.dcm4chex.archive.dcm.storescp.StoreScp;
import org.dcm4chex.archive.dcm.storescp.StoreScpService;
import org.dcm4chex.archive.util.DelSCPDBImpl;

/**
 * @author YangLin@cn-arg.com
 * @version 1.0
 * @since 02.25.2009
 * 
 * The factory class that provides DBStoreSCP singleton, 
 * DBDataSource instances, and record delete service. 
 */
public class DBScpFactory implements ScpFactory {

	private DBStoreScp dbStoreScp;
	
	private DelSCPDBImpl dsi; 
	
	/**
     * Construct a DBStoreScp instance.
     */
	public StoreScp getStoreScp(StoreScpService service) {
		if(dbStoreScp == null) {
			dbStoreScp = new DBStoreScp(service);
		}
		return dbStoreScp;
	}

	/**
     * Construct a DBDataSource instance.
     */
	public DataSource getDataSource(File file, Dataset mergeAttrs, int bufferSize) {
		return new DBDataSource(file, mergeAttrs, new byte[bufferSize]);
	}

	/**
     * Delete some OrdDicom record according to record Id.
     * Since deletion is done by some deamon thread, this
     * method is thread-safe. 
     */
	public boolean getFileDeleted(File file, Logger log, boolean deleteEmptyParents) {
		
		if (dsi == null) {
			dsi = new DelSCPDBImpl();
		}

		int number = 0;

		try {
			number = dsi.deleteDBFile(file);
		} catch (Exception e) {
			log.warn("Failed to delete file: " + file);
			return false;
		}

		if (number == 0) {
			log.warn("File: " + file + " was already deleted");
			return true;
		} 
     
	    if (deleteEmptyParents) {
			File parent = file.getParentFile();
			while (parent.delete()) {
				log.info("M-DELETE directory: " + parent);
				parent = parent.getParentFile();
			}
		}
	    
		return true;	
	}

}
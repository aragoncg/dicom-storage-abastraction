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
package org.dcm4chex.archive.web.maverick.admin.perm;

import javax.servlet.ServletConfig;

import org.apache.log4j.Logger;

/**
 * 
 * @author franz.willer@gwi-ag.com
 * @version $Revision: 2548 $ $Date: 2006-06-22 13:45:24 +0200 (Thu, 22 Jun 2006) $
 * @since 13.04.2006
 */
public abstract class FolderPermissionsFactory {
	
	private static Logger log = Logger.getLogger(FolderPermissionsFactory.class.getName());
	
	private static FolderPermissionsFactory instance = null;
	
	private String initParameter;
	public static FolderPermissionsFactory getInstance(ServletConfig cfg) {
		if ( instance != null ) return instance;
		String factoryClassName = cfg.getInitParameter("folderPermissionsFactory");
		if ( factoryClassName == null ) factoryClassName = "org.dcm4chex.archive.web.maverick.admin.perm.FolderPermissionsPropertyFactory";
		try {
			ClassLoader l = Thread.currentThread().getContextClassLoader();
			instance = (FolderPermissionsFactory) l.loadClass( factoryClassName ).newInstance();
			instance.setInitParameter( cfg.getInitParameter("folderPermissionsFactory_cfg") );
			instance.init();
			return instance;
		} catch (InstantiationException x) {
			log.error("Could not instantiate: "+factoryClassName, x);
			return new DummyPermissionFactory();
		} catch (IllegalAccessException x) {
			log.error("No access to instantiate factory: "+factoryClassName, x);
			return new DummyPermissionFactory();
		} catch (ClassNotFoundException x) {
			log.error("Class not found: "+factoryClassName, x);
			return new DummyPermissionFactory();
		}
	}
	
	public abstract void init();
	
	public abstract FolderPermissions getFolderPermissions(String userID);
	
	/**
	 * @return Returns the initParameter.
	 */
	public String getInitParameter() {
		return initParameter;
	}
	/**
	 * @param initParameter The initParameter to set.
	 */
	public void setInitParameter(String initParameter) {
		this.initParameter = initParameter;
	}
	/**
	 * Dummy implementation of FolderPermissionsFactory.
	 * <p>
	 * Returns always an empty FolderPermissions object!
	 * 
	 * @author franz.willer
	 *
	 */
	static class DummyPermissionFactory extends FolderPermissionsFactory {

		public void init() {}

		public FolderPermissions getFolderPermissions(String userID) {
			return new FolderPermissions();
		}
		
	}
}

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

package org.dcm4chex.archive.ejb.session;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.FinderException;
import javax.ejb.ObjectNotFoundException;
import javax.ejb.RemoveException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.dcm4chex.archive.ejb.interfaces.AEDTO;
import org.dcm4chex.archive.ejb.interfaces.AELocal;
import org.dcm4chex.archive.ejb.interfaces.AELocalHome;
import org.dcm4chex.archive.exceptions.UnknownAETException;

/**
 * 
 * @author <a href="mailto:umberto.cappellini@tiani.com">Umberto Cappellini</a>
 * @version $Revision: 6844 $ $Date: 2008-08-22 14:23:10 +0200 (Fri, 22 Aug 2008) $
 * @since 14.01.2004
 * 
 * @ejb.bean name="AEManager" type="Stateless" view-type="remote"
 *           jndi-name="ejb/AEManager"
 * 
 * @ejb.transaction-type type="Container"
 * 
 * @ejb.transaction type="Required"
 * 
 * @ejb.ejb-ref ejb-name="AE" view-type="local" ref-name="ejb/AE"
 */
public abstract class AEManagerBean implements SessionBean {

    private AELocalHome aeHome;

    private SessionContext ctx;

    private static final int MAX_MAX_CACHE_SIZE = 1000;
    private static int maxCacheSize = 20;
    private static Map aeCache = Collections.synchronizedMap(
            new LinkedHashMap(32, 0.75f, true) {

                private static final long serialVersionUID = -5477659896294241869L;

                protected boolean removeEldestEntry(Map.Entry eldest) {
                    return size() > maxCacheSize;
                }
            });
    
    public void setSessionContext(SessionContext ctx) throws EJBException,
            RemoteException {
        Context jndiCtx = null;
        try {
            jndiCtx = new InitialContext();
            aeHome = (AELocalHome) jndiCtx.lookup("java:comp/env/ejb/AE");
        } catch (NamingException e) {
            throw new EJBException(e);
        } finally {
            if (jndiCtx != null) {
                try {
                    jndiCtx.close();
                } catch (NamingException ignore) {
                }
            }
        }
        this.ctx = ctx;
    }

    public void unsetSessionContext() {
        aeHome = null;
        ctx = null;
    }

    /**
     * @ejb.interface-method
	 * @ejb.transaction type="Supports"
     */
    public int getCacheSize() {
        return AEManagerBean.aeCache.size();
    }

    /**
     * @ejb.interface-method
	 * @ejb.transaction type="Supports"
     */
    public int getMaxCacheSize() {
        return AEManagerBean.maxCacheSize;
    }

    /**
     * @ejb.interface-method
	 * @ejb.transaction type="Supports"
     */
    public void setMaxCacheSize(int maxCacheSize) {
    	if (maxCacheSize < 0 || maxCacheSize > MAX_MAX_CACHE_SIZE) {
    		throw new IllegalArgumentException("maxCacheSize: " + maxCacheSize);
    	}
        AEManagerBean.maxCacheSize = maxCacheSize;
    }

    /**
     * @ejb.interface-method
	 * @ejb.transaction type="Supports"
     */
    public void clearCache() {
        AEManagerBean.aeCache.clear();
    }

    /**
     * @throws FinderException 
     * @ejb.interface-method
     * @ejb.transaction type="Supports"
     */
    public AEDTO findByPrimaryKey(long aePk)
            throws FinderException {
        return aeHome.findByPrimaryKey(new Long(aePk)).toDTO();
    }

    /**
     * @throws FinderException 
     * @throws UnknownAETException 
     * @ejb.interface-method
     * @ejb.transaction type="Supports"
     */
    public AEDTO findByAET(String aet)
            throws FinderException, UnknownAETException {
    	AEDTO ae = (AEDTO) AEManagerBean.aeCache.get(aet);
    	if (ae == null) {
	        try {
	            ae = aeHome.findByAET(aet).toDTO();
	            AEManagerBean.aeCache.put(aet, ae);
	        } catch (ObjectNotFoundException e) {
	            throw new UnknownAETException(aet);
	        }
    	}
    	return ae;
    }

    /**
     * @throws FinderException 
     * @ejb.interface-method
     * @ejb.transaction type="Supports"
     */
    public List findAll() throws FinderException {
        ArrayList ret = new ArrayList();
        for (Iterator i = aeHome.findAll().iterator(); i.hasNext();) {
            ret.add(((AELocal) i.next()).toDTO());
        }
        return ret;
    }

    /**
     * @ejb.interface-method
     */
    public void updateAE(AEDTO modAE) throws FinderException {
        try {
            AELocal ae = aeHome.findByPrimaryKey(new Long(modAE.getPk()));
            AEManagerBean.aeCache.remove(ae.getTitle());
            ae.setTitle(modAE.getTitle());
            ae.setHostName(modAE.getHostName());
            ae.setPort(modAE.getPort());
            ae.setCipherSuites(modAE.getCipherSuitesAsString());
            ae.setIssuerOfPatientID(modAE.getIssuerOfPatientID());
            ae.setUserID(modAE.getUserID());
            ae.setPassword(modAE.getPassword());
            ae.setFileSystemGroupID(modAE.getFileSystemGroupID());
            ae.setDescription(modAE.getDescription());
            ae.setWadoUrl(modAE.getWadoUrl());
       } catch (FinderException e) {
            ctx.setRollbackOnly();
            throw e;
        }
    }

    /**
     * @ejb.interface-method
     */
    public void newAE(AEDTO newAE) throws CreateException {
        aeHome.create(
                newAE.getTitle(), 
                newAE.getHostName(),
                newAE.getPort(),
                newAE.getCipherSuitesAsString(),
                newAE.getIssuerOfPatientID(),
                newAE.getUserID(),
                newAE.getPassword(),
                newAE.getFileSystemGroupID(),
                newAE.getDescription(),
                newAE.getWadoUrl());
    }

    /**
     * @ejb.interface-method
     */
    public void removeAE(long aePk) throws Exception {
        try {
            aeHome.remove(new Long(aePk));
            AEManagerBean.aeCache.clear();
        } catch (RemoveException e) {
            throw new Exception(e);
        }
    }

}

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

package org.dcm4chex.archive.ejb.entity;

import javax.ejb.CreateException;
import javax.ejb.EntityBean;

import org.apache.log4j.Logger;
import org.dcm4chex.archive.ejb.interfaces.AEDTO;

/**
 * Application Entity bean.
 * 
 * @author <a href="mailto:gunter@tiani.com">Gunter Zeilinger</a>
 * 
 * @ejb.bean name="AE" type="CMP" view-type="local" primkey-field="pk"
 *           local-jndi-name="ejb/AE"
 * 
 * @ejb.transaction type="Required"
 * 
 * @ejb.persistence table-name="ae"
 * 
 * @jboss.entity-command name="hsqldb-fetch-key"
 * 
 * @ejb.finder 
 *      signature="Collection findAll()"
 *      query="SELECT OBJECT(a) FROM AE AS a" transaction-type="Supports"
 * @jboss.query
 *      signature="Collection findAll()"
 *      strategy="on-find"
 *      eager-load-group="*"
 * 
 * @ejb.finder
 *      signature="org.dcm4chex.archive.ejb.interfaces.AELocal findByAET(java.lang.String aet)"
 *      query="SELECT OBJECT(a) FROM AE AS a WHERE a.title = ?1"
 *      transaction-type="Supports"
 * @jboss.query
 *      signature="org.dcm4chex.archive.ejb.interfaces.AELocal findByAET(java.lang.String aet)"
 *      strategy="on-find"
 *      eager-load-group="*"
 *             
 * 
 */
public abstract class AEBean implements EntityBean {

    private static final Logger log = Logger.getLogger(AEBean.class);

    /**
     * Auto-generated Primary Key
     * 
     * @ejb.interface-method
     * @ejb.pk-field
     * @ejb.persistence column-name="pk"
     * @jboss.persistence auto-increment="true"
     * 
     */
    public abstract Long getPk();

    public abstract void getPk(Long pk);

    /**
     * Application Entity Title
     * 
     * @ejb.interface-method
     * @ejb.persistence column-name="aet"
     */
    public abstract String getTitle();

    /**
     * @ejb.interface-method
     */
    public abstract void setTitle(String title);

    /**
     * @ejb.interface-method
     * @ejb.persistence column-name="hostname"
     */
    public abstract String getHostName();

    /**
     * @ejb.interface-method
     */
    public abstract void setHostName(String name);

    /**
     * @ejb.interface-method
     * @ejb.persistence column-name="port"
     */
    public abstract int getPort();

    /**
     * @ejb.interface-method
     */
    public abstract void setPort(int port);

    /**
     * @ejb.interface-method
     * @ejb.persistence column-name="cipher_suites"
     */
    public abstract String getCipherSuites();

    /**
     * @ejb.interface-method
     */
    public abstract void setCipherSuites(String cipherSuites);
    
    /**
     * @ejb.interface-method
     * @ejb.persistence column-name="pat_id_issuer"
     */
    public abstract String getIssuerOfPatientID();

    /**
     * @ejb.interface-method
     */
    public abstract void setIssuerOfPatientID(String issuer);

    /**
     * @ejb.interface-method
     * @ejb.persistence column-name="user_id"
     */
    public abstract String getUserID();

    /**
     * @ejb.interface-method
     */
    public abstract void setUserID(String user);
    
    /**
     * @ejb.interface-method
     * @ejb.persistence column-name="passwd"
     */
    public abstract String getPassword();

    /**
     * @ejb.interface-method
     */
    public abstract void setPassword(String passwd);
        
    /**
     * @ejb.interface-method
     * @ejb.persistence column-name="fs_group_id"
     */
    public abstract String getFileSystemGroupID();

    /**
     * @ejb.interface-method
     */ 
    public abstract void setFileSystemGroupID(String id);
    
    /**
     * @ejb.interface-method
     * @ejb.persistence column-name="ae_desc"
     */
    public abstract String getDescription();

    /**
     * @ejb.interface-method
     */
    public abstract void setDescription(String desc);

    /**
     * @ejb.interface-method
     * @ejb.persistence column-name="wado_url"
     */
    public abstract String getWadoUrl();

    /**
     * @ejb.interface-method
     */
    public abstract void setWadoUrl(String desc);

    /**
     * @ejb.create-method
     */
    public Long ejbCreate(String title, String hostname, int port,
            String cipherSuites, String issuer, String user, String passwd,
            String fsGroupID, String desc, String wadoUrl)
            throws CreateException {
        if (log.isDebugEnabled()) {
            log.debug("create AEBean(" + title + ")");
        }
        setTitle(title);
        setHostName(hostname);
        setPort(port);
        setCipherSuites(cipherSuites);
        setIssuerOfPatientID(issuer);
        setUserID(user);
        setPassword(passwd);
        setFileSystemGroupID(fsGroupID);
        setDescription(desc);
        this.setWadoUrl(wadoUrl);
        return null;
    }

    public void ejbPostCreate(String title, String host, int port,
            String cipherSuites, String issuer, String user, String passwd,
            String fsGroupID, String desc, String wadoUrl)
            throws CreateException {
    }

    /**
     * @ejb.interface-method
     * @ejb.transaction type="Supports"
     */
    public AEDTO toDTO() {
        return new AEDTO(
                getPk().longValue(),
                getTitle(),
                getHostName(),
                getPort(),
                getCipherSuites(),
                getIssuerOfPatientID(),
                getUserID(),
                getPassword(),
                getFileSystemGroupID(),
                getDescription(),
                getWadoUrl());
    }
    
    /**
     * @ejb.interface-method
     * @ejb.transaction type="Supports"
     */
    public String asString() {
        StringBuffer sb = new StringBuffer(64);
        sb.append(getProtocol()).append("://").append(getTitle()).append('@')
                .append(getHostName()).append(':').append(getPort());
        return sb.toString();
    }

    private String getProtocol() {
        String cipherSuites = getCipherSuites();
        if (cipherSuites == null || cipherSuites.length() == 0) {
            return "dicom";
        }
        if ("SSL_RSA_WITH_NULL_SHA".equals(cipherSuites)) {
            return "dicom-tls.nodes";
        }
        if ("SSL_RSA_WITH_3DES_EDE_CBC_SHA".equals(cipherSuites)) {
            return "dicom-tls.3des";
        }
        if ("TLS_RSA_WITH_AES_128_CBC_SHA,SSL_RSA_WITH_3DES_EDE_CBC_SHA"
                .equals(cipherSuites)) {
            return "dicom-tls.aes";
        }
        return "dicom-tls";
    }
    
    
}

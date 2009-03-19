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

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 2754 $ $Date: 2006-09-14 10:27:39 +0200 (Thu, 14 Sep 2006) $
 * @since 27.12.2004
 * 
 * @ejb.bean
 * 	name="Device"
 * 	type="CMP"
 * 	view-type="local"
 * 	primkey-field="pk"
 * 	local-jndi-name="ejb/Device"
 * 
 * @ejb.transaction
 * 	type="Required"
 * 
 * @ejb.persistence
 * 	table-name="device"
 * 
 * @jboss.entity-command
 * 	name="hsqldb-fetch-key"
 * 
 * @ejb.finder
 * 	signature="Collection findAll()"
 * 	query="SELECT OBJECT(d) FROM Device AS d"
 * 	transaction-type="Supports"
 *
 * @ejb.finder
 * 	signature="org.dcm4chex.archive.ejb.interface.DeviceLocal findByStationName(java.lang.String name)"
 * 	query="SELECT OBJECT(d) FROM Device AS d WHERE d.stationName = ?1"
 *  transaction-type="Supports"
 *
 * @jboss.query
 * 	signature="org.dcm4chex.archive.ejb.interfaces.DeviceLocal findByStationName(java.lang.String name)"
 *  strategy="on-find"
 *  eager-load-group="*"
 *
 * @ejb.finder
 * 	signature="java.util.Collection findByProtocolCode( org.dcm4chex.archive.ejb.interfaces.CodeLocal code)"
 * 	query="SELECT DISTINCT OBJECT(d) FROM Device d, IN(d.protocolCodes) p WHERE p = ?1"
 *  transaction-type="Supports"
 * 
 * @jboss.query
 * 	signature="java.util.Collection findByProtocolCode(org.dcm4chex.archive.ejb.interfaces.CodeLocal code)"
 *  strategy="on-find"
 *  eager-load-group="*"
 *
 */
public abstract class DeviceBean implements EntityBean {
    
    private static final Logger log = Logger.getLogger(DeviceBean.class);

    /**
	 * @ejb.create-method
	 */
    public Long ejbCreate(String stationName, String aet, String md)
        throws CreateException
    {
        setStationName(stationName);
        setStationAET(aet);
        setModality(md);
        return null;
    }

    public void ejbPostCreate(String stationName, String aet, String md)
    	throws CreateException
    {
    }
    
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

    public abstract void setPk(Long pk);

    /**
	 * @ejb.interface-method
	 * @ejb.persistence column-name="station_name"
	 */
    public abstract String getStationName();
    public abstract void setStationName(String stationName);

    /**
	 * @ejb.interface-method
	 * @ejb.persistence column-name="station_aet"
	 */
    public abstract String getStationAET();
    public abstract void setStationAET(String aet);
    
    /**
     * @ejb.interface-method
     * @ejb.persistence column-name="modality"
     */
    public abstract String getModality();
    public abstract void setModality(String md);
    
    /**
     * @ejb.interface-method
     * @ejb.relation name="device-protocol-codes"
     * 	role-name="devices-for-protocol-code"
     *  target-ejb="Code"
     *  target-role-name="protocol-codes-for-device"
     *  target-multiple="yes"
     *
     * @jboss.relation-table table-name="rel_dev_proto"
     *
     * @jboss.relation
     *  fk-column="prcode_fk"
     *  related-pk-field="pk"     
     *
     * @jboss.target-relation
     *  fk-column="device_fk"
     *  related-pk-field="pk"     
     */
    public abstract java.util.Collection getProtocolCodes();
    
    /**
     * @ejb.interface-method
     */
    public abstract void setProtocolCodes(java.util.Collection codes);
    

}

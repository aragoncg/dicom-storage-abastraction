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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

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

import org.apache.log4j.Logger;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.dict.Tags;
import org.dcm4chex.archive.ejb.entity.CodeBean;
import org.dcm4chex.archive.ejb.interfaces.CodeLocal;
import org.dcm4chex.archive.ejb.interfaces.CodeLocalHome;
import org.dcm4chex.archive.ejb.interfaces.DeviceLocal;
import org.dcm4chex.archive.ejb.interfaces.DeviceLocalHome;

/**
 * 
 * @author franz.willer@tiani.com
 * @version $Revision: 2668 $ $Date: 2006-08-03 20:53:19 +0200 (Thu, 03 Aug 2006) $
 * @since 17.02.2005
 * 
 * @ejb.bean
 *  name="CodeToDeviceMapping"
 *  type="Stateless"
 *  view-type="remote"
 *  jndi-name="ejb/CodeToDeviceMapping"
 * 
 * @ejb.transaction-type 
 *  type="Container"
 * 
 * @ejb.transaction 
 *  type="Required"
 * 
 * @ejb.ejb-ref
 *  ejb-name="Code" 
 *  view-type="local"
 *  ref-name="ejb/Code" 
 * 
 * @ejb.ejb-ref
 *  ejb-name="Device" 
 *  view-type="local"
 *  ref-name="ejb/Device" 
 * 
 */
public abstract class CodeToDeviceMappingBean implements SessionBean {

    private CodeLocalHome codeHome;

    private DeviceLocalHome devHome;

    private static final Logger log = Logger.getLogger(CodeToDeviceMappingBean.class);

    public void setSessionContext(SessionContext arg0) throws EJBException,
            RemoteException {
        Context jndiCtx = null;
        try {
            jndiCtx = new InitialContext();
            codeHome = (CodeLocalHome) jndiCtx
                    .lookup("java:comp/env/ejb/Code");
            devHome = (DeviceLocalHome) jndiCtx
                    .lookup("java:comp/env/ejb/Device");
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
    }

    public void unsetSessionContext() {
        codeHome = null;
        devHome = null;
    }

    /**
     * @throws CreateException
     * @throws FinderException
     * @ejb.interface-method
     */
    public void createMapping( Dataset ds  ) throws CreateException, FinderException {
    	DcmElement spsSeq = ds.get( Tags.SPSSeq );
    	DcmElement codeSeq;
    	int len = spsSeq.countItems();
    	int lenCS;
    	Dataset ds1, dsCode;
    	for ( int i = 0 ; i < len ; i++ ) {
    		ds1 = spsSeq.getItem( i );
    		DeviceLocal dl = getDeviceLocal( ds1 );
    		List codes = new ArrayList();
    		codeSeq = ds1.get( Tags.ScheduledProtocolCodeSeq );
    		lenCS = codeSeq.countItems();
    		for ( int j = 0 ; j < lenCS ; j++ ) {
    			dsCode = codeSeq.getItem( j );
    	    	codes.add( CodeBean.valueOf( codeHome, dsCode ) );
    		}
    		dl.setProtocolCodes( codes );
    		log.info("Device "+dl.getStationName()+" mapped to protocols:"+codes);
     	}
    }

    /**
	 * @param ds1
	 * @return
     * @throws FinderException
     * @throws CreateException
	 */
	private DeviceLocal getDeviceLocal(Dataset ds1) throws FinderException, CreateException {
		String name = ds1.getString( Tags.ScheduledStationName );
		String aet = ds1.getString( Tags.ScheduledStationAET );
		String modality = ds1.getString( Tags.Modality );
		DeviceLocal dl = null;
		try {
			dl = devHome.findByStationName( name );
		} catch ( Exception x ) { //IGNORE
		}
		if ( dl == null ) {
			dl = devHome.create( name, aet, modality );
		} else {
			//delete mapping with this device ?
			//update aet and/or modality ?
		}
		return dl;
	}

	
	/**
	 * @throws FinderException
	 * @throws CreateException
	 * @ejb.interface-method
	 */
	public Dataset addScheduledStationInfo(Dataset ds) throws FinderException {
		DcmElement spsSeq = ds.get(Tags.SPSSeq);
		DcmElement codeSeq;
		int len = spsSeq.countItems();
		Dataset sps;
		for (int i = 0; i < len; i++) {
			sps = spsSeq.getItem(i);
			DeviceLocal device = lookupDevice(sps);
			if (device != null) {
				sps.putAE(Tags.ScheduledStationAET, device.getStationAET());
				sps.putSH(Tags.ScheduledStationName, device.getStationName());
				String reqModality = sps.getString(Tags.Modality);
				if (reqModality == null) {
					sps.putCS(Tags.Modality, device.getModality());
				} else if (!reqModality.equals(device.getModality())) {
					log.warn("Different Modality (" + reqModality
							+ ") in request and device (" + device.getModality()
							+ ") !!!");
				}
			}
		}
		return ds;
	}
    
	private DeviceLocal lookupDevice(Dataset sps) throws FinderException {
		String name = sps.getString(Tags.ScheduledStationName);
		if (name != null) {
			try {
				return devHome.findByStationName(name);
			} catch (ObjectNotFoundException ignore) {
				log.warn("Failed to find device with name: " + name);
			}
		}
		Dataset protocol = sps.getItem(Tags.ScheduledProtocolCodeSeq);
		if (protocol == null) {
			return null;			
		}
		String codeValue = protocol.getString(Tags.CodeValue);
		String codingScheme = protocol.getString(Tags.CodingSchemeDesignator);
		Collection col = codeHome.findByValueAndDesignator(codeValue,
				codingScheme);
		if (col.isEmpty()) {
			log.warn("Failed to find device for unkown Protocol Code: "
					+ codeValue + '^' + codingScheme);
			return null;
		}
		Collection col1 = devHome.findByProtocolCode((CodeLocal) col.iterator()
				.next());
		if (col1.isEmpty()) {
			log.warn("Failed to find device for Protocol Code: " + codeValue
					+ '^' + codingScheme);
			return null;
		}
		return (DeviceLocal) col1.iterator().next();
	}

	/**
	 * @throws FinderException
	 * @throws EJBException
	 * @throws RemoveException
	 * @ejb.interface-method
	 */
    public boolean deleteDevice( String stationName ) throws FinderException, EJBException, RemoveException {
    	DeviceLocal dl = devHome.findByStationName( stationName );
    	if ( dl == null ) return false;
    	deleteDevice( dl );
    	return true;
    }
    
    private void deleteDevice( DeviceLocal dl ) throws EJBException, RemoveException {
    	Collection col = new ArrayList();
    	col.addAll( dl.getProtocolCodes() ); //need independent collection!
    	String name = dl.getStationName();
    	log.info("Delete device:"+name);
    	dl.remove();
    	Iterator iter = col.iterator();
    	while ( iter.hasNext() ) {
    		CodeLocal code = (CodeLocal) iter.next();
    		log.info("Delete protocol code for device ("+name+"):"+
    					code.getCodeValue()+"("+code.getCodingSchemeDesignator()+")" );
        	try {
        		( code ).remove();
        	} catch ( Exception x ) { log.error("Cant delete code ('"+code.getCodeValue()+"')!!!"); }
    	}
    	
    	
    }
    
	/**
     * @throws FinderException
	 * @throws EJBException
	 * @throws RemoveException
	 * @ejb.interface-method
     */
    public boolean deleteMapping() throws FinderException, EJBException, RemoveException {
    	Collection col = devHome.findAll();
    	Iterator iter = col.iterator();
    	while ( iter.hasNext() ) {
    		deleteDevice( (DeviceLocal) iter.next() );
    	}
    	return true;
    }

}
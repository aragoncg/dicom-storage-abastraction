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

import java.util.Collection;
import java.util.Iterator;

import javax.ejb.EJBException;
import javax.ejb.FinderException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.log4j.Logger;
import org.dcm4che.data.Dataset;
import org.dcm4chex.archive.ejb.conf.AttributeFilter;
import org.dcm4chex.archive.ejb.interfaces.PatientLocal;
import org.dcm4chex.archive.ejb.interfaces.PatientLocalHome;

/**
 * FixPatientAttributes Bean
 * 
 * @ejb.bean
 *  name="FixPatientAttributes"
 *  type="Stateless"
 *  view-type="remote"
 *  jndi-name="ejb/FixPatientAttributes"
 * 
 * @ejb.transaction-type 
 *  type="Container"
 * 
 * @ejb.transaction 
 *  type="Required"
 * 
 * @ejb.ejb-ref
 *  ejb-name="Patient" 
 *  view-type="local"
 *  ref-name="ejb/Patient" 
 * 
 * @ejb.env-entry name="AttributeFilterConfigURL" type="java.lang.String"
 *                value="resource:dcm4chee-attribute-filter.xml"
 * 
 * @author <a href="mailto:franz.willer@gwi-ag.com">Franz Willer </a>
 * @version $Revision: 4952 $ $Date: 2007-09-04 16:42:41 +0200 (Tue, 04 Sep 2007) $
 *  
 */
public abstract class FixPatientAttributesBean implements SessionBean {

	private static Logger log = Logger.getLogger(FixPatientAttributesBean.class);

    private PatientLocalHome patHome;

    public void setSessionContext(SessionContext ctx) {
        Context jndiCtx = null;
        try {
            jndiCtx = new InitialContext();
            patHome = (PatientLocalHome) jndiCtx.lookup("java:comp/env/ejb/Patient");
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
        patHome = null;
    }
    

    /**
     * Check patient attributes.
     * <p>
     * 
     * @param offset first patient to check (paging)
     * @param limit  number of patients to check (paging)
     * @param doUpdate true will update patient record, false leave patient record unchanged.
     * 
     * @return int[2] containing number of 'fixed/toBeFixed' patient records
     *                and number of checked patient records
     * 
     * @throws FinderException
     * @ejb.interface-method
     */
    public int[] checkPatientAttributes(int offset, int limit, boolean doUpdate) throws FinderException {
    	Collection col = patHome.findAll(offset,limit);
    	if ( col.isEmpty() ) return null;
    	PatientLocal patient;
    	Dataset patAttrs, filtered;
    	int[] result = { 0, 0 };
        AttributeFilter filter = AttributeFilter.getPatientAttributeFilter();
    	for ( Iterator iter = col.iterator() ; iter.hasNext() ; result[1]++) {
			patient = (PatientLocal) iter.next();
			patAttrs = patient.getAttributes(false);
			filtered = filter.filter(patAttrs);
			if (patAttrs.size() > filtered.size()) {
			    log.warn("Detect Patient Record [pk= " + patient.getPk() +
			    		"] with non-patient attributes:");
				log.warn(patAttrs);
				if (doUpdate) {
				    patient.setAttributes(filtered);
				    log.warn(
						"Remove non-patient attributes from Patient Record [pk= "
							+ patient.getPk() + "]");
				}
				result[0]++;
			}
     	}
    	return result;
    }

}
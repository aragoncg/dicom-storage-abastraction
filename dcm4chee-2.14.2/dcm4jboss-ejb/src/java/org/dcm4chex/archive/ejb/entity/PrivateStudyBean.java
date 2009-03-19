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
import org.dcm4che.data.Dataset;
import org.dcm4che.dict.Tags;
import org.dcm4che.dict.UIDs;
import org.dcm4chex.archive.common.DatasetUtils;
import org.dcm4chex.archive.ejb.interfaces.PrivatePatientLocal;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 2754 $ $Date: 2006-09-14 10:27:39 +0200 (Thu, 14 Sep 2006) $
 * @since Dec 14, 2005
 * 
 * @ejb.bean name="PrivateStudy" type="CMP" view-type="local"
 *           local-jndi-name="ejb/PrivateStudy" primkey-field="pk"
 * @ejb.transaction type="Required"
 * @ejb.persistence table-name="priv_study"
 * @jboss.entity-command name="hsqldb-fetch-key"
 * 
 * @ejb.finder signature="java.util.Collection findByPrivateType(int privateType)"
 *             query="SELECT OBJECT(a) FROM PrivateStudy AS a WHERE a.privateType = ?1"
 *             transaction-type="Supports"
 * @ejb.finder signature="java.util.Collection findByStudyIuid(int privateType, java.lang.String uid)"
 *             query="SELECT OBJECT(a) FROM PrivateStudy AS a WHERE a.privateType = ?1 AND a.studyIuid = ?2"
 *             transaction-type="Supports"
 */
public abstract class PrivateStudyBean implements EntityBean {
    private static final Logger log = Logger.getLogger(PrivateStudyBean.class);
    /**
     * @ejb.create-method
     */
    public Long ejbCreate(int type, Dataset ds, PrivatePatientLocal patient)
    throws CreateException {
    	setPrivateType(type);
        setAttributes(ds);
        return null;
    }

    public void ejbPostCreate(int type, Dataset ds, PrivatePatientLocal patient)
    throws CreateException {
    	setPatient(patient);
    }
    
    /**
     * Auto-generated Primary Key
     *
     * @ejb.interface-method
     * @ejb.pk-field
     * @ejb.persistence column-name="pk"
     * @jboss.persistence auto-increment="true"
     */
    public abstract Long getPk();
    public abstract void setPk(Long pk);

    /**
     * @ejb.interface-method
     * @ejb.persistence column-name="priv_type"
     */
    public abstract int getPrivateType();
    public abstract void setPrivateType(int type);

 	/**
     * @ejb.persistence column-name="study_attrs"
     */
    public abstract byte[] getEncodedAttributes();
    public abstract void setEncodedAttributes(byte[] bytes);

    /**
     * @ejb.interface-method
     * @ejb.persistence column-name="study_iuid"
     */
    public abstract String getStudyIuid();
    public abstract void setStudyIuid(String uid);

    /**
	 * @ejb.interface-method
     * @ejb.persistence column-name="accession_no"
     */
    public abstract String getAccessionNumber();
    public abstract void setAccessionNumber(String acc_no);
    
    /**
     * @ejb.interface-method
     */
    public Dataset getAttributes() {
        Dataset ds = DatasetUtils.fromByteArray(getEncodedAttributes());
        return ds;
    }

    /**
     * @ejb.interface-method
     */
    public void setAttributes(Dataset ds) {
    	setStudyIuid(ds.getString(Tags.StudyInstanceUID));
    	setAccessionNumber(ds.getString(Tags.AccessionNumber));
        Dataset tmp = ds.excludePrivate();
        setEncodedAttributes(DatasetUtils.toByteArray(tmp));
    }


    /**
     * @ejb.interface-method
     * @ejb.relation name="priv-patient-study" role-name="priv-study-of-patient" cascade-delete="yes"
     * @jboss.relation fk-column="patient_fk" related-pk-field="pk"
     */
    public abstract PrivatePatientLocal getPatient();
    public abstract void setPatient(PrivatePatientLocal patient);

    /**
     * @ejb.interface-method view-type="local"
     * @ejb.relation name="priv-study-series" role-name="priv-study-has-series"
     */
    public abstract java.util.Collection getSeries();
    public abstract void setSeries(java.util.Collection series);

}

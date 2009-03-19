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

import java.util.Collection;
import java.util.Iterator;

import javax.ejb.CreateException;
import javax.ejb.EntityBean;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;

import org.apache.log4j.Logger;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.dict.Tags;
import org.dcm4chex.archive.ejb.interfaces.CodeLocal;
import org.dcm4chex.archive.ejb.interfaces.CodeLocalHome;

/**
 * @ejb.bean name="Code" type="CMP" view-type="local"
 * 	primkey-field="pk"
 * 	local-jndi-name="ejb/Code"
 * 
 * @ejb.transaction type="Required"
 * 
 * @ejb.persistence table-name="code"
 * 
 * @jboss.entity-command name="hsqldb-fetch-key"
 * 
 * @ejb.finder
 * 	signature="Collection findAll()"
 * 	query="SELECT OBJECT(a) FROM Code AS a"
 * 	transaction-type="Supports"
 * 
 * @ejb.finder
 * 	signature="java.util.Collection findByValueAndDesignator(java.lang.String value, java.lang.String designator)"
 * 	query="SELECT OBJECT(a) FROM Code AS a WHERE a.codeValue = ?1 AND a.codingSchemeDesignator = ?2"
 *  transaction-type="Supports"
 * 
 * @author <a href="mailto:gunter@tiani.com">Gunter Zeilinger </a>
 *  
 */
public abstract class CodeBean implements EntityBean {

    private static final Logger log = Logger.getLogger(CodeBean.class);

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
     * Code Value
     * 
     * @ejb.interface-method
     * @ejb.persistence column-name="code_value"
     */
    public abstract String getCodeValue();

    public abstract void setCodeValue(String value);

    /**
     * Code Value
     * 
     * @ejb.interface-method
     * @ejb.persistence column-name="code_designator"
     */
    public abstract String getCodingSchemeDesignator();

    public abstract void setCodingSchemeDesignator(String designator);

    /**
     * Code Value
     * 
     * @ejb.interface-method
     * @ejb.persistence column-name="code_version"
     */
    public abstract String getCodingSchemeVersion();

    public abstract void setCodingSchemeVersion(String version);

    /**
     * Code Value
     * 
     * @ejb.interface-method
     * @ejb.persistence column-name="code_meaning"
     */
    public abstract String getCodeMeaning();

    public abstract void setCodeMeaning(String meaning);

    /**
     * 
     * @ejb.interface-method
     */
    public String asString() {
        return prompt();
    }

    private String prompt() {
        return "Code[pk=" + getPk() + ", value=" + getCodeValue()
                + ", designator=" + getCodingSchemeDesignator() + ", version="
                + getCodingSchemeVersion() + ", meaning=" + getCodeMeaning()
                + "]";
    }

    /**
     * Create Media.
     * 
     * @ejb.create-method
     */
    public Long ejbCreate(String value, String designator, String version,
            String meaning) throws CreateException {
        setCodeValue(value);
        setCodingSchemeDesignator(designator);
        setCodingSchemeVersion(version);
        setCodeMeaning(meaning);
        return null;
    }

    public void ejbPostCreate(String value, String designator, String version,
            String meaning) throws CreateException {
        log.info("Created " + prompt());

    }

    public void ejbRemove() throws RemoveException {
        log.info("Deleting " + prompt());
    }

    public static CodeLocal valueOf(CodeLocalHome codeHome, Dataset item)
            throws CreateException, FinderException {
        if (item == null) return null;

        final String value = item.getString(Tags.CodeValue);
        final String designator = item.getString(Tags.CodingSchemeDesignator);
        final String version = item.getString(Tags.CodingSchemeVersion);
        final String meaning = item.getString(Tags.CodeMeaning);
        Collection c = codeHome.findByValueAndDesignator(value, designator);
        for (Iterator it = c.iterator(); it.hasNext();) {
            final CodeLocal code = (CodeLocal) it.next();
            if (version == null) { return code; }
            final String version2 = code.getCodingSchemeVersion();
            if (version2 == null || version2.equals(version)) { return code; }
        }
        return codeHome.create(value, designator, version, meaning);
    }
    
    public static void addCodesTo(CodeLocalHome codeHome, DcmElement sq, Collection c) 
    throws CreateException, FinderException {
    	if (sq == null || sq.isEmpty()) return;
        Dataset item = sq.getItem(0);
        if (item.isEmpty()) return;
        c.add(CodeBean.valueOf(codeHome, item));
    	for (int i = 1, n = sq.countItems(); i < n; i++) {
    		c.add(CodeBean.valueOf(codeHome, sq.getItem(i)));
    	}
    }
    
    public static boolean checkCodes(String prompt, DcmElement sq) {
        if (sq == null || sq.isEmpty())
            return true;
        for (int i = 0, n = sq.countItems(); i < n; i++) {
            Dataset item = sq.getItem(i);
            if (!item.containsValue(Tags.CodeValue)) {
                log.warn("Missing Code Value (0008,0100) in " + prompt
                        + " - ignore all items");
                return false;
            }
            if (!item.containsValue(Tags.CodingSchemeDesignator)) {
                log.warn("Missing Coding Scheme Designator (0008,0102) in "
                        + prompt + " - ignore all items");
                return false;
            }
            if (!item.containsValue(Tags.CodeMeaning)) {
                log.warn("Missing Code Meaning (0008,0104) in " + prompt
                        + " - ignore all items");
                return false;
            }
        }
        return true;
    }
}

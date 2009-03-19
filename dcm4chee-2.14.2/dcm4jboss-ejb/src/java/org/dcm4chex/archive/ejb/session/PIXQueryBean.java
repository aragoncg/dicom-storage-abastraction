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
 * Agfa-Gevaert Group.
 * Portions created by the Initial Developer are Copyright (C) 2003-2005
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * See @authors listed below.
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.ejb.EJBException;
import javax.ejb.FinderException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.dcm4chex.archive.ejb.interfaces.PatientLocal;
import org.dcm4chex.archive.ejb.interfaces.PatientLocalHome;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision: 3398 $ $Date: 2007-06-21 15:25:32 +0200 (Thu, 21 Jun 2007) $
 * @since Jun 21, 2007
 * 
 * @ejb.bean name="PIXQuery" type="Stateless" view-type="remote" jndi-name="ejb/PIXQuery"
 * @ejb.transaction-type type="Container"
 * @ejb.transaction type="Required"
 * @ejb.ejb-ref ejb-name="Patient" view-type="local" ref-name="ejb/Patient" 
 */
public abstract class PIXQueryBean implements SessionBean {
    
    private PatientLocalHome patHome;    

    public void setSessionContext(SessionContext ctx) throws RemoteException {
        Context jndiCtx = null;
        try {
            jndiCtx = new InitialContext();
            patHome = (PatientLocalHome) jndiCtx
                    .lookup("java:comp/env/ejb/Patient");
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
     * @ejb.interface-method
     */    
    public List queryCorrespondingPIDs(String patientID, String issuer,
            String[] domains) {
        try {
            Collection c = isWildCard(patientID) 
                ? patHome.findCorrespondingLike(toLIKE(patientID), issuer)
                : patHome.findCorresponding(patientID, issuer);
            return toPIDs(c, domains);
        } catch (FinderException e) {
            throw new EJBException(e);
        }
    }

    /**
     * @ejb.interface-method
     */    
    public List queryCorrespondingPIDsByPrimaryPatientID(String patientID,
            String issuer, String[] domains) {
        try {
            Collection c = isWildCard(patientID) 
                ? patHome.findCorrespondingByPrimaryPatientIDLike(toLIKE(patientID), issuer)
                : patHome.findCorrespondingByPrimaryPatientID(patientID, issuer);
            return toPIDs(c, domains);
        } catch (FinderException e) {
            throw new EJBException(e);
        }
    }    

    /**
     * @ejb.interface-method
     */    
    public List queryCorrespondingPIDsByOtherPatientID(String patientID,
            String issuer, String[] domains) {
        try {
            Collection c = isWildCard(patientID) 
                ? patHome.findCorrespondingByOtherPatientIDLike(toLIKE(patientID), issuer)
                : patHome.findCorrespondingByOtherPatientID(patientID, issuer);
            return toPIDs(c, domains);
        } catch (FinderException e) {
            throw new EJBException(e);
        }
    }    

    private ArrayList toPIDs(Collection pats, String[] domains) {
        ArrayList l = new ArrayList(pats.size());
        List domainList = domains != null ? Arrays.asList(domains) : null;
        for (Iterator iter = pats.iterator(); iter.hasNext();) {
            PatientLocal pat = (PatientLocal) iter.next();
            String iss = pat.getIssuerOfPatientId();
            if (domainList == null || domainList.contains(iss)) {
                l.add(new String[] { pat.getPatientId(), iss });
            }
        }
        return l;
    }
    
    private String toLIKE(String patientID) {
        StringBuffer sb = new StringBuffer(patientID);
        for (int i = 0; i < sb.length(); i++) {
            switch (sb.charAt(i)) {
                case '?' :
                    sb.setCharAt(i, '_');
                    break;
                case '*' :
                    sb.setCharAt(i, '%');
                    break;
                case '\\' :
                case '_' :
                case '%' :
                    sb.insert(i++, '\\');
                    break;
            }
        }
        return sb.toString();
    }

    private boolean isWildCard(String s) {
        return s.indexOf('*') != -1 || s.indexOf('?') != -1;
    }

}

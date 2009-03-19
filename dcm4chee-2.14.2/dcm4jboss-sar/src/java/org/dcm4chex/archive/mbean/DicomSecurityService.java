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
 * Java(TM), hosted at http://sourceforge.net/projects/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * Agfa-Gevaert N.V.
 * Portions created by the Initial Developer are Copyright (C) 2002-2005
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * See listed authors below.
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
package org.dcm4chex.archive.mbean;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.security.auth.Subject;

import org.dcm4che.net.AAssociateRJ;
import org.dcm4che.net.AAssociateRJException;
import org.dcm4che.net.AAssociateRQ;
import org.dcm4che.net.Association;
import org.dcm4che.net.AssociationFactory;
import org.dcm4che.net.UserIdentityAC;
import org.dcm4che.net.UserIdentityNegotiator;
import org.dcm4che.net.UserIdentityRQ;
import org.dcm4chex.archive.ejb.interfaces.AEDTO;
import org.dcm4chex.archive.ejb.interfaces.AEManager;
import org.dcm4chex.archive.ejb.interfaces.AEManagerHome;
import org.dcm4chex.archive.exceptions.ConfigurationException;
import org.dcm4chex.archive.exceptions.UnknownAETException;
import org.dcm4chex.archive.util.EJBHomeFactory;
import org.jboss.security.SimplePrincipal;
import org.jboss.security.plugins.JaasSecurityManager;
import org.jboss.system.ServiceMBeanSupport;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision$ $Date$
 * @since Aug 24, 2007
 */
public class DicomSecurityService extends ServiceMBeanSupport
        implements UserIdentityNegotiator {

    private JaasSecurityManager securityManager;   
    private String securityDomain;
    private String defUserID;
    private String defPassword;
    private boolean rejectIfNoUserIdentity;

    public final String getSecurityDomain() {
        return securityDomain;
    }

    public final void setSecurityDomain(String securityDomain) {
        if (securityDomain.equals(this.securityDomain)) {
            return;
        }
        this.securityDomain = securityDomain;
        if (super.getState() == STARTED) {
            initSecurityManager();
        }
    }
    
    public final boolean isRejectIfNoUserIdentity() {
        return rejectIfNoUserIdentity;
    }

    public final void setRejectIfNoUserIdentity(boolean reject) {
        this.rejectIfNoUserIdentity = reject;
    }

    private static String maskNull(String val, String def) {
        return val != null ? val : def;
    }

    private static String nullify(String val, String nullval) {
        String trim = val.trim();
        return trim.equals(nullval) ? null : trim;
    }

    public final String getDefaultUserID() {
        return maskNull(defUserID, "-");
    }

    public final void setDefaultUserID(String defUserID) {
        this.defUserID = nullify(defUserID, "-");
    }

    public final String getDefaultPassword() {
        return maskNull(defPassword, "-");
    }

    public final void setDefaultPassword(String defPassword) {
        this.defPassword = nullify(defPassword, "-");
    }

    protected void startService() throws Exception {
        initSecurityManager();
    }

    private void initSecurityManager() {
        InitialContext iniCtx = null;
        try {
            iniCtx = new InitialContext();
            securityManager = (JaasSecurityManager)
                iniCtx.lookup("java:/jaas/" + securityDomain);
        } catch (NamingException e) {
            throw new ConfigurationException(e);
        } finally {
            if (iniCtx != null) {
                try {
                    iniCtx.close();
                } catch (NamingException ignore) {}
            }
        }        
    }
    
    public UserIdentityNegotiator userIdentityNegotiator() {
        return this;
    }

    public UserIdentityAC negotiate(Association assoc)
            throws AAssociateRJException {
        String userId = null;
        String passwd = null;
        AAssociateRQ rq = assoc.getAAssociateRQ();
        UserIdentityRQ uidRQ = rq.getUserIdentity();
        if (uidRQ != null) {
            userId = uidRQ.getUsername();
            passwd = uidRQ.getPasscode();
        } else {
            try {
                AEDTO ae = aeMgr().findByAET(rq.getCallingAET());
                userId = ae.getUserID();
                passwd = ae.getPassword();
            } catch (UnknownAETException e) {
            } catch (Exception e) {
				throw new ConfigurationException(e);
			}
            if (userId == null || userId.length() == 0) {
                if (rejectIfNoUserIdentity) {
                    throw new AAssociateRJException(
                            AAssociateRJ.REJECTED_PERMANENT,
                            AAssociateRJ.SERVICE_PROVIDER_ACSE,
                            AAssociateRJ.NO_REASON_GIVEN);                   
                }
                if (defUserID == null) {
                    return null;
                }
                userId = defUserID;
                passwd = defPassword;
            }
        }
        Subject subject = new Subject();
        if (!isValid(userId, passwd, subject)) {
            throw new AAssociateRJException(
                    AAssociateRJ.REJECTED_PERMANENT,
                    AAssociateRJ.SERVICE_PROVIDER_ACSE,
                    AAssociateRJ.NO_REASON_GIVEN);
        }
        assoc.putProperty("user", subject);
        return uidRQ != null && uidRQ.isPositiveResponseRequested()
                ? AssociationFactory.getInstance().newUserIdentity() : null;
    }

    private AEManager aeMgr() throws Exception {
        AEManagerHome home = (AEManagerHome) EJBHomeFactory.getFactory()
                .lookup(AEManagerHome.class, AEManagerHome.JNDI_NAME);
        return home.create();
    }
    
    public boolean isValid(String userId, String passwd, Subject subject) {
        return securityManager.isValid(
                new SimplePrincipal(userId), passwd, subject);
    }
}

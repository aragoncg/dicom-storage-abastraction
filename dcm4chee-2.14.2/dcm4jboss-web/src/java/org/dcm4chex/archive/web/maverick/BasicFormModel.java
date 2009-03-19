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

package org.dcm4chex.archive.web.maverick;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.dcm4chex.archive.web.maverick.admin.DCMUser;

/**
 * @author franz.willer@gwi-ag.com
 * @version $Revision: 5517 $ $Date: 2007-11-23 12:47:20 +0100 (Fri, 23 Nov 2007) $
 * @since 13.02.2006
 */
public abstract class BasicFormModel {

    private static final String RESOURCE_BUNDLE_MESSAGES = "messages";

    private String currentUser;
    private final boolean admin;
    
    /** Popup message */
    private String popupMsg = null;
    /** externalPopup message (from a foreign controller) */
    private String externalPopupMsg = null;
    private Locale locale = Locale.ENGLISH;
    private ResourceBundle[] messages;

    protected Logger log = Logger.getLogger( getClass().getName() );
    
	protected BasicFormModel( HttpServletRequest request ) {
		currentUser = request.getUserPrincipal().getName();
    	admin = request.isUserInRole(DCMUser.WEBADMIN);
        messages = (ResourceBundle[]) request.getSession().getAttribute("dcm4chee-web-messages");
        if ( messages == null ) {
            messages = new ResourceBundle[]{ResourceBundle.getBundle(RESOURCE_BUNDLE_MESSAGES, locale)};
            request.getSession().setAttribute("dcm4chee-web-messages", messages);
        }
   }
	
	public String getModelName() { return "BASIC"; }

    protected String formatMessage(String key, Object[] args) {
        return MessageFormat.format(messages[0].getString(key), args);
    }

	/**
	 * @return Returns the currentUser.
	 */
	public String getCurrentUser() {
		return currentUser;
	}
	/**
	 * @return Returns the admin.
	 */
	public boolean isAdmin() {
		return admin;
	}
	
	/**
	 * Returns the popup message.
	 * <p>
	 * if popupMsg is null this Method returns the externalPopupMsg.
	 * @return Returns the popupMsg.
	 */
	public String getPopupMsg() {
		if ( popupMsg == null ) {
			popupMsg = externalPopupMsg;
			externalPopupMsg = null;
		}
		return popupMsg;
	}

    public void clearPopupMsg() {
        this.popupMsg = null;
    }
    public void setPopupMsg(String msgId, String arg) {
        setPopupMsg(msgId, new String[]{arg});
    }
    public void setPopupMsg(String msgId, String[] args) {
        this.popupMsg = msgId != null ? formatMessage(msgId, args) : null;
    }

	/**
	 * @return Returns the externalPopupMsg.
	 */
	public String getExternalPopupMsg() {
		return externalPopupMsg;
	}
	/**
	 * @param externalPopupMsg The externalPopupMsg to set.
	 */
	public void setExternalPopupMsg(String msgId, String[] args) {
		this.externalPopupMsg = formatMessage(msgId, args);
	}

    /**
     * @return the locale
     */
    public String getLanguage() {
        return locale.getLanguage();
    }

    /**
     * @param locale the locale to set
     */
    public void setLanguage(String arg) {
        if ( arg == null || arg.trim().length() < 2) return;
        Locale defaultLocale = Locale.getDefault();
        try {
            locale = new Locale(arg);
            Locale.setDefault(locale);
            messages[0] = ResourceBundle.getBundle(RESOURCE_BUNDLE_MESSAGES, locale);
        } finally {
            Locale.setDefault(defaultLocale);
        }
    }
    
    public Locale currentLocale() {
    	return locale;
    }
}
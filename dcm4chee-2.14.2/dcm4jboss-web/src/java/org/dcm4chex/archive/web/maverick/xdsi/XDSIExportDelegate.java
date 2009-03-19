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

package org.dcm4chex.archive.web.maverick.xdsi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.RuntimeMBeanException;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.dict.Tags;
import org.dcm4chex.archive.ejb.interfaces.ContentManager;
import org.dcm4chex.archive.ejb.interfaces.ContentManagerHome;
import org.dcm4chex.archive.util.EJBHomeFactory;
import org.dcm4chex.archive.web.maverick.util.CodeItem;
import org.infohazard.maverick.flow.ControllerContext;
import org.jboss.mx.util.MBeanServerLocator;

/**
 * @author franz.willer@gwi-ag.com
 * @version $Revision: 7709 $ $Date: 2008-10-22 16:41:47 +0200 (Wed, 22 Oct 2008) $
 */
public class XDSIExportDelegate {

    private static final String XDSI_DELEGATE_ATTR_NAME = "xdsiDelegate";

    private static MBeanServer server;
    private static ObjectName keyObjectServiceName;
    private static ObjectName xdsiServiceName;

    private static final DcmObjectFactory dof = DcmObjectFactory.getInstance();

    private static Logger log = Logger.getLogger( XDSIExportDelegate.class.getName() );



    public XDSIExportDelegate() {
    }

    public static final XDSIExportDelegate getInstance( ControllerContext ctx ) {
        HttpSession session = ctx.getRequest().getSession();
        XDSIExportDelegate delegate = (XDSIExportDelegate) session.getAttribute(XDSI_DELEGATE_ATTR_NAME);
        if ( delegate == null ) {
            delegate = new XDSIExportDelegate();
            try {
                delegate.init(ctx);
            } catch (Exception e) {
                throw new NullPointerException("Cant initialize XDSIExportDelegate!");
            }
            session.setAttribute(XDSI_DELEGATE_ATTR_NAME, delegate);
        }
        return delegate;
    }

    public void init(ControllerContext ctx) throws Exception {
        if (keyObjectServiceName != null) return;
        server = MBeanServerLocator.locate();
        String s = ctx.getServletConfig().getInitParameter("keyObjectServiceName");
        keyObjectServiceName = new ObjectName(s);
        s = ctx.getServletConfig().getInitParameter("xdsiServiceName");
        xdsiServiceName = new ObjectName(s);
    }

    public boolean exportXDSI(XDSIModel xdsiModel) throws Exception{
        if ( xdsiModel.getNumberOfInstances() < 1 ) {
            throw new IllegalArgumentException("No Instances selected!");
        }
        Collection items = getObserverContextItems(getAuthorPerson(xdsiModel.getUser()));
        Dataset rootInfo = getRootInfo(xdsiModel);
        List contentItems = getContentItems( xdsiModel );
        contentItems.addAll(items);
        try {
            Boolean b;
            if ( ! xdsiModel.isPdfExport() ) {
                Dataset keyObjectDS = getKeyObject( xdsiModel.getInstances(), rootInfo, contentItems);
                b = (Boolean) server.invoke(xdsiServiceName,
                        "sendSOAP",
                        new Object[] { keyObjectDS, xdsiModel.listMetadataProperties() },
                        new String[] { Dataset.class.getName(), Properties.class.getName() });
            } else {
                String docUID = (String) xdsiModel.getInstances().iterator().next();
                b = (Boolean) server.invoke(xdsiServiceName,
                        "exportPDF",
                        new Object[] { docUID, xdsiModel.listMetadataProperties() },
                        new String[] { String.class.getName(), Properties.class.getName() });
            }
            return b.booleanValue();
        } catch (Exception e) {
            log.warn("Failed to export Selection:", e);
            throw e;
        }
    }

    public boolean exportPDF(XDSIModel xdsiModel) throws Exception{
        if ( xdsiModel.getNumberOfInstances() < 1 ) {
            throw new IllegalArgumentException("No Instances selected!");
        }
        Collection items = getObserverContextItems(getAuthorPerson(xdsiModel.getUser()));
        Dataset rootInfo = getRootInfo(xdsiModel);
        List contentItems = getContentItems( xdsiModel );
        contentItems.addAll(items);
        String docUID = (String) xdsiModel.getInstances().iterator().next();
        try {
            Boolean b = (Boolean) server.invoke(xdsiServiceName,
                    "exportPDF",
                    new Object[] { docUID, xdsiModel.listMetadataProperties() },
                    new String[] { String.class.getName(), Properties.class.getName() });
            return b.booleanValue();
        } catch (Exception e) {
            log.warn("Failed to export Selection:", e);
            throw e;
        }
    }

    public boolean createFolder(XDSIModel model) throws Exception{
        try {
            Boolean b = (Boolean) server.invoke(xdsiServiceName,
                    "createFolder",
                    new Object[] { model.listMetadataProperties() },
                    new String[] { Properties.class.getName() });
            return b.booleanValue();
        } catch (Exception e) {
            log.warn("Failed to create Folder:", e);
            throw e;
        }
    }

    private Collection getObserverContextItems(String personName) {
        Dataset ds = dof.newDataset();
        ds.putCS(Tags.RelationshipType, "HAS OBS CONTEXT");
        ds.putCS(Tags.ValueType,"CODE");
        DcmElement cnSq = ds.putSQ(Tags.ConceptNameCodeSeq);
        Dataset cnDS = cnSq.addNewItem();
        cnDS.putSH(Tags.CodeValue, "121005");
        cnDS.putSH(Tags.CodingSchemeDesignator, "DCM");
        cnDS.putLO(Tags.CodeMeaning, "ObserverType");
        DcmElement ccSq = ds.putSQ(Tags.ConceptCodeSeq);
        Dataset ccDS = ccSq.addNewItem();
        ccDS.putSH(Tags.CodeValue, "121006");
        ccDS.putSH(Tags.CodingSchemeDesignator, "DCM");
        ccDS.putLO(Tags.CodeMeaning, "Person");

        Dataset ds1 = dof.newDataset();
        ds1.putCS(Tags.RelationshipType, "HAS OBS CONTEXT");
        ds1.putCS(Tags.ValueType,"PNAME");
        DcmElement cnSq1 = ds1.putSQ(Tags.ConceptNameCodeSeq);
        Dataset cnDS1 = cnSq1.addNewItem();
        cnDS1.putSH(Tags.CodeValue, "121008");
        cnDS1.putSH(Tags.CodingSchemeDesignator, "DCM");
        cnDS1.putLO(Tags.CodeMeaning, "Person Observer Name");
        ds1.putPN(Tags.PersonName, personName);
        ArrayList col = new ArrayList();
        col.add(ds);
        col.add(ds1);
        return col;
    }

    /**
     * @param user
     * @return
     */
    public String getAuthorPerson(String user) {
        try {
            return (String) server.invoke(xdsiServiceName,
                    "getAuthorPerson",
                    new Object[] {user},
                    new String[] {String.class.getName()});
        } catch (Exception e) {
            log.warn("Failed to get author person for user "+user+" ! Reason:"+ e.getCause());
            return null;
        }
    }

    /**
     * @param xdsiModel
     * @return
     */
    private List getContentItems(XDSIModel xdsiModel) {
        List items = new ArrayList();
        return items;
    }

    /**
     * @param xdsiModel
     * @return
     */
    private Dataset getRootInfo(XDSIModel xdsiModel) {
        Dataset rootInfo = DcmObjectFactory.getInstance().newDataset();
        DcmElement sq = rootInfo.putSQ(Tags.ConceptNameCodeSeq);
        Dataset item = sq.addNewItem();
        CodeItem selectedDocTitle = xdsiModel.selectedDocTitle();
        item.putSH(Tags.CodeValue,selectedDocTitle.getCodeValue());
        item.putSH(Tags.CodingSchemeDesignator,selectedDocTitle.getCodeDesignator());
        item.putLO(Tags.CodeMeaning, selectedDocTitle.getCodeMeaning());
        return rootInfo;
    }

    public Dataset getKeyObject(Collection iuids, Dataset rootInfo, List contentItems) {
        Object o = null;
        try {
            o = server.invoke(keyObjectServiceName,
                    "getKeyObject",
                    new Object[] { iuids, rootInfo, contentItems },
                    new String[] { Collection.class.getName(), Dataset.class.getName(), Collection.class.getName() });
        } catch (RuntimeMBeanException x) {
            log.warn("RuntimeException thrown in KeyObject Service:"+x.getCause());
            throw new IllegalArgumentException(x.getCause().getMessage());
        } catch (Exception e) {
            log.warn("Failed to create Key Object:", e);
            throw new IllegalArgumentException("Error: KeyObject Service cant create manifest Key Object! Reason:"+e.getClass().getName());
        }
        return (Dataset) o;
    }


    private ContentManager lookupContentManager() throws Exception {
        ContentManagerHome home = (ContentManagerHome) EJBHomeFactory
        .getFactory().lookup(ContentManagerHome.class,
                ContentManagerHome.JNDI_NAME);
        return home.create();
    }

    /**
     * @return
     */
    public CodeItem[] getConfiguredAuthorRoles() {
        return getCodeItems("listAuthorRoles");
    }

    public CodeItem[] getConfiguredClassCodes() {
        return getCodeItems("listClassCodes");
    }
    public CodeItem[] getConfiguredContentTypeCodes() {
        return getCodeItems("listContentTypeCodes");
    }
    public CodeItem[] getConfiguredHealthCareFacilityTypeCodes() {
        return getCodeItems("listHealthCareFacilityTypeCodes");
    }

    public CodeItem[] getConfiguredEventCodes() {
        return getCodeItems("listEventCodes");
    }

    public CodeItem[] getConfiguredDocTitles() {
        return getCodeItems("listDocTitleCodes");
    }
    public CodeItem[] getConfiguredConfidentialityCodes() {
        return getCodeItems("listConfidentialityCodes");
    }

    private CodeItem[] getCodeItems(String methodName) {
        try {
            List l = (List) server.invoke(xdsiServiceName,
                    methodName,
                    new Object[] {},
                    new String[] {});
            CodeItem[] items = new CodeItem[l.size()];
            for ( int i = 0, len = l.size() ; i < len ; i++ ) {
                items[i] = CodeItem.valueofDCM( l.get(i).toString());//DCM (D)esignator(C)odevalue(M)eaning
            }
            return items;
        } catch (Exception e) {
            log.error("Failed to get list of configured Codes! method:"+methodName, e);
            return null;
        }
    }

    /**
     * @return
     */
    public Properties joinMetadataProperties(Properties props) {
        try {
            return (Properties) server.invoke(xdsiServiceName,
                    "joinMetadataProperties",
                    new Object[] {props},
                    new String[] {Properties.class.getName()});
        } catch (Exception e) {
            log.error("Failed to get XDS-I Metadata Properties:", e);
            return null;
        }
    }

}

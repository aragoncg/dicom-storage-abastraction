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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.servlet.http.HttpSession;
import javax.xml.bind.JAXBElement;

import org.apache.log4j.Logger;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4chee.xds.infoset.v30.AdhocQueryResponse;
import org.dcm4chee.xds.infoset.v30.ExtrinsicObjectType;
import org.dcm4chee.xds.infoset.v30.IdentifiableType;
import org.dcm4chee.xds.infoset.v30.ObjectRefType;
import org.dcm4chee.xds.infoset.v30.RegistryObjectListType;
import org.dcm4chee.xds.infoset.v30.RegistryPackageType;
import org.dcm4chee.xds.infoset.v30.SlotType1;
import org.dcm4chee.xds.infoset.v30.util.InfoSetUtil;
import org.infohazard.maverick.flow.ControllerContext;
import org.jboss.mx.util.MBeanServerLocator;

/**
 * @author franz.willer@gwi-ag.com
 * @version $Revision: 7709 $ $Date: 2008-10-22 16:41:47 +0200 (Wed, 22 Oct 2008) $
 */
public class XDSQueryDelegate {

    private static MBeanServer server;
    private static ObjectName xdsQueryServiceName;

    private static final DcmObjectFactory dof = DcmObjectFactory.getInstance();

    private static Logger log = Logger.getLogger( XDSQueryDelegate.class.getName() );

    private static final String XDS_QUERY_DELEGATE_ATTR_NAME = "xdsQueryDelegate";

    private boolean useLeafFind = true;

    public XDSQueryDelegate() {
    }

    public static final XDSQueryDelegate getInstance(ControllerContext ctx) {
        HttpSession session = ctx.getRequest().getSession();
        XDSQueryDelegate delegate = (XDSQueryDelegate) session.getAttribute(XDS_QUERY_DELEGATE_ATTR_NAME);
        if ( delegate == null ) {
            delegate = new XDSQueryDelegate();
            try {
                delegate.init(ctx);
            } catch (Exception e) {
                throw new NullPointerException("Cant initialize XDSQueryDelegate!");
            }
            session.setAttribute(XDS_QUERY_DELEGATE_ATTR_NAME, delegate);
        }
        return delegate;
    }
    public void init(ControllerContext ctx) throws Exception {
        server = MBeanServerLocator.locate();
        String s = ctx.getServletConfig().getInitParameter("xdsQueryServiceName");
        xdsQueryServiceName = new ObjectName(s);
    }

    public void findDocuments(String patId, String issuer, XDSConsumerModel consumerModel) throws Exception {
        String patWithIssuer;
        if (issuer != null && issuer.length() > 0){
            patWithIssuer=patId+"^^^"+issuer;
        } else {
            patWithIssuer = patId;
        }

        AdhocQueryResponse resp = performFindXXX(patWithIssuer, "findDocuments", useLeafFind);
        List docs = useLeafFind ? buildList(resp) : ensureLeafResult(resp, "getDocuments");
        consumerModel.addDocuments(patId,docs);
    }

    public void findDocuments(XDSIModel model) throws Exception {
        String patId = model.getSourcePatientId();
        try {
            AdhocQueryResponse resp = performFindXXX(patId, "findDocuments", useLeafFind);
            List docs = useLeafFind ? buildList(resp) : ensureLeafResult(resp, "getDocuments");
            model.setDocuments(docs);
        } catch (Exception e) {
            log.warn("Failed to find documents for:"+patId, e);
            throw e;
        }
    }

    public void findFolders(String patId, XDSConsumerModel consumerModel) throws Exception {
        AdhocQueryResponse resp = performFindXXX(patId, "findFolders", true);
        List folders = ensureLeafResult(resp, "getFolders");
        consumerModel.addFolders(patId,folders);
    }
    public void findFolders(XDSIModel model) throws Exception{
        String patId = model.getSourcePatientId();
        try {
            AdhocQueryResponse resp = performFindXXX(patId, "findFolders",true);
            List folders = ensureLeafResult(resp, "getFolders");
            model.setFolders( folders );
        } catch (Exception e) {
            log.warn("Failed to find folders for:"+patId, e);
            throw e;
        }
    }

    private AdhocQueryResponse performFindXXX(String patId, String type, boolean useLeaf ) throws InstanceNotFoundException, MBeanException, ReflectionException {
        log.info("Perform (Find) Query "+type+" with patId:"+patId);
        AdhocQueryResponse resp = (AdhocQueryResponse) server.invoke(xdsQueryServiceName, type,
                new Object[] { patId, null, new Boolean(useLeaf) },
                new String[] { String.class.getName(), String.class.getName(), boolean.class.getName() });
        log.info("Query response:"+resp);
        if ( resp != null ) {
            log.info("Resp status:"+resp.getStatus() );
            log.info("Resp RegistryObjectList:"+resp.getRegistryObjectList() );
            log.info("Resp RegistryErrorList:"+resp.getRegistryErrorList() );
        }
        return resp;
    }

    private AdhocQueryResponse performGetXXX(List uuids, String type) throws InstanceNotFoundException, MBeanException, ReflectionException {
        log.info("Perform (Get) Query "+type+" with uuids:"+uuids);
        AdhocQueryResponse resp = (AdhocQueryResponse) server.invoke(xdsQueryServiceName, type,
                new Object[] { uuids },
                new String[] { List.class.getName() });
        log.info("Query response:"+resp);
        if ( resp != null ) {
            log.info("Resp status:"+resp.getStatus() );
            log.info("Resp RegistryObjectList:"+resp.getRegistryObjectList() );
            log.info("Resp RegistryErrorList:"+resp.getRegistryErrorList() );
        }
        return resp;
    }
    private List ensureLeafResult(AdhocQueryResponse resp, String methodName) throws Exception, InstanceNotFoundException, MBeanException, ReflectionException {
        List docs = buildList(resp);
        if ( !docs.isEmpty() && (docs.get(0) instanceof String) ) {
            log.info("List of uuids:"+docs);
            docs = buildList(performGetXXX(docs, methodName));
        }
        return docs;
    }

    private List buildList(AdhocQueryResponse resp) throws Exception {
        RegistryObjectListType objList = resp.getRegistryObjectList();
        List uuids = new ArrayList();
        List col = objList.getIdentifiable();
        if ( col == null || col.isEmpty() ) return uuids;
        Object o;
        for ( Iterator iter = col.iterator() ; iter.hasNext() ; ) {
            o = ((JAXBElement)iter.next()).getValue();
            log.info("add Entry to result list: "+o);
            if ( o instanceof String ) {
                uuids.add(o);
            } else if ( o instanceof ExtrinsicObjectType ) {
                uuids.add( new XDSDocumentObject((ExtrinsicObjectType)o));
            } else if ( o instanceof RegistryPackageType ) {
                uuids.add(new XDSFolderObject((RegistryPackageType)o));
            } else if ( o instanceof IdentifiableType ) { //ignore other entries of type Identifiable
            } else {
                log.info("Unexpected result type found ("+
                        o.getClass().getName()+")! (should be String (ObjectRef) or ExtrinsicObject (LeafClass))");
                throw new IllegalArgumentException("Unexpected result type found ("+
                        o.getClass().getName()+")! (should be String (ObjectRef) or ExtrinsicObject (LeafClass))");
            }
        }
        log.info("return uuids: "+uuids);
        return uuids;
    }


    public void setUseLeafFind(boolean b) {
        this.useLeafFind = b;
    }

    public void clearDocumentList(String patId, String issuer,
            XDSConsumerModel model) {
        List l = model.getDocuments(patId);
        if ( l != null ) 
            l.clear();

    }
}

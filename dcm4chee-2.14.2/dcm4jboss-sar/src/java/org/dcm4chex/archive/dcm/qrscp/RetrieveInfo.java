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

package org.dcm4chex.archive.dcm.qrscp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dcm4che.dict.UIDs;
import org.dcm4che.net.AAssociateRQ;
import org.dcm4che.net.AssociationFactory;
import org.dcm4chex.archive.common.Availability;
import org.dcm4chex.archive.ejb.jdbc.FileInfo;

/**
 * @author gunter.zeilinger@tiani.com
 * @version Revision $Date: 2008-11-18 14:58:47 +0100 (Tue, 18 Nov 2008) $
 * @since 23.03.2005
 */

final class RetrieveInfo {

    private static final String[] NATIVE_LE_TS = { UIDs.ExplicitVRLittleEndian,
        UIDs.ImplicitVRLittleEndian };
    
    private static final String[] NO_PIXEL_TS = { UIDs.NoPixelData };
    
    private static final String[] NO_PIXEL_DEFL_TS = { UIDs.NoPixelDataDeflate,
        UIDs.NoPixelData };
    
    private static final boolean isNativeLE_TS(String uid) {
        return UIDs.ExplicitVRLittleEndian.equals(uid)
            || UIDs.ImplicitVRLittleEndian.equals(uid);
    }

    private static class IuidsAndTsuids {
        final Set<String> iuids = new HashSet<String>();
        final Set<String> tsuids = new HashSet<String>();
    }
    
    private final int size;
    private final Map<String, IuidsAndTsuids> iuidsAndTsuidsByCuid =
            new HashMap<String, IuidsAndTsuids>();
    private final Map<String, List<FileInfo>> localFilesByIuid =
            new LinkedHashMap<String, List<FileInfo>>();
    private final Map<String, Set<String>> iuidsByRemoteAET =
            new HashMap<String, Set<String>>();
    private final Map<String, Set<String>> iuidsByExternalAET =
            new HashMap<String, Set<String>>();
    private final Set<String> notAvailableIuids = new HashSet<String>();
    private final Set<String> availableIuids = new HashSet<String>();
    private Map.Entry<String, Set<String>> curMoveForward;

    private boolean externalRetrieveAET;
    
    RetrieveInfo(QueryRetrieveScpService service, FileInfo[][] instInfos) {
        FileInfo[] fileInfos;
        FileInfo fileInfo;
        String iuid;
        this.size = instInfos.length;
        for (int i = 0; i < size; ++i) {
            fileInfos = instInfos[i];
            iuid = fileInfos[0].sopIUID;
            notAvailableIuids.add(iuid);
            for (int j = 0; j < fileInfos.length; j++) {
                fileInfo = fileInfos[j];
                if (fileInfo.fileRetrieveAET != null 
                        && (fileInfo.availability == Availability.ONLINE
                                || fileInfo.availability == Availability.NEARLINE)) {
                    if (service.isLocalRetrieveAET(fileInfo.fileRetrieveAET)) {
                        putLocalFile(fileInfo);
                    } else {
                        putIuid(iuidsByRemoteAET, fileInfo.fileRetrieveAET, iuid);
                    }
                } else if (fileInfo.extRetrieveAET != null) {
                    putIuid(iuidsByExternalAET, fileInfo.extRetrieveAET, iuid);
                }
            }
        }
    }

    private void putLocalFile(FileInfo fileInfo) {
        String iuid = fileInfo.sopIUID;
        String cuid = fileInfo.sopCUID;
        IuidsAndTsuids iuidsAndTsuids = iuidsAndTsuidsByCuid.get(cuid);
        if (iuidsAndTsuids == null) {
            iuidsAndTsuids = new IuidsAndTsuids();
            iuidsAndTsuidsByCuid.put(cuid, iuidsAndTsuids);
        }
        iuidsAndTsuids.iuids.add(iuid);
        iuidsAndTsuids.tsuids.add(fileInfo.tsUID);
        List<FileInfo> localFiles = localFilesByIuid.get(iuid);
        if (localFiles == null) {
            localFiles = new ArrayList<FileInfo>();
            localFilesByIuid.put(iuid, localFiles);
        }
        localFiles.add(fileInfo);
        availableIuids.add(iuid);
        notAvailableIuids.remove(iuid);
    }

    private void putIuid(Map<String, Set<String>> iuidsByAET, String aet,
            String iuid) {
        Set<String> iuids = iuidsByAET.get(aet);
        if (iuids == null) {
            iuids = new LinkedHashSet<String>();
            iuidsByAET.put(aet, iuids);
        }
        iuids.add(iuid);
        availableIuids.add(iuid);
        notAvailableIuids.remove(iuid);
    }
    
    public void addPresContext(AAssociateRQ rq,
            boolean sendWithDefaultTransferSyntax,
            boolean offerNoPixelData,
            boolean offerNoPixelDataDeflate) {
        String cuid;
        String tsuid;
        IuidsAndTsuids iuidsAndTsuids;
        AssociationFactory asf = AssociationFactory.getInstance();
        Iterator<Map.Entry<String, IuidsAndTsuids>> it =
                iuidsAndTsuidsByCuid.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, IuidsAndTsuids> entry = it.next();
            cuid = entry.getKey();
            iuidsAndTsuids = entry.getValue();
            if (sendWithDefaultTransferSyntax) {
                rq.addPresContext(asf.newPresContext(rq.nextPCID(), cuid, 
                        UIDs.ImplicitVRLittleEndian)); 
                continue;
            }
            rq.addPresContext(asf.newPresContext(rq.nextPCID(), cuid, 
                    NATIVE_LE_TS));
            if (offerNoPixelDataDeflate) {
                rq.addPresContext(asf.newPresContext(rq.nextPCID(), cuid, 
                        NO_PIXEL_DEFL_TS));
            } else if (offerNoPixelData) {
                rq.addPresContext(asf.newPresContext(rq.nextPCID(), cuid, 
                        NO_PIXEL_TS));
            }
            Iterator<String> it2 = iuidsAndTsuids.tsuids.iterator();
            while (it2.hasNext()) {
                tsuid = it2.next();
                if (!isNativeLE_TS(tsuid)) {
                    rq.addPresContext(asf.newPresContext(rq.nextPCID(), cuid, 
                            new String[] { tsuid }));
                }
            }
        }        
    }

    public Iterator<String> getCUIDs() {
        return iuidsAndTsuidsByCuid.keySet().iterator();
    }
    
    public Set<String> removeInstancesOfClass(String cuid) {
        IuidsAndTsuids iuidsAndTsuids = iuidsAndTsuidsByCuid.get(cuid);
        Iterator<String> it = iuidsAndTsuids.iuids.iterator();
        String iuid;
        while (it.hasNext()) {
            iuid = it.next();
            localFilesByIuid.remove(iuid);
            removeIuid(iuidsByRemoteAET, iuid);
            removeIuid(iuidsByExternalAET, iuid);
        }
        return iuidsAndTsuids.iuids;
    }

    private void removeIuid(Map<String, Set<String>> iuidsByAET, String iuid) {
        Iterator<Set<String>> it = iuidsByAET.values().iterator();
        Set<String> iuids;
        while (it.hasNext()) {
            iuids = it.next();
            iuids.remove(iuid);
            if (iuids.isEmpty())
                it.remove();
        }
    }

    private static final Comparator<Map.Entry<String, Set<String>>>
            ASC_IUIDS_SIZE = new Comparator<Map.Entry<String, Set<String>>>() {

        public int compare(Map.Entry<String, Set<String>> o1,
                Map.Entry<String, Set<String>> o2) {
            return o1.getValue().size() - o2.getValue().size();
        }
    };

    public boolean nextMoveForward() {
        if (!iuidsByRemoteAET.isEmpty()) {
            externalRetrieveAET = false;
            curMoveForward = removeNextRemoteAET(iuidsByRemoteAET);
            return true;
        }
        if (!iuidsByExternalAET.isEmpty()) {
            externalRetrieveAET = true;
            curMoveForward = removeNextRemoteAET(iuidsByExternalAET);
            return true;
        }
        curMoveForward = null;
        return false;
    }

    private Map.Entry<String, Set<String>>
            removeNextRemoteAET(Map<String, Set<String>> iuidsByAET) {
        Map.Entry<String, Set<String>> entry =
                Collections.max(iuidsByAET.entrySet(), ASC_IUIDS_SIZE);
        iuidsByAET.remove(entry.getKey());
        Set<String> iuids = entry.getValue();
        String iuid;
        Iterator<String> it = iuids.iterator();
        while (it.hasNext()) {
            iuid = it.next();
            removeIuid(iuidsByRemoteAET, iuid);
            removeIuid(iuidsByExternalAET, iuid);            
        }
        return entry;
    }

    public final Collection<List<FileInfo>> getLocalFiles() {
        return localFilesByIuid.values();
    }

    public final Set<String> removeLocalIUIDs() {
        Set<String> iuids = localFilesByIuid.keySet();
        for (Iterator<String> iter = iuids.iterator(); iter.hasNext();) {
            String iuid = iter.next();
            removeIuid(iuidsByRemoteAET, iuid);
            removeIuid(iuidsByExternalAET, iuid);
        }
        return iuids;
    }

    public final String getMoveForwardAET() {
        return curMoveForward != null ? curMoveForward.getKey() : null;
    }

    public final Set<String> getMoveForwardUIDs() {
        return curMoveForward != null ? curMoveForward.getValue() : null;
    }

    public final boolean isExternalRetrieveAET() {
        return externalRetrieveAET;
    }

    public final Set<String> getNotAvailableIUIDs() {
        return notAvailableIuids;
    }

    public final Set<String> getAvailableIUIDs() {
        return availableIuids;
    }

}

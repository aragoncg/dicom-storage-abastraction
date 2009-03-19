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

import java.util.Iterator;

import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.dict.Tags;
import org.dcm4che.net.DcmServiceException;
import org.dcm4che.util.UIDGenerator;
import org.jboss.logging.Logger;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 2642 $ $Date: 2006-07-25 11:45:57 +0200 (Tue, 25 Jul 2006) $
 * @since Feb 1, 2006
 */
class VMFBuilder {
    private static final int[] SHARED_FG = {
        Tags.SharedFunctionalGroupsSeq,
    };
	private static final int[] COMMON = {
		Tags.SOPClassUID,
		Tags.Rows,
		Tags.Columns,
		Tags.BitsAllocated,
		Tags.BitsStored,
		Tags.HighBit
	};
    
	private final Dataset result;
    private final Dataset sharedFGs;
    private final DcmElement perFrameFG;
    private final Dataset fgCfg;
	private final Dataset common;
	private int frames = 0;
    private final Logger log;
	
	public VMFBuilder(QueryRetrieveScpService service, Dataset firstFrame,
            Dataset cfg) {
        this.log = service.getLog();
		this.result = DcmObjectFactory.getInstance().newDataset();
		this.result.putAll(cfg.exclude(SHARED_FG));
		this.result.putAll(firstFrame.subSet(cfg));
        this.result.putUI(Tags.SOPInstanceUID, 
                UIDGenerator.getInstance().createUID());
        this.sharedFGs = result.putSQ(Tags.SharedFunctionalGroupsSeq).addNewItem();
        this.perFrameFG = result.putSQ(Tags.PerFrameFunctionalGroupsSeq);
        this.fgCfg = cfg.get(Tags.SharedFunctionalGroupsSeq).getItem();
		this.common = result.subSet(COMMON);
        addFrameInternal(firstFrame);
	}

	public void addFrame(Dataset frame) throws DcmServiceException {
		if (!frame.subSet(COMMON).equals(common))
			throw new DcmServiceException(0xC002, "Failed to pack series in MF.");
		addFrameInternal(frame);
	}

	private void addFrameInternal(Dataset frame)
    {
        if (log.isDebugEnabled())
            log.debug("Adding " + (frames + 1) + " frame.");
        boolean firstFrame = perFrameFG.isEmpty();
        Dataset frameFG = perFrameFG.addNewItem();
        Dataset refImg = frameFG.putSQ(Tags.RefImageSeq).addNewItem();
        refImg.putUI(Tags.RefSOPClassUID, frame.getString(Tags.SOPClassUID));
        refImg.putUI(Tags.RefSOPInstanceUID, frame.getString(Tags.SOPInstanceUID));
        for (Iterator iter = fgCfg.iterator(); iter.hasNext();)
        {
            DcmElement fgSq = (DcmElement) iter.next();
            Dataset fgFilter = fgSq.getItem();
            Dataset fg = frame.subSet(fgFilter);
            int fgSqTag = fgSq.tag();
            DcmElement frameFgSq = frameFG.putSQ(fgSqTag);
            DcmElement sharedFgSq = sharedFGs.get(fgSqTag);
            if (sharedFgSq != null) {
                Dataset sharedFg = sharedFgSq.getItem();
                if (sharedFg.equals(fg)) {
                    if (log.isDebugEnabled())
                        log.debug("Share Function group " + fgSq);
                    frameFgSq.addItem(sharedFg);
                    continue;
                }
                sharedFGs.remove(fgSqTag);
                if (log.isDebugEnabled())
                    log.debug("Stop shareing Function group " + fgSq);
            }
            Dataset newFg = frameFgSq.addNewItem();
            newFg.putAll(fg);
            if (firstFrame) {
                sharedFGs.putSQ(fgSqTag).addItem(newFg);
            }
        }
        frames++;
    }

    public Dataset getResult() {
        for (Iterator iter = sharedFGs.iterator(); iter.hasNext();)
        {
            DcmElement fgSq = (DcmElement) iter.next();
            int fgSqTag = fgSq.tag();
            for (int i = 0; i < frames; i++)
            {
                Dataset frameFG = perFrameFG.getItem(i);
                frameFG.remove(fgSqTag);
            }
        }
		result.putIS(Tags.NumberOfFrames, frames);
		return result;
	}

}

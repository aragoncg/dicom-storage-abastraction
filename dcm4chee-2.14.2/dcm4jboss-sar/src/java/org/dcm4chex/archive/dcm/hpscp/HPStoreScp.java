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

package org.dcm4chex.archive.dcm.hpscp;

import java.io.IOException;

import org.dcm4che.data.Command;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.dict.Status;
import org.dcm4che.dict.Tags;
import org.dcm4che.net.ActiveAssociation;
import org.dcm4che.net.DcmServiceBase;
import org.dcm4che.net.DcmServiceException;
import org.dcm4che.net.Dimse;
import org.dcm4chex.archive.ejb.interfaces.HPStorage;
import org.dcm4chex.archive.ejb.interfaces.HPStorageHome;
import org.dcm4chex.archive.util.EJBHomeFactory;
import org.dcm4chex.archive.util.HomeFactoryException;
import org.jboss.logging.Logger;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 5747 $ $Date: 2008-01-24 00:44:34 +0100 (Thu, 24 Jan 2008) $
 * @since Aug 17, 2005
 */
public class HPStoreScp extends DcmServiceBase {

	private final HPScpService service;

	private final Logger log;

	private static final int[] TYPE1_HP_ATTR = { Tags.HangingProtocolName,
			Tags.HangingProtocolDescription, Tags.HangingProtocolLevel,
			Tags.HangingProtocolCreator, Tags.HangingProtocolCreationDatetime,
			Tags.HangingProtocolDefinitionSeq, Tags.NumberOfPriorsReferenced,
			Tags.ImageSetsSeq, Tags.DisplaySetsSeq };

	private static final int[] TYPE1_IMAGESET_ATTR = {
			Tags.ImageSetSelectorSeq, Tags.TimeBasedImageSetsSeq };

	private static final int[] TYPE1_IMAGESET_SELECTOR_ATTR = {
			Tags.ImageSetSelectorUsageFlag, Tags.SelectorAttribute,
			Tags.SelectorAttributeVR };

	private static final int[] TYPE1_TIMEBASED_IMAGESET_ATTR = {
			Tags.ImageSetNumber, Tags.ImageSetSelectorCategory };

	private static final int[] TYPE1_DISPLAYSET_ATTR = { Tags.DisplaySetNumber,
			Tags.DisplaySetPresentationGroup, Tags.ImageSetNumber,
			Tags.ImageBoxesSeq };

	private static final int[] TYPE1_IMAGEBOX_ATTR = { Tags.ImageBoxNumber,
			Tags.DisplayEnvironmentSpatialPosition, Tags.ImageBoxLayoutType };

	public HPStoreScp(HPScpService service) {
		this.service = service;
		this.log = service.getLog();
	}

	protected void doCStore(ActiveAssociation activeAssoc, Dimse rq,
			Command rspCmd) throws IOException, DcmServiceException {
		Command rqCmd = rq.getCommand();
		String iuid = rqCmd.getAffectedSOPInstanceUID();
		String cuid = rqCmd.getAffectedSOPClassUID();
		Dataset hp = rq.getDataset();
		log.debug("Receive HP:\n");
		log.debug(hp);
		checkAttrs(hp, iuid, cuid);
		updateDB(hp);
	}

	private void updateDB(Dataset hp) throws DcmServiceException {
		try {
			HPStorage hpStorage = getStorageHome().create();
			try {
				hpStorage.store(hp);
			} catch (DcmServiceException e) {
				throw e;
			} finally {
				try {
					hpStorage.remove();
				} catch (Exception ignore) {
				}
			}
		} catch (Exception e) {
			throw new DcmServiceException(Status.ProcessingFailure, e);
		}		
	}

    private HPStorageHome getStorageHome() throws HomeFactoryException {
        return (HPStorageHome) EJBHomeFactory.getFactory().lookup(
                HPStorageHome.class, HPStorageHome.JNDI_NAME);
    }
	
	private void checkAttrs(Dataset hp, String iuid, String cuid)
			throws DcmServiceException {
		String s;
		if (!iuid.equals(s = hp.getString(Tags.SOPInstanceUID))) {
			datasetDoesNotMatchSOPClass("SOP Instance UID: " + s);
		}
		if (!cuid.equals(s = hp.getString(Tags.SOPClassUID))) {
			datasetDoesNotMatchSOPClass("SOP Class UID: " + s);
		}
		checkType1Attrs(hp, TYPE1_HP_ATTR, "Missing Type 1 Attribute ");
		DcmElement issq = hp.get(Tags.ImageSetsSeq);
		for (int i = 0, n = issq.countItems(); i < n; i++) {
			Dataset is = issq.getItem(i);
			checkType1Attrs(is, TYPE1_IMAGESET_ATTR,
					"Missing Type 1 Attribute (0072,0020)/");
			DcmElement isselsq = is.get(Tags.ImageSetSelectorSeq);
			for (int j = 0, m = isselsq.countItems(); j < m; j++) {
				checkType1Attrs(isselsq.getItem(j),
						TYPE1_IMAGESET_SELECTOR_ATTR,
						"Missing Type 1 Attribute (0072,0020)/(0072,0022)/");
			}
			DcmElement tbissq = is.get(Tags.TimeBasedImageSetsSeq);
			for (int j = 0, m = tbissq.countItems(); j < m; j++) {
				checkType1Attrs(tbissq.getItem(j),
						TYPE1_TIMEBASED_IMAGESET_ATTR,
						"Missing Type 1 Attribute (0072,0020)/(0072,0030)/");
			}
		}
		DcmElement dssq = hp.get(Tags.DisplaySetsSeq);
		for (int i = 0, n = dssq.countItems(); i < n; i++) {
			Dataset ds = dssq.getItem(i);
			checkType1Attrs(ds, TYPE1_DISPLAYSET_ATTR,
					"Missing Type 1 Attribute (0072,0200)/");
			DcmElement ibsq = ds.get(Tags.ImageBoxesSeq);
			for (int j = 0, m = ibsq.countItems(); j < m; j++) {
				checkType1Attrs(ibsq.getItem(j), TYPE1_IMAGEBOX_ATTR,
						"Missing Type 1 Attribute (0072,0200)/(0072,0300)/");
			}
		}
	}

	private void checkType1Attrs(Dataset attrs, int[] type1, String prompt)
			throws DcmServiceException {
		for (int i = 0; i < type1.length; ++i) {
			if (!attrs.containsValue(type1[i]))
				datasetDoesNotMatchSOPClass(prompt + Tags.toString(type1[i]));
		}
	}

	private void datasetDoesNotMatchSOPClass(String msg)
			throws DcmServiceException {
		throw new DcmServiceException(Status.DataSetDoesNotMatchSOPClassError,
				msg);

	}
}

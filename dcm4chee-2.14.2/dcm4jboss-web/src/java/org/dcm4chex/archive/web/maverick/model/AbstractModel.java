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

package org.dcm4chex.archive.web.maverick.model;

import java.nio.ByteBuffer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.dict.Tags;
import org.dcm4che.util.DAFormat;
import org.dcm4cheri.util.StringUtils;
import org.dcm4chex.archive.common.PrivateTags;
import org.dcm4chex.archive.util.Convert;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 3202 $ $Date: 2007-03-14 10:50:14 +0100 (Wed, 14 Mar 2007) $
 * @since 05.10.2004
 *
 */
public abstract class AbstractModel extends BasicDatasetModel {

    private List childs = new ArrayList();
    private List childsPK = null;

    protected AbstractModel() {
        super();
    }

    protected AbstractModel(Dataset ds) {
        super(ds);
    }
    
    public boolean update( Dataset dsNew ) {
        ByteBuffer bb = ds.getByteBuffer(PrivateTags.SeriesPk);
        long pk = bb == null ? -1 : Convert.toLong(bb.array());
        bb = ds.getByteBuffer(PrivateTags.SeriesPk);
        long pkNew = bb == null ? -1 : Convert.toLong(bb.array());
    	if ( pk != pkNew ) {
    		return false;
    	}
    	this.ds = dsNew;
    	ds.setPrivateCreatorID(PrivateTags.CreatorID);
    	return true;
    }

    /**
     * Returns the childs of this model as List.
     * 
     * @return List of childs.
     */
    public List listOfChilds() {
    	return childs;
    }
    
    /**
     * Set the childs for this model.
     * <p>
     * This method should not be used directly! Use dedicated method of implementation class instead.
     * <p>
     * If param is null, return without any changes!
     * <p>
     * Resets <code>childsPK</code> to obtain new child pk's with <code>containsPK</code> method.
     * 
     * @param list New list of childs.
     */
    public void setChilds( List list ) {
    	if ( list != null ) {
    		childs = list;
    		childsPK = null;
    	}
    }
    
    /**
     * Checks if this model contains given child.
     * 
     * @param model A childs model
     * @return true if this model contains given child.
     */
    public boolean contains( AbstractModel model ) {
    	return childs.contains( model );
    }
    
    /**
     * Checks if this model contains a child with given pk.
     * <p>
     * Use carefully, because pk's are only unique within a model type!
     * 
     * @param pk The childs pk
     * @return 
     */
    public boolean containsPK( Long pk ) {
    	return childPKs().contains( pk );
    }
    
    /**
     * Gets the list of all clients pk's.
     * <p>
     * This method is used primary in method <code>containsPK</code>
     * 
     * @return List of childs pk's.
     */
    public List childPKs(){
    	if ( childsPK != null ) return childsPK;
    	childsPK = new ArrayList();
    	Iterator iter = listOfChilds().iterator();
    	while ( iter.hasNext() ) {
    		childsPK.add( new Long( ((AbstractModel) iter.next() ).getPk() ) );
    	}
    	return childsPK;
    }
    
    /**
     * Get the pk of this model instance.
     * 
     * @return pk
     */
    public abstract long getPk();
    
}
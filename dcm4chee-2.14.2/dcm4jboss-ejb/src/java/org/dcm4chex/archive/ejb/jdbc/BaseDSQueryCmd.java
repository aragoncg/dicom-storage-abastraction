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

package org.dcm4chex.archive.ejb.jdbc;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;

import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.dict.Tags;
import org.dcm4che.dict.VRs;

/**
 * @author <a href="mailto:gunter@tiani.com">Gunter Zeilinger</a>
 * @version $Revision: 6070 $ $Date: 2008-02-20 16:21:57 +0100 (Wed, 20 Feb 2008) $
 */
public abstract class BaseDSQueryCmd extends BaseReadCmd {


    protected final Dataset keys;
    /** Contains all supported matching keys of the 'root' Dataset (have to include Seq.Tag if match is supported in SQ)  */
    protected final IntList matchingKeys= new IntList();
    /** Contains supported matching keys of sequence Items. key=SQ tag, value=list of supported tags) */
    protected final HashMap seqMatchingKeys = new HashMap();

    protected final SqlBuilder sqlBuilder = new SqlBuilder();

    protected final boolean filterResult;

    protected final boolean type2;

    protected BaseDSQueryCmd(Dataset keys, boolean filterResult,
            boolean noMatchForNoValue, int transactionIsolationLevel)
            throws SQLException {
        super(JdbcProperties.getInstance().getDataSource(),
                transactionIsolationLevel);
        this.keys = keys;
        this.filterResult = filterResult;
        this.type2 = noMatchForNoValue ? SqlBuilder.TYPE1 : SqlBuilder.TYPE2;
    }


    public void execute() throws SQLException {
        execute(sqlBuilder.getSql());
    }
    
    public boolean isMatchNotSupported() {
        return sqlBuilder.isMatchNotSupported();
    }
    
    /**
     * Check if this QueryCmd use an unsupported matching key.
     * 
     * @return true if an unsupported matching key is found!
     */
    public boolean isMatchingKeyNotSupported() {
        return findUnsupportedMatchingKey(keys, matchingKeys);
    }

    /**
     * Search for unsupported Matching key.
     * <p>
     * Returns true if a key with value (a matching key) is found that is not supported.
     * <p>
     * <code>matchingKeys</code> holds the supported keys for the current Dataset <code>ds</code>
     * <p>
     * If <code>ds</code> contains a sequence element, all items of this sequence are also checked against a new list
     * of matching keys.<br>
     * 
     * @param ds Dataset to check for matching keys
     * @param matchingKeys List containing all supported keys for ds.
     * 
     * @return true if an unsupported key is found.
     */
    protected boolean findUnsupportedMatchingKey(Dataset ds, IntList matchingKeys) {
        DcmElement el;
        int tag;
        for ( Iterator iter=ds.iterator() ; iter.hasNext() ; ) {
            el = (DcmElement) iter.next();
            tag = el.tag();
            if (el.isEmpty() || tag == Tags.SpecificCharacterSet
                    || Tags.isPrivate(tag)) {
                continue;
            }
            if ( el.vr() != VRs.SQ ) {
                if (matchingKeys == null || !matchingKeys.contains(tag)) {
                    log.warn("QueryCmd: Unsupported matching key found! key:"+el);
                    return true;
                }
            } else {
                IntList il = matchingKeys.contains(tag) ? //is matching of this sequence allowed?
                        (IntList)seqMatchingKeys.get(new Integer(tag)) : null;
                for ( int i=0; i<el.countItems() ; i++ ) {
                    if( findUnsupportedMatchingKey(el.getItem(i),il) ) {
                        log.warn("QueryCmd: Unsupported matching key found in SQ "+el);
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    protected void adjustDataset(Dataset ds, Dataset keys) {
        for (Iterator it = keys.iterator(); it.hasNext();) {
            DcmElement key = (DcmElement) it.next();
            final int tag = key.tag();
            if (tag == Tags.SpecificCharacterSet || Tags.isPrivate(tag))
                continue;

            final int vr = key.vr();
            DcmElement el = ds.get(tag);
            if (el == null) {
                el = ds.putXX(tag, vr);
            }
            if (vr == VRs.SQ) {
                DcmElement filteredEl = null;
                Dataset keyItem = key.getItem();
                if (keyItem != null) {
                    if (el.isEmpty()) {
                        el.addNewItem();
                    } else if (filterResult && !keyItem.isEmpty()) {
                        filteredEl = ds.putSQ(tag);
                    }
                    for (int i = 0, n = el.countItems(); i < n; ++i) {
                        Dataset item = el.getItem(i);
                        adjustDataset(item, keyItem);
                        if (filteredEl != null) {
                            filteredEl.addItem(item = item.subSet(keyItem));
                        }
                    }
                }
            }
        }
    }
    
    static class IntList {
        int[] values = new int[40];
        int pos = 0;
        
        public IntList add(int i){
            if (pos == values.length ) resize();
            values[pos++] = i;
            return this;
        }
        public IntList add(int[] vals){
            while (pos+vals.length >= values.length ) resize();
            for ( int i=0;i<vals.length;i++ ) {
                values[pos++] = vals[i];
            }
            return this;
        }
        
        public boolean contains(int val) {
            for ( int i = 0 ; i < pos ; i++ ) {
                if ( values[i] == val ) return true;
            }
            return false;
        }
        
        private void resize() {
           int[] tmp = new int[pos+20];
           System.arraycopy(values,0,tmp,0,pos);
           values = tmp;
        }
    }

 }
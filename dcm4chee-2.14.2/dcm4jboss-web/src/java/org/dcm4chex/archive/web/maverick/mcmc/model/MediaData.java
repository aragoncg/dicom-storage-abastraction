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

package org.dcm4chex.archive.web.maverick.mcmc.model;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.dcm4chex.archive.ejb.interfaces.MediaDTO;

/**
 * @author franz.willer
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class MediaData {

	private static final long GBYTE = 1000000000L;
	private static final long MBYTE = 1000000L;
	private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	private static Map mapDefinedStati;
	public static final Collection DEFINED_MEDIA_STATI = _getMediaStatusList();
	
	private long mediaPk;
	private Date createdTime;
	private Date updatedTime;
	private String filesetID;
	private String filesetIUID;
	private String mediaCreationRequestIUID;
	private int mediaStatus;
	private String mediaStatusString;
	private String mediaStatusInfo;
	private long mediaUsage;
	private boolean instancesAvailable;
	
	/**
	 * Creates a MediaData object with given MediaDTO object.
	 * 
	 * @param mediaDTO The MediaDTO object.
	 */
	public MediaData( MediaDTO mediaDTO ) {
		mediaPk = mediaDTO.getPk();
		createdTime = mediaDTO.getCreatedTime();
		updatedTime = mediaDTO.getUpdatedTime();
		filesetID = mediaDTO.getFilesetId();
		filesetIUID = mediaDTO.getFilesetIuid();
		mediaCreationRequestIUID = mediaDTO.getMediaCreationRequestIuid();
		mediaStatus = mediaDTO.getMediaStatus();
		mediaStatusString = getStatusString( mediaStatus );
		mediaStatusInfo = mediaDTO.getMediaStatusInfo();
		mediaUsage = mediaDTO.getMediaUsage();
		instancesAvailable = mediaDTO.getInstancesAvailable();
	}
	
	/**
	 * Creates an empty MediaData object with given pk.
	 * 
	 * @param pk Primary key of media.
	 */
	public MediaData( long pk ) {
		mediaPk = pk;
	}
	
	/**
	 * Returns a mediaDTO object from this MediaData object.
	 * 
	 * @return MediaDTO object.
	 */
	public MediaDTO asMediaDTO() {
		MediaDTO dto = new MediaDTO();
		dto.setPk( this.mediaPk );
		dto.setFilesetId( this.filesetID );
		dto.setFilesetIuid( this.filesetIUID );
		dto.setMediaStatus( this.mediaStatus );
		dto.setMediaStatusInfo( this.mediaStatusInfo );
		dto.setMediaUsage( this.mediaUsage );
		dto.setCreatedTime( this.createdTime );
		dto.setUpdatedTime( this.updatedTime );
		dto.setMediaCreationRequestIuid( this.mediaCreationRequestIUID );
		return dto;
	}
	
	public long getMediaPk() {
		return mediaPk;
	}

	/**
	 * @return Returns the instanceAvailable.
	 */
	public boolean isInstancesAvailable() {
		return instancesAvailable;
	}
	
	/**
	 * @return Returns the createdTime.
	 */
	public String getCreatedTime() {
		return formatter.format(createdTime);
	}
	/**
	 * @return Returns the filesetID.
	 */
	public String getFilesetID() {
		return filesetID;
	}
	/**
	 * @return Returns the filesetIUID.
	 */
	public String getFilesetIUID() {
		return filesetIUID;
	}
	/**
	 * @return Returns the mediaCreationRequestIUID.
	 */
	public String getMediaCreationRequestIUID() {
		return mediaCreationRequestIUID;
	}
	
	public void setMediaStatus( int status ) {
		mediaStatus = status;
		mediaStatusString = getStatusString( mediaStatus );
	}
	
	/**
	 * @return Returns the mediaStatus.
	 */
	public int getMediaStatus() {
		return mediaStatus;
	}
	/**
	 * @return Returns the mediaStatusString.
	 */
	public String getMediaStatusString() {
		return mediaStatusString;
	}
	/**
	 * @return Returns the mediaStatusInfo.
	 */
	public String getMediaStatusInfo() {
		return mediaStatusInfo;
	}
	
	public void setMediaStatusInfo( String info ) {
		this.mediaStatusInfo = info;
	}
	
	/**
	 * @return Returns the mediaUsage.
	 */
	public long getMediaUsage() {
		return mediaUsage;
	}
	/**
	 * Returns a String representation with trailing (G|M|K)B.
	 * <p>
	 * Rounds the value to a more readable form.
	 * <p>
	 * Use 1000 instead of 1024 for one KB.
	 * 
	 * @param size the size in bytes.
	 * 
	 * @return The size with unit. e.g. 2GB
	 */
	public String getMediaUsageWithUnit() {
		NumberFormat nf = new DecimalFormat();
		nf.setGroupingUsed( true );
		if ( mediaUsage >= GBYTE ){
				return nf.format(mediaUsage/MBYTE)+" GB";//##,### GByte
		} else if ( mediaUsage > MBYTE ) {
				return nf.format(mediaUsage/1000L)+" MB";
		} else {
			return nf.format(mediaUsage)+" KB";
		}
	}
	/**
	 * @return Returns the updatedTime.
	 */
	public String getUpdatedTime() {
		return formatter.format(updatedTime);
	}
	
	/**
	 * Returns the String representation of the given media status.
	 * 
	 * @param status The media status
	 * 
	 * @return Media status as String.
	 */
	public static String getStatusString( int status ) {
		MediaStatus ms = (MediaStatus) mapDefinedStati.get( new Integer( status ) );
		if ( ms == null ) {
			return "unknown ("+status+")";
		} else {
			return ms.getDescription();
		}
	}
	
	/**
	 * Return a collection of all defined media stati.
	 * <p>
	 * Build the map <code>mapDefinedStati</code> with status as Integer as key, value is a MediaStatus object.
	 * <p>
	 * Returns the values from the map.
	 * 
	 * @return Collection of all defined media stati.
	 */
	private static Collection _getMediaStatusList() {
		mapDefinedStati = new HashMap();//TODO get from MediaDTO!
		mapDefinedStati.put( new Integer(MediaDTO.OPEN), new MediaStatus( MediaDTO.OPEN, "OPEN", 1 ) );
		mapDefinedStati.put( new Integer(MediaDTO.ERROR), new MediaStatus( MediaDTO.ERROR, "FAILED", 2 ) );
		mapDefinedStati.put( new Integer(MediaDTO.QUEUED), new MediaStatus( MediaDTO.QUEUED, "QUEUED", 3 ) );
		mapDefinedStati.put( new Integer(MediaDTO.TRANSFERING), new MediaStatus( MediaDTO.TRANSFERING, "TRANSFERING", 4 ) );
		mapDefinedStati.put( new Integer(MediaDTO.BURNING), new MediaStatus( MediaDTO.BURNING, "CREATING", 5 ) );
		mapDefinedStati.put( new Integer(MediaDTO.COMPLETED), new MediaStatus( MediaDTO.COMPLETED, "COMPLETED", 6 ) );
		return mapDefinedStati.values();
	}
	
    public boolean equals( Object o ) {
    	if ( o == null || ! ( o instanceof MediaData ) ) return false;
    	return this.getMediaPk() == ( (MediaData) o ).getMediaPk(); 
    }
    
    public int hashCode() {
    	return (int)mediaPk;
    }

	
	/**
	 * Container Class for MediaStatus.
	 * <p>
	 * encapsulate status value as int and description (String)
	 * 
	 * @author franz.willer
	 *
	 * TODO To change the template for this generated type comment go to
	 * Window - Preferences - Java - Code Style - Code Templates
	 */
	public static class MediaStatus {
		int status;
		String description;
		int order;
		
		public MediaStatus( int status, String desc, int order ) {
			this.status = status;
			this.description = desc;
			this.order = order;
		}
		/**
		 * @return Returns the description.
		 */
		public String getDescription() {
			return description;
		}
		/**
		 * @return Returns the status.
		 */
		public int getStatus() {
			return status;
		}
		/**
		 * @return Returns the order.
		 */
		public int getOrder() {
			return order;
		}
	}

}


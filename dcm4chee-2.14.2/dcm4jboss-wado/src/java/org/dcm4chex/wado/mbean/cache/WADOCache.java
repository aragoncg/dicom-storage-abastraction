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

package org.dcm4chex.wado.mbean.cache;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author franz.willer
 * 
 * This Interface defines the basic methods of an WADO cache for DICOM images.
 * <p>
 * The images are represented as File objects to allow streaming of the image
 * data.
 */
public interface WADOCache {

	
    /**
     * Get an image as BufferedImage from cache.
     * <p>
     * This method returns the image from the default path of this cache.<br>
     * 
     * @param studyUID
     *            Unique identifier of the study.
     * @param seriesUID
     *            Unique identifier of the series.
     * @param instanceUID
     *            Unique identifier of the instance.
     * @param rows
     *            Image height in pixel.
     * @param columns
     *            Image width in pixel.
     * @param region
     *            Image region defined by two points in opposing corners
     * @param windowWidth
     *            Decimal string representing the contrast of the image.
     * @param windowCenter
     *            Decimal string representing the luminosity of the image.
     * @param imageQuality
     *            Integer string (1-100) representing required quality of
     *            the image to be returned within the range 1 to 100
     * 
     * @return The image if in cache or null.
     */
    BufferedImage getImage(String studyUID, String seriesUID,
            String instanceUID, String rows, String columns, String region,
            String windowWidth, String windowCenter, String imageQuality,
            String suffix);

    /**
     * Get an image of special size from cache.
     * <p>
     * This method use a image size (rows and columns) and a region (two points)
     * to search on a special path of this cache.
     * 
     * @param studyUID
     *            Unique identifier of the study.
     * @param seriesUID
     *            Unique identifier of the series.
     * @param instanceUID
     *            Unique identifier of the instance.
     * @param rows
     *            Image height in pixel.
     * @param columns
     *            Image width in pixel.
     * @param region
     *            Image region defined by two points in opposing corners
     * @param windowWidth
     *            Decimal string representing the contrast of the image.
     * @param windowCenter
     *            Decimal string representing the luminosity of the image.
     * @param imageQuality
     *            Integer string (1-100) representing required quality of
     *            the image to be returned within the range 1 to 100
     * 
     * @return The File object of the image if in cache or null.
     */
    File getImageFile(String studyUID, String seriesUID, String instanceUID,
            String rows, String columns, String region, String windowWidth,
            String windowCenter, String imageQuality, String suffix);

    /**
     * Put a region of an image of special size to this cache.
     * <p>
     * Stores the image on a special path of this cache.
     * 
     * @param image
     *            The image (with special size)
     * @param studyUID
     *            Unique identifier of the study.
     * @param seriesUID
     *            Unique identifier of the series.
     * @param instanceUID
     *            Unique identifier of the instance.
     * @param rows
     *            Image height in pixel.
     * @param columns
     *            Image width in pixel.
     * @param region
     *            Image region defined by two points in opposing corners
     * @param windowWidth
     *            Decimal string representing the contrast of the image.
     * @param windowCenter
     *            Decimal string representing the luminosity of the image.
     * @param imageQuality
     *            Integer string (1-100) representing required quality of
     *            the image to be returned within the range 1 to 100
     * 
     * @return The File object of the image in this cache.
     * @throws IOException
     */
    File putImage(BufferedImage image, String studyUID, String seriesUID,
            String instanceUID, String pixelRows, String pixelColumns,
            String region, String windowWidth, String windowCenter,
            String imageQuality, String suffix) throws IOException;

    /**
     * Puts a stream to this cache.
     * 
     * @param stream
     *            The InputStream to store.
     * @param studyUID
     *            Unique identifier of the study.
     * @param seriesUID
     *            Unique identifier of the series.
     * @param instanceUID
     *            Unique identifier of the instance.
     * @param rows
     *            Image height in pixel.
     * @param columns
     *            Image width in pixel.
     * @param region
     *            Rectangular region of the image (defined by two points)
     * @param windowWidth
     *            Decimal string representing the contrast of the image.
     * @param windowCenter
     *            Decimal string representing the luminosity of the image.
     * @param imageQuality
     *            Integer string (1-100) representing required quality of
     *            the image to be returned within the range 1 to 100
     * 
     * @return The stored File object.
     * 
     * @throws IOException
     */
    File putStream(InputStream stream, String studyUID, String seriesUID,
            String instanceUID, String pixelRows, String pixelColumns,
            String region, String windowWidth, String windowCenter,
            String imageQuality, String suffix) throws IOException;

    /**
     * Return the File object to get or store a file for given arguments.
     * <p>
     * If the cache object referenced with arguments is'nt in this cache the
     * returned file object exists() method will result false!
     * 
     * @param studyUID
     *            Unique identifier of the study.
     * @param seriesUID
     *            Unique identifier of the series.
     * @param instanceUID
     *            Unique identifier of the instance.
     * @param key
     *            TODO
     * 
     * @return File object to get or store a file.
     */
    File getFileObject(String studyUID, String seriesUID, String instanceUID,
            String key);

    /**
     * Clears this cache.
     * <p>
     * Remove all images in this cache.
     */
    void clearCache();

    /**
     * Removes old entries to shrink this cache.
     * 
     * @param background
     *            If true clean process runs in a seperate thread.
     * @throws IOException
     * @throws IOException
     */
    void freeDiskSpace(boolean background) throws IOException;


    /**
     * Remove all cached entries for given StudyUID
     * @param studyIUID
     */
    void purgeStudy(String studyIUID);
    
    /**
     * Setter for root directory of this cache.
     * <p>
     * If a relative path is given, the resulting absolute path is relative to
     * the servers home dir.
     * 
     * @param newRoot
     *            The root directory for this cache.
     */
    void setCacheRoot(String newRoot);

    /**
     * Getter for root directory of this cache.
     * <p>
     * This method returns always the same String used in setCacheRoot!
     * 
     * @return The root directory for this cache (relative or absolute)
     */
    String getCacheRoot();

    /**
     * Getter for the absolute root directory of this cache.
     * 
     * @return The root directory for this cache (absolute)
     */
    File getAbsCacheRoot();

    /**
     * Returns the min drive space that must be available on the caches drive.
     * <p>
     * This value is used to determine if this cache should be cleaned.
     * 
     * @return Min allowed free diskspace in bytes.
     */
    long getMinFreeSpace();

    /**
     * Returns the free space that should be remain after cleaning the cache.
     * <p>
     * This value is used as lower watermark of the cleaning process.
     * 
     * @return Preferred free diskspace in bytes.
     */
    long getPreferredFreeSpace();

    /**
     * Returns the current free space on drive where this cache is stored.
     * 
     * @return Current free diskspace in bytes
     * @throws IOException
     */
    long showFreeSpace() throws IOException;

    /**
     * Returns true if a client redirect should be used if requewsted DICOM
     * object is not local.
     * 
     * @return True if client redirect should be used..
     */
    public boolean isClientRedirect();

    /**
     * Set the flag to determine if a client redirect should be used if the
     * requested DICOM object is not local.
     * 
     * @param clientRedirect
     *            True for client side redirect, false for server side redirect.
     */
    public void setClientRedirect(boolean clientRedirect);

    /**
     * Returns true if a server side redirect should be cached.
     * 
     * @return True if a server side redirected request should be cached.
     */
    public boolean isRedirectCaching();

    /**
     * Set the flag if caching is enabled for server side redirect.
     * 
     * @param redirectCaching
     *            True to enable caching.
     */
    public void setRedirectCaching(boolean redirectCaching);

    public String getImageQuality();

    public void setImageQuality(String imageQuality);

    public String getImageWriterClass();

	public void setImageWriterClass(String imageWriterClass);
}

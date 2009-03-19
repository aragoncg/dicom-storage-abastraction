/******************************************
 *                                        *
 *  dcm4che: A OpenSource DICOM Toolkit   *
 *                                        *
 *  Distributable under LGPL license.     *
 *  See terms of license at gnu.org.      *
 *                                        *
 ******************************************/

package org.dcm4chex.archive.notif;

import java.io.Serializable;

/**
 * @author franz.willer@gwi-ag.com
 * @version $Revision: 7596 $ $Date: 2008-10-16 16:53:05 +0200 (Thu, 16 Oct 2008) $
 * @since Nov 9, 2005
 */
public class PatientUpdated implements Serializable {

    private static final long serialVersionUID = -829912804804286932L;

    private String patientID;

    private String description;

    public PatientUpdated(String patientID, String description) {
        this.patientID = patientID;
        this.description = description;

    }

    /**
     * @return Returns the patientID.
     */
    public String getPatientID() {
        return patientID;
    }

    /**
     * @return Returns the description.
     */
    public String getDescription() {
        return description;
    }
}

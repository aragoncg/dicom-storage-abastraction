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

package org.dcm4chex.archive.hsm;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.compress.tar.TarEntry;
import org.apache.commons.compress.tar.TarInputStream;
import org.dcm4cheri.util.StringUtils;
import org.dcm4chex.archive.ejb.jdbc.FileInfo;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 3230 $ $Date: 2007-03-29 21:28:51 +0200 (Thu, 29 Mar 2007) $
 * @since Mar 1, 2006
 */
public class VerifyTar {

    private static final int BUF_SIZE = 8192;
    private static final String USAGE = "Usage: java -jar verifytar.jar [-p<num>] <file or directory path>[..]\n\n"
            + " -p<num>  Strip the smallest prefix containing <num> leading slashes from each\n"
            + "          file name prompted to stdout.";


    public static void verify(File file, byte[] buf)
            throws IOException, VerifyTarException {
        FileInputStream in = new FileInputStream(file);
        try {
            verify(in, file.toString(), buf);
        } finally {
            in.close();
        }
    }
    
    public static void verify(InputStream in, String tarname, byte[] buf)
    throws IOException, VerifyTarException {
        verify(in, tarname, buf, null);
    }

    public static void verify(InputStream in, String tarname, byte[] buf, ArrayList objectNames)
    throws IOException, VerifyTarException {
        TarInputStream tar = new TarInputStream(in);
        try {
            TarEntry entry = tar.getNextEntry();
            if (entry == null)
                throw new VerifyTarException("No entries in " + tarname);
            String entryName = entry.getName();
            if (!"MD5SUM".equals(entryName))
                throw new VerifyTarException("Missing MD5SUM entry in "
                        + tarname);
            DataInputStream dis = new DataInputStream(tar);
            
            HashMap md5sums = new HashMap();
            String line;
            while ((line = dis.readLine()) != null) {
                char[] c = line.toCharArray();
                byte[] md5sum = new byte[16];
                for (int i = 0, j = 0; i < md5sum.length; i++, j++, j++) {
                    md5sum[i] = (byte) ((fromHexDigit(c[j]) << 4)
                            | fromHexDigit(c[j + 1]));
                }
                md5sums.put(line.substring(34), md5sum);
            }
            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
            while ((entry = tar.getNextEntry()) != null) {
                entryName = entry.getName();
                if(objectNames != null && !objectNames.remove(entryName))
                    throw new VerifyTarException( "TAR " + tarname + " contains entry: "
                            + entryName + " not in file list");
                byte[] md5sum = (byte[]) md5sums.remove(entryName);
                if (md5sum == null)
                    throw new VerifyTarException("Unexpected TAR entry: "
                            + entryName + " in " + tarname);
                digest.reset();
                in = new DigestInputStream(tar, digest);
                while (in.read(buf) > 0)
                    ;
                if (!Arrays.equals(digest.digest(), md5sum)) {
                    throw new VerifyTarException(
                            "Failed MD5 check of TAR entry: " + entryName
                            + " in " + tarname);
                }
            }
            if (!md5sums.isEmpty())
                throw new VerifyTarException("Missing TAR entries: "
                        + md5sums.keySet() + " in " + tarname);
            if (objectNames != null && !objectNames.isEmpty())
                throw new VerifyTarException("Missing TAR entries from object list: "
                        + objectNames.toString() + " in " + tarname);
        } finally {
            tar.close();
        }
    }

    public static int fromHexDigit(char c) {
        return c - ((c <= '9') ? '0' : (((c <= 'F') ? 'A' : 'a') - 10));
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println(USAGE);
            System.exit(-1);
        }
        int off = 0;
        int strip = 0;
        if (args[0].startsWith("-p")) {
            try {
                strip = Integer.parseInt(args[0].substring(2));
                off = 1;
            } catch (NumberFormatException e) {
                System.out.println(USAGE);
                System.exit(-1);
            }
        }
        int errors = 0;
        byte[] buf = new byte[BUF_SIZE];
        for (int i = off; i < args.length; i++) {
            try {
                errors += VerifyTar.verify(new File(args[i]), strip, buf);
            } catch (FileNotFoundException e) {
                System.err.println(e.getMessage());
                System.exit(-2);
            }
        }
        System.exit(errors);
    }

    private static int verify(File file, int strip, byte[] buf) 
            throws FileNotFoundException {
        int errors = 0;
        if (file.isDirectory()) {
            String[] ss = file.list();
            for (int i = 0; i < ss.length; i++) {
                errors += verify(new File(file, ss[i]), strip, buf);
            }
        } else {
             String tarname = file.getPath();

            try {
                int pos = 0;
                while (strip-- > 0) {
                    pos = tarname.indexOf(File.separatorChar, pos);
                    if (pos == -1)
                        break;
                    pos++;
                }
                if (pos != -1) {
                    System.out.print(tarname.substring(pos));
                    System.out.print(' ');
                }
                verify(file, buf);
                System.out.println("ok");
            } catch (Exception e) {
                errors = 1;
                System.out.println(e.getMessage());
            }
        }
        return errors;
    }


}

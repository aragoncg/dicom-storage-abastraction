/*
 * Copyright 2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dcm4chex.archive.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.dcm4che.util.Executer;

/**
 * General File System utilities.
 * <p>
 * This class provides static utility methods for general file system
 * functions not provided via the JDK {@link java.io.File File} class.
 * <p>
 * The current functions provided are:
 * <ul>
 * <li>Get the free space on a drive
 * </ul>
 *
 * @author Frank W. Zammetti
 * @author Stephen Colebourne
 * @version $Id: FileSystemUtils.java 6691 2008-07-30 13:17:46Z gunterze $
 * @since Commons IO 1.1
 */
public class FileSystemUtils {

    /** Singleton instance, used mainly for testing. */
    private static final FileSystemUtils INSTANCE = new FileSystemUtils();

    /** Operating system state flag for error. */
    private static final int INIT_PROBLEM = -1;
    /** Operating system state flag for neither Unix nor Windows. */
    private static final int OTHER = 0;
    /** Operating system state flag for Windows. */
    private static final int WINDOWS = 1;
    /** Operating system state flag for Unix. */
    private static final int UNIX = 2;
    
    private static String dfCommand = "df";
    private static String dfCommandOption = "-P";

    /** The operating system flag. */
    private static final int OS;
    static {
        int os = OTHER;
        try {
            String osName = System.getProperty("os.name");
            if (osName == null) {
                throw new IOException("os.name not found");
            }
            osName = osName.toLowerCase();
            // match
            if (osName.indexOf("windows") != -1) {
                os = WINDOWS;
            } else if (osName.indexOf("linux") != -1 ||
                osName.indexOf("sun os") != -1 ||
                osName.indexOf("sunos") != -1 ||
                osName.indexOf("solaris") != -1 ||
                osName.indexOf("mpe/ix") != -1 ||
                osName.indexOf("hp-ux") != -1 ||
                osName.indexOf("aix") != -1 ||
                osName.indexOf("freebsd") != -1 ||
                osName.indexOf("irix") != -1 ||
                osName.indexOf("digital unix") != -1 ||
                osName.indexOf("unix") != -1 ||
                osName.indexOf("mac os x") != -1) {
                os = UNIX;
            } else {
                os = OTHER;
            }

        } catch (Exception ex) {
            os = INIT_PROBLEM;
        }
        OS = os;
    }
    
    /** JDK6 File.getUsableSpace(), if available. */
    private static Method jdk6getUsableSpace;
    static {
        try {
            jdk6getUsableSpace = File.class.getMethod("getUsableSpace", (Class[]) null);
        } catch (Exception e) {
            jdk6getUsableSpace = null;
        }        
    }
    
    public static final String getDFCommand() {
        return dfCommand;
    }

    public static final void setDFCommand(String dfCommand) {
        FileSystemUtils.dfCommand = dfCommand;
    }

    public static final String getDFCommandOption() {
        return dfCommandOption;
    }

    public static final void setDFCommandOption(String dfCommandOption) {
        FileSystemUtils.dfCommandOption = dfCommandOption;
    }

    /**
     * Instances should NOT be constructed in standard programming.
     */
    public FileSystemUtils() {
        super();
    }

    //-----------------------------------------------------------------------
    /**
     * Returns the free space on a drive or volume in a cross-platform manner.
     * Note that some OS's are NOT currently supported, including OS/390.
     * <pre>
     * FileSystemUtils.freeSpace("C:");  // Windows
     * FileSystemUtils.freeSpace("/volume");  // *nix
     * </pre>
     * The free space is calculated via the command line.
     * It uses 'dir /-c' on Windows and 'df' on *nix.
     *
     * @param path  the path to get free space for, not null, not empty on Unix
     * @return the amount of free drive space on the drive or volume
     * @throws IllegalArgumentException if the path is invalid
     * @throws IllegalStateException if an error occurred in initialisation
     * @throws IOException if an error occurs when finding the free space
     */
    public static long freeSpace(String path) throws IOException {
        if (jdk6getUsableSpace != null) {
            try {
                Long l = (Long) jdk6getUsableSpace.invoke(
                		new File(path), (Object[]) null);
                long space = l.longValue();
                // Following is a workaround for a bug in the Sun JDK 6. The
                // File.getFreeSpace method always returns 0 on file systems
                // greater than 4 TB. If we get a 0 back from the method call,
                // fall through to the "df" style check.
                if (space != 0) 
                    return space;
            } catch (Exception e) {
                // Should not happen
                e.printStackTrace();
            }
        }
        return INSTANCE.freeSpaceOS(path, OS);
    }

    /**
     * Returns the free space on a drive or volume in a cross-platform manner.
     * Note that some OS's are NOT currently supported, including OS/390.
     * <pre>
     * FileSystemUtils.freeSpace("C:");  // Windows
     * FileSystemUtils.freeSpace("/volume");  // *nix
     * </pre>
     * The free space is calculated via the command line.
     * It uses 'dir /-c' on Windows and 'df' on *nix.
     *
     * @param path  the path to get free space for, not null, not empty on Unix
     * @param os  the operating system code
     * @return the amount of free drive space on the drive or volume
     * @throws IllegalArgumentException if the path is invalid
     * @throws IllegalStateException if an error occurred in initialisation
     * @throws IOException if an error occurs when finding the free space
     */
    long freeSpaceOS(String path, int os) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("Path must not be empty");
        }
        switch (os) {
            case WINDOWS:
                return freeSpaceWindows(path);
            case UNIX:
                return freeSpaceUnix(path);
            case OTHER:
                throw new IllegalStateException("Unsupported operating system");
            default:
                throw new IllegalStateException(
                  "Exception caught when determining operating system");
        }
    }

    /**
     * Find free space on the Windows platform using the 'dir' command.
     *
     * @param path  the path to get free space for, including the colon
     * @return the amount of free drive space on the drive
     * @throws IOException if an error occurs
     */
    long freeSpaceWindows(String path) throws IOException {
//        path = FilenameUtils.normalize(path);
//        if (path.length() > 2 && path.charAt(1) == ':') {
//            path = path.substring(0, 2);  // seems to make it work
//        }

        // build and run the 'dir' command
        String[] cmdAttrbs = new String[] {"cmd.exe", "/C", "dir /c \"" + path+ "\""};

        // read in the output of the command to an ArrayList
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        try {
            new Executer(cmdAttrbs, stdout , null).waitFor();
        } catch (InterruptedException e) {
            throw new IOException(e.getMessage());
        }
        BufferedReader in = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(stdout.toByteArray())));
        ArrayList lines = new ArrayList();
        String line = in.readLine();
        while (line != null) {
            line = line.toLowerCase().trim();
            lines.add(line);
            line = in.readLine();
        }
  

        if (lines.size() == 0) {
            // unknown problem, throw exception
            throw new IOException(
                    "Command line 'dir /c' did not return any info " +
                    "for command '" + cmdAttrbs[2] + "'");
        }

        // now iterate over the lines we just read and find the LAST
        // non-empty line (the free space bytes should be in the last element
        // of the ArrayList anyway, but this will ensure it works even if it's
        // not, still assuming it is on the last non-blank line)
        long bytes = -1;
        int i = lines.size() - 1;
        int bytesStart = 0;
        int bytesEnd = 0;
        outerLoop: while (i > 0) {
            line = (String) lines.get(i--);
            if (line.length() > 0) {
                // found it, so now read from the end of the line to find the
                // last numeric character on the line, then continue until we
                // find the first pair of non-numeric character, and everything between
                // that and the last numeric character inclusive is our free
                // space bytes count (with or without grouping seoperator(s))
                int j = line.length() - 1;
                innerLoop1: while (j >= 0) {
                    char c = line.charAt(j);
                    if (Character.isDigit(c)) {
                      // found the last numeric character, this is the end of
                      // the free space bytes count
                      bytesEnd = j + 1;
                      break innerLoop1;
                    }
                    j--;
                }
                innerLoop2: while (j >= 0) {//Loop until found two adjoining non digit characters.
                    char c = line.charAt(j);
                    if (!Character.isDigit(c) && ( j == 0 || !Character.isDigit(line.charAt(j-1)))) {
                      break innerLoop2;
                    }
                    j--;
                }
                bytesStart = j;
                break outerLoop;
            }
        }

        // remove non digit chars in the bytes count
        StringBuffer buf = new StringBuffer(line.substring(bytesStart, bytesEnd));
        char c;
        for (int k = 0; k < buf.length(); k++) {
            c = buf.charAt(k);
            if (!Character.isDigit(c)) {
                buf.deleteCharAt(k--);
            }
        }
        bytes = Long.parseLong(buf.toString());
        return bytes;
    }

    /**
     * Find free space on the *nix platform using the 'df' command.
     *
     * @param path  the path to get free space for
     * @return the amount of free drive space on the volume
     * @throws IOException if an error occurs
     */
    long freeSpaceUnix(String path) throws IOException {
        if (path.length() == 0) {
            throw new IllegalArgumentException("Path must not be empty");
        }
//      path = FilenameUtils.normalize(path);

        // build and run the 'dir' command
        String[] cmdAttribs = new String[] {dfCommand, dfCommandOption, path};

        // read the output from the command until we come to the second line

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        try {
            new Executer(cmdAttribs, stdout , null).waitFor();
        } catch (InterruptedException e) {
            throw new IOException(e.getMessage());
        }
        BufferedReader in = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(stdout.toByteArray())));
        String line1 = in.readLine(); // header line (ignore it)
        String line2 = in.readLine(); // the line we're interested in
        String line3 = in.readLine(); // possibly interesting line
        return parseUnixDF(path, line1, line2, line3);
    }

    public static long parseDF(String path, String line1, String line2, String line3)
    		throws IOException {
        return INSTANCE.parseUnixDF(path, line1, line2, line3);
    }

    long parseUnixDF(String path, String line1, String line2, String line3)
            throws IOException {
        if (line2 == null) {
            // unknown problem, throw exception
            throw new IOException(
                    "'" + dfCommand + " " + dfCommandOption +
                    "' did not return info as expected " + "for path '" + path +
                    "'- response on first line was '" + line1 + "'");
        }
        
        // try to get block size from header - if it fails, assume 512
        long blocksize = 512L;
        int blocksizeEnd = line1.indexOf('-');
        if (blocksizeEnd > 0) {
            int factor = 1;
            if (line1.charAt(blocksizeEnd - 1) == 'K') {
                --blocksizeEnd;
                factor = 1024;
            }
            int blocksizePos = blocksizeEnd;
            while (blocksizePos > 0 && Character.isDigit(
                    line1.charAt(blocksizePos - 1))) {
                --blocksizePos;
            }
            if (blocksizePos < blocksizeEnd) {
                blocksize = factor * Long.parseLong(
                        line1.substring(blocksizePos, blocksizeEnd));
            }
        }
        
        line2 = line2.trim();

        // Now, we tokenize the string. The fourth element is what we want.
        StringTokenizer tok = new StringTokenizer(line2, " ");
        if (tok.countTokens() < 4) {
            // could be long Filesystem, thus data on third line
            if (tok.countTokens() == 1 && line3 != null) {
                line3 = line3.trim();
                tok = new StringTokenizer(line3, " ");
            } else {
                throw new IOException(
                        "'" + dfCommand + " " + dfCommandOption +
                        "' did not return data as expected for path '" + path +
                        "'- check path is valid");
            }
        } else {
            tok.nextToken(); // Ignore Filesystem
        }
        tok.nextToken(); // Ignore 1K-blocks
        tok.nextToken(); // Ignore Used
        String freeSpace = tok.nextToken();
        try {
            return (Long.parseLong(freeSpace) * blocksize);
        } catch (NumberFormatException ex) {
            throw new IOException(
                    "'" + dfCommand + " " + dfCommandOption +
                    "' did not return numeric data as expected for path '" + 
                    path + "'- check path is valid");
        }
    }
}
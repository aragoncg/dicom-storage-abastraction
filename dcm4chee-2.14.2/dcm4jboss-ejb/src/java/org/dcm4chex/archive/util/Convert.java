package org.dcm4chex.archive.util;

public class Convert {

	public static byte[] toBytes(long n) {
		byte[] b = new byte[8];
		b[0] = (byte) (n >>> 56);
		b[1] = (byte) (n >>> 48);
		b[2] = (byte) (n >>> 40);
		b[3] = (byte) (n >>> 32);
		b[4] = (byte) (n >>> 24);
		b[5] = (byte) (n >>> 16);
		b[6] = (byte) (n >>> 8);
		b[7] = (byte) (n);

		return b;
	}

	public static long toLong(byte[] b) {
		long n = (long) (b[0] & 0xff) << 56 | (long) (b[1] & 0xff) << 48
				| (long) (b[2] & 0xff) << 40 | (long) (b[3] & 0xff) << 32
				| (long) (b[4] & 0xff) << 24 | (long) (b[5] & 0xff) << 16
				| (long) (b[6] & 0xff) << 8 | (long) (b[7] & 0xff);

		return n;
	}
}

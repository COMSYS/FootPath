package de.uvwxy.footpath.h263;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;



public class ISOBoxParser {

	private FileInputStream fis = null;
	private int fisPtr = 0; // points to the next unread byte

	/**
	 * The standard constructor for this class. Please provide a FileInputStream
	 * that has not been read, i.e. the current position is still equal to 0.
	 * 
	 * @param fis
	 *            the FileInputStream
	 */
	public ISOBoxParser(FileInputStream fis) {
		this.fis = fis;
	}

	/**
	 * This functions reads the Iso Media File Format and prints which boxes are
	 * present, in order of appearance, and their length.
	 * 
	 * @throws IOException
	 *             things can go wrong
	 */
	public void printBoxTypes() throws IOException {
		// pointer is 0
		int boxBytes = 0;
		
		while (fis.available() > 0) {
			int len = readBoxLen();
			
			String type = readBoxType();

			DebugOut.debug_vv("found type " + type + ", len " + len + " bytes, @" + fisPtr);

			// Only skip this box if it has no sub boxes
			if (type.equals("moov")) {
				boxBytes += len;
			} else if (type.equals("ftyp")) {
				boxBytes += len;
				skipInFrontOfNextHeader(len);
			}else if (type.equals("free")) {
				boxBytes += len;
				skipInFrontOfNextHeader(len);
			} else if (type.equals("mdat")) {
				boxBytes += len;
				skipInFrontOfNextHeader(len);
			} else if (type.equals("udta")) {

			} else if (type.equals("trak")) {

			} else if (type.equals("mdia")) {

			} else if (type.equals("minf")) {

			} else if (type.equals("dinf")) {

			} else if (type.equals("stbl")) {

			} else if (type.equals("trak")) {

			} else {
				skipInFrontOfNextHeader(len);
			}

		} // while (fis.available() > 0)
		
		DebugOut.debug_vv("total file (derived from boxes) size: " + (boxBytes) + " bytes"); // = filesize
	}

	/**
	 * This functions reads the Iso Media File Format and dumps the contents
	 * of the mdat box (without header), into inputfile.???.mdat
	 * @param outFile name of the file to save (in working directory) 
	 * @throws IOException
	 */
	public void dumpMDATBox(String outFile) throws IOException {
		// pointer is 0

		while (fis.available() > 0) {
			int len = readBoxLen();
			String type = readBoxType();

			if (type.equals("mdat")) {
				DebugOut.debug_vv("found type " + type + ", len " + len + " @"
						+ fisPtr);
				// TODO write code to dumphere
				FileOutputStream out = new FileOutputStream(outFile);

				int lenLeft = len - 8; // header + type already read
				int fileSizeInBytes = lenLeft;
				while (lenLeft > 0) {
					if (lenLeft > 1024) {
						byte[] b = new byte[1024];
						fis.read(b);
						out.write(b);
						lenLeft -= 1024;
					}
					int rbyte = fis.read();
					// rbyte should be >= 0. -1 on error
					if (rbyte != -1) {
						out.write(rbyte);
					} else {
						break;
					}
					len--;
				} // while (lenLeft > 0)

				// write buffers to disk
				out.flush();
				// close file handle
				out.close();

				// exit function
				return;

			} else {
				skipInFrontOfNextHeader(len);
			} // if (type.equals("mdat"))
			DebugOut.debug_vv("found type " + type + ", len " + len + " @" + fisPtr);
		}
	}
	
	public int jumpIntoMDATBox() throws IOException {
		// pointer is 0

		while (fis.available() > 0) {
			int len = readBoxLen();
			String type = readBoxType();

			if (type.equals("mdat")) {
				DebugOut.debug_vv("found " + type + " box, len " + len);
				DebugOut.debug_vv("returning fisPtr: " + fisPtr);
				return fisPtr;
			} else {
				skipInFrontOfNextHeader(len);
			} // if (type.equals("mdat"))
		}
		DebugOut.debug_vv("mdat not found");
		DebugOut.debug_vv("returning -1");
		return -1;
	}

	/**
	 * Reads the length of the box, i.e. reads the next 4 bytes (from current
	 * pointer) into a 32bit integer. This value is returned.
	 * @return
	 * @throws IOException things can go wrong
	 */
	private int readBoxLen() throws IOException {
		byte[] four_bytes = new byte[4];
		int ret = read4Bytes(four_bytes);
		int len = fourBytesToInt(four_bytes);
		return len;
	}

	/**
	 * Reads the type of the box, i.e. reads the next 4 bytes (from current
	 * pointer) into a 4 char String. This String is returned.
	 * @return
	 * @throws IOException things can go wrong
	 */
	private String readBoxType() throws IOException {
		byte[] four_bytes = new byte[4];
		int ret = read4Bytes(four_bytes);
		String type = fourBytesToString(four_bytes);
		return type;
	}

	/**
	 * This function takes the length of a current read box, and proceeds to
	 * jump infront of the next length integer of the following box. (It
	 * subtracts the 8 bytes containing box length and type)
	 * 
	 * @param boxSize
	 * @throws IOException
	 */
	private void skipInFrontOfNextHeader(int boxSize) throws IOException {
		// header size is read -4
		// header type is read -4
		fis.skip(boxSize - 8);
		fisPtr += boxSize - 8;
	}

	/**
	 * Reads 4 byte from the input stream, and increases the pointer fisPtr by
	 * 4.
	 * @param bytes
	 * @return
	 * @throws IOException
	 */
	private int read4Bytes(byte[] bytes) throws IOException {
		int ret = fis.read(bytes);
		fisPtr += ret;
		return ret;
	}

	/**
	 * This functions takes 4(!) bytes (a byte[4] array), and returns a String
	 * representation. "fail" is returned if the input array has a length != 4.
	 * @param c input array of bytes (length 4!)
	 * @return String representation of input byte array
	 */
	private String fourBytesToString(byte[] c) {
		if (c.length != 4) {
			return "fail";
		}
		return "" + (char) c[0] + (char) c[1] + (char) c[2] + (char) c[3];
	}

	/**
	 * Convert 4 bytes (a byte[4] array!) into an integer.
	 * Correct conversion using ByteBuffer.
	 * @param c
	 * @return
	 */
	private int fourBytesToInt(byte[] c) {
		ByteBuffer bb = ByteBuffer.wrap(c);
		if (c.length != 4) {
			return -1;
		}
		int t = bb.getInt();
		return t;
	}
}

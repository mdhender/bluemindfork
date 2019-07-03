/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.eas.utils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
 * File manipulation functions
 * 
 * 
 */
public class FileUtils {

	private static final int BUFF_SIZE = 100000;

	/**
	 * Fast stream transfer method
	 * 
	 * @param in
	 * @param out
	 * @throws IOException
	 */
	public static void transfer(InputStream in, OutputStream out,
			boolean closeIn) throws IOException {
		final byte[] buffer = new byte[BUFF_SIZE];

		try {
			while (true) {
				int amountRead = in.read(buffer);
				if (amountRead == -1) {
					break;
				}
				out.write(buffer, 0, amountRead);
			}
		} finally {
			if (closeIn) {
				in.close();
			}
			out.flush();
			out.close();
		}
	}

	public static String streamString(InputStream in, boolean closeIn)
			throws IOException {
		return new String(streamBytes(in, closeIn), "utf-8");
	}

	public static byte[] streamBytes(InputStream in, boolean closeIn)
			throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		transfer(in, out, closeIn);
		return out.toByteArray();
	}

	/**
	 * 
	 * BM-4746
	 * 
	 * @param in
	 * @return
	 * @throws IOException
	 */
	public static byte[] streamAndSanitizeBytes(InputStream in)
			throws IOException {

		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String line = null;
		StringBuffer sb = new StringBuffer();
		while ((line = br.readLine()) != null) {
			sb.append(line).append("\r\n");
		}
		br.close();

		return sb.toString().getBytes();
	}

}

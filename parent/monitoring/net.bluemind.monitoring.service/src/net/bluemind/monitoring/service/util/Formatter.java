/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2016
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
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
package net.bluemind.monitoring.service.util;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.monitoring.api.FetchedData;

/**
 * The formatter is a utility class to format strings/parse results etc...
 * 
 * @author vincent
 *
 */
public abstract class Formatter {

	private final static long KB_FACTOR = 1000;
	private final static long MB_FACTOR = 1000 * KB_FACTOR;
	private final static long GB_FACTOR = 1000 * MB_FACTOR;

	/**
	 * Parses data into a list. Typically used after getting a raw result in
	 * which data is separated with a special character (new line, tab...)
	 * 
	 * @param result
	 *            The raw result to be parsed
	 * @param delimiter
	 *            The string used to parse the raw result
	 * @return The list containing the parsed data
	 */
	public static List<FetchedData> parseRawResultInFetchedDataList(String result) {

		String currentLine = "";
		List<FetchedData> list = new ArrayList<FetchedData>();
		Scanner sc = new Scanner(result);

		while (sc.hasNextLine()) {
			currentLine = sc.nextLine();
			if (StringUtils.isNotBlank(currentLine)) {
				list.add(new FetchedData(null, currentLine.trim()));
			}

		}

		sc.close();

		return list;
	}

	/**
	 * Automatically fills the data pieces of a fetched data with the data that
	 * it contains and the given titles. The data to be added is separated by
	 * whitespace.
	 * 
	 * @param data
	 *            The fetched data in which data pieces must be filled
	 * @param eltCount
	 *            The number of elements to be read
	 * @param titles
	 *            The list of titles matching the data pieces
	 */
	public static void fillDataPieces(FetchedData data, int eltCount, String[] titles) {
		if (data.data != null && !data.data.equals("")) {
			Scanner sc = new Scanner(data.data);

			for (int i = 0; i < eltCount; i++) {
				data.addDataPiece(new FetchedData(titles[i], sc.next()));
			}

			sc.close();

		} else {
			throw new ServerFault("Formatter.fillDataPieces()");
		}
	}

	/**
	 * Find matches from a given data and a pattern
	 * 
	 * @param data
	 *            The data to be analyzed
	 * @param pattern
	 *            The pattern to be used
	 * @return The matching string
	 */
	public static String getMatches(String data, String pattern) throws NoSuchElementException {
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(data);
		String result = new String();

		while (m.find()) {
			result += m.group();
		}

		if (result.equals("") || result.equals("/")) {
			throw new NoSuchElementException("Formatter.getMatches()");
		}

		return result;
	}

	/**
	 * Transforms line breaks into the \n character
	 * 
	 * @param data
	 *            the data in which the line breaks need to be transformed
	 * @return
	 */
	public static String transformLineBreaksToChar(String data) {
		// TODO throw possible ?
		return data.replace("\n", "\\n");
	}

	/**
	 * Transforms human readable size prepend by M, K, G \n character
	 * 
	 * @param size
	 * 
	 * @return size in bytes
	 */
	public static double humanReadabletoBytes(String size) {

		double ret = Double.parseDouble(size.substring(0, size.length() - 1));
		switch (size.charAt(size.length() - 1)) {
		case 'g':
		case 'G':
			return ret * GB_FACTOR;
		case 'm':
		case 'M':
			return ret * MB_FACTOR;
		case 'k':
		case 'K':
			return ret * KB_FACTOR;
		}
		return -1;
	}
}

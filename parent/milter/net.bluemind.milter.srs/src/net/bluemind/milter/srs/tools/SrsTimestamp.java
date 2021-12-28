/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.milter.srs.tools;

import java.util.concurrent.TimeUnit;

import com.google.common.base.Strings;

public class SrsTimestamp {
	@SuppressWarnings("serial")
	private static class InvalidChar extends RuntimeException {
	}

	private static final int timePrecision = new Long(TimeUnit.DAYS.toSeconds(1)).intValue();

	private static final String timeBaseChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
	private static final char[] timeBaseCharsArray = timeBaseChars.toCharArray();
	private static final int timeBaseBits = 5; // 2^5 = 32 = timeBaseChars.lenght

	private static final int timeSize = 2; // Encoded timestamp string length
	// number of possible timestamps values
	private static final int timeSlot = new Double(Math.pow(timeBaseChars.length(), timeSize)).intValue();

	private static final int maxAge = 10; // SRS timestamp maximum validity in days

	public static String from(long timestamp) {
		long ts = timestamp / timePrecision;
		int second = new Long(ts & ((1 << timeBaseBits) - 1)).intValue();

		ts = ts >> timeBaseBits;
		int first = new Long(ts & ((1 << timeBaseBits) - 1)).intValue();

		return new StringBuilder().append(timeBaseCharsArray[first]).append(timeBaseCharsArray[second]).toString();
	}

	public static boolean check(String srsTimeStamp) {
		if (Strings.isNullOrEmpty(srsTimeStamp) || srsTimeStamp.length() > 2) {
			return false;
		}

		char[] sts = srsTimeStamp.toCharArray();

		int then = 0;
		try {
			then = updateThen(then, sts[0]);
			then = updateThen(then, sts[1]);
		} catch (InvalidChar ic) {
			return false;
		}

		long now = (System.currentTimeMillis() / TimeUnit.SECONDS.toMillis(1) / timePrecision) % timeSlot;

		while (now < then) {
			now = now + timeSlot;
		}

		if (now <= then + maxAge) {
			return true;
		}

		return false;
	}

	private static int updateThen(int then, char c) {
		int pos = timeBaseChars.indexOf(Character.toUpperCase(c));
		if (pos == -1) {
			throw new InvalidChar();
		}

		return (then << timeBaseBits) | pos;
	}
}

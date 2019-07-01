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
package net.bluemind.imap;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

import junit.framework.TestCase;
import net.bluemind.imap.impl.DecoderUtils;
import net.bluemind.imap.impl.DecodingException;

public class DateParsingSpeedTests extends TestCase {

	int CNT = 20000;

	public void testOldMethod() {
		// 22-Mar-2010 14:26:18 +0100

		SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss Z", Locale.US);
		String d = "22-Mar-2010 14:26:18 +0100";
		long time = System.currentTimeMillis();
		for (int i = 0; i < CNT; i++) {
			try {
				Date result = df.parse(d);
				assertNotNull(result);
			} catch (Exception e) {
				fail();
			}
		}
		time = System.currentTimeMillis() - time;
		System.out.println(getName() + " Done in " + time + "ms.");
	}

	public void testJavaTime() {
		DateTimeFormatter df = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss Z");
		df = df.withLocale(Locale.US);
		String d = "22-Mar-2010 14:26:18 +0100";

		long time = System.currentTimeMillis();
		for (int i = 0; i < CNT; i++) {
			try {
				Date result = Date.from(ZonedDateTime.parse(d.trim(), df).toInstant());
				assertNotNull(result);
			} catch (Exception e) {
				fail();
			}
		}
		time = System.currentTimeMillis() - time;
		System.out.println(getName() + " Done in " + time + "ms.");

	}

	public void testJames() {
		String d2 = " 9-Dec-2012 18:38:26 +0100";
		String d = "22-Mar-2010 14:26:18 +0100";

		try {
			Date first = DecoderUtils.decodeDateTime(d2);
			assertNotNull(first);
		} catch (DecodingException e1) {
			e1.printStackTrace();
		}

		long time = System.currentTimeMillis();
		for (int i = 0; i < CNT; i++) {
			try {
				Date result = DecoderUtils.decodeDateTime(d);
				assertNotNull(result);
			} catch (Exception e) {
				fail();
			}
		}
		time = System.currentTimeMillis() - time;
		System.out.println(getName() + " Done in " + time + "ms.");

	}

	public void testEquals() throws ParseException, DecodingException {
		DateTimeFormatter df = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss Z");
		df = df.withLocale(Locale.US);
		String d = " 5-Apr-2012 10:54:38 +0200";
		Date joda = Date.from(ZonedDateTime.parse(d.trim(), df).toInstant());
		SimpleDateFormat jdkDf = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss Z", Locale.US);
		Date jdk = jdkDf.parse(d);

		assertEquals(jdk, joda);
		Date james = DecoderUtils.decodeDateTime(d);
		System.out.println("james: " + james);
		assertEquals(jdk, james);
	}
}

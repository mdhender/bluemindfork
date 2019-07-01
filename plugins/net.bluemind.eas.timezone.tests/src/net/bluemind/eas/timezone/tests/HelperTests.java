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
package net.bluemind.eas.timezone.tests;

import static org.junit.Assert.assertNotNull;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.junit.Test;

import net.bluemind.eas.timezone.EASTimeZone;
import net.bluemind.eas.timezone.EASTimeZoneHelper;
import net.bluemind.eas.timezone.TimeZoneCodec;

public class HelperTests {

	@Test
	public void convertEuropeParis() {
		TimeZone javaTz = TimeZone.getTimeZone("Europe/Paris");
		System.out.println("rawOffset: " + javaTz.getRawOffset());
		EASTimeZone easTz = EASTimeZoneHelper.from(javaTz);
		assertNotNull(easTz);
		System.out.println(easTz.toString());
	}

	@Test
	public void convertPacific() {
		TimeZone javaTz = TimeZone.getTimeZone("America/Los_Angeles");
		EASTimeZone easTz = EASTimeZoneHelper.from(javaTz);
		assertNotNull(easTz);
		System.out.println(easTz.toString());
	}

	@Test
	public void convertWAT() {
		TimeZone javaTz = TimeZone.getTimeZone("Africa/Libreville");
		System.out.println("id: " + javaTz.getID());
		Calendar cal = new GregorianCalendar();
		cal.setTimeZone(javaTz);
		cal.setTime(new Date());
		Date now = cal.getTime();
		System.out.println(now);
		EASTimeZone easTz = EASTimeZoneHelper.from(javaTz);
		assertNotNull(easTz);
		System.out.println(easTz.toString());
	}

	@Test
	public void understandIPhoneLibreville() {
		String raw = "xP///wAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAxP///w==";
		EASTimeZone decoded = TimeZoneCodec.decode(raw);
		System.out.println("decodes to " + decoded.toString());
		TimeZone javaTz = EASTimeZoneHelper.from(decoded);
		System.out.println("Found " + javaTz.getID());
	}

	@Test
	public void understandIPhoneTz() {
		String raw = "xP///wAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAoAAAAFAAMAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAMAAAAFAAIAAAAAAAAAxP///w==";
		EASTimeZone decoded = TimeZoneCodec.decode(raw);
		System.out.println("decodes to " + decoded.toString());
		TimeZone javaTz = EASTimeZoneHelper.from(decoded);
		System.out.println("Found " + javaTz.getID());
	}

	@Test
	public void understandAndroidTz() {
		String raw = "xP///wAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAoAAAAFAAMAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAMAAAAEAAIAAAAAAAAAxP///w==";
		EASTimeZone decoded = TimeZoneCodec.decode(raw);
		System.out.println("decodes to " + decoded.toString());
		TimeZone javaTz = EASTimeZoneHelper.from(decoded);
		System.out.println("Found " + javaTz.getID());
	}

	@Test
	public void convertAST() {
		TimeZone javaTz = TimeZone.getTimeZone("AST");
		EASTimeZone easTz = EASTimeZoneHelper.from(javaTz);
		assertNotNull(easTz);
		System.out.println(easTz.toString());
	}

}

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.TimeZone;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import io.netty.buffer.ByteBufAllocatorMetric;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import net.bluemind.eas.timezone.EASTimeZone;
import net.bluemind.eas.timezone.EASTimeZoneHelper;
import net.bluemind.eas.timezone.TimeZoneCodec;

public class HelperTests {

	private static long beforeDirect;
	private static Level prevLvl;

	@BeforeClass
	public static void leaks() {
		ByteBufAllocatorMetric metrics = UnpooledByteBufAllocator.DEFAULT.metric();
		beforeDirect = metrics.usedDirectMemory();
		prevLvl = ResourceLeakDetector.getLevel();
		ResourceLeakDetector.setLevel(Level.PARANOID);
	}

	@AfterClass
	public static void unleaks() {
		ByteBufAllocatorMetric metrics = UnpooledByteBufAllocator.DEFAULT.metric();
		System.err.println("direct usage: " + metrics.usedDirectMemory() + ", before: " + beforeDirect);
		ResourceLeakDetector.setLevel(prevLvl);
	}

	@Test
	public void americaLosAngeles() {
		String raw = "4AEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAsAAAABAAIAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAMAAAACAAIAAAAAAAAAxP///w==";
		compare(raw, "America/Los_Angeles");
	}

	@Test
	public void americaNewYork() {
		String raw = "LAEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAsAAAABAAIAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAMAAAACAAIAAAAAAAAAxP///w==";
		compare(raw, "America/New_York");
	}

	@Test
	public void asiaHoChiMinh() {
		String raw = "XP7//wAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAxP///w==";
		compare(raw, "Asia/Ho_Chi_Minh");
	}

	@Test
	public void asiaShanghai() {
		String raw = "IP7//wAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAxP///w==";
		compare(raw, "Asia/Shanghai");
	}

	@Test
	public void asiaKabul() {
		String raw = "8v7//wAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAxP///w==";
		compare(raw, "Asia/Kabul");
	}

	@Test
	public void europeLondon() {
		String raw = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAoAAAAFAAIAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAMAAAAFAAEAAAAAAAAAxP///w==";
		compare(raw, "Europe/London");
	}

	@Test
	public void europeRiga() {
		String raw = "iP///wAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAoAAAAFAAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAMAAAAFAAMAAAAAAAAAxP///w==";
		compare(raw, "Europe/Riga");
	}

	@Test
	public void africaJohannesbourg() {
		String raw = "iP///wAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAxP///w==";
		compare(raw, "Africa/Johannesbourg");
	}

	@Test
	public void africaLibreville() {
		String raw = "xP///wAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAxP///w==";
		compare(raw, "Africa/Libreville");
	}

	@Test
	public void europeMoscow() {
		String raw = "TP///wAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAxP///w==";
		compare(raw, "Europe/Moscow");
	}

	@Test
	public void africaDubai() {
		String raw = "EP///wAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAxP///w==";
		compare(raw, "Africa/Dubai");
	}

	@Test
	public void americaGuatemala() {
		String raw = "aAEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAxP///w==";
		compare(raw, "America/Guatemala");
	}

	@Test
	public void americaBogota() {
		String raw = "LAEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAxP///w==";
		compare(raw, "America/Bogota");
	}

	@Test
	public void atlanticReykjavik() {
		String raw = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAxP///w==";
		compare(raw, "Atlantic/Reykjavik");
	}

	@Test
	public void pacificWallis() {
		String raw = "MP3//wAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAxP///w==";
		compare(raw, "Pacific/Wallis");
	}

	@Test
	public void antarcticaMacquarie() {
		String raw = "bP3//wAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAxP///w==";
		compare(raw, "Antarctica/Macquarie");
	}

	@Test
	public void convertAST() {
		TimeZone javaTz = TimeZone.getTimeZone("AST");
		EASTimeZone easTz = EASTimeZoneHelper.from(javaTz);
		assertNotNull(easTz);
		System.out.println(easTz.toString());
	}

	@Test
	public void americaToronto() {
		String raw = "LAEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAsAAAABAAIAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAMAAAACAAIAAAAAAAAAxP///w==";
		EASTimeZone decoded = TimeZoneCodec.decode(raw);
		System.out.println("decodes to " + decoded.toString());

		assertEquals(0, decoded.standardDate.year);
		assertEquals(11, decoded.standardDate.month); // november
		assertEquals(0, decoded.standardDate.dayOfWeek); // sunday
		assertEquals(1, decoded.standardDate.day); // 1st
		assertEquals(2, decoded.standardDate.hour);
		assertEquals(0, decoded.standardDate.minute);
		assertEquals(0, decoded.standardDate.second);
		assertEquals(0, decoded.standardDate.ms);

		assertEquals(0, decoded.daylightDate.year);
		assertEquals(3, decoded.daylightDate.month); // march
		assertEquals(0, decoded.daylightDate.dayOfWeek); // sunday
		assertEquals(2, decoded.daylightDate.day); // 2nd
		assertEquals(2, decoded.daylightDate.hour);
		assertEquals(0, decoded.daylightDate.minute);
		assertEquals(0, decoded.daylightDate.second);
		assertEquals(0, decoded.daylightDate.ms);

		TimeZone tz = TimeZone.getTimeZone("America/Toronto");
		EASTimeZone easTz = EASTimeZoneHelper.from(tz);
		System.out.println(easTz.toString());

		assertEquals(decoded.standardDate, easTz.standardDate);
		assertEquals(decoded.daylightDate, easTz.daylightDate);
	}

	@Test
	public void europeParis() {
		String raw = "xP///wAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAoAAAAFAAMAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAMAAAAFAAIAAAAAAAAAxP///w==";
		EASTimeZone decoded = TimeZoneCodec.decode(raw);
		System.out.println(decoded.toString());

		assertEquals(0, decoded.standardDate.year);
		assertEquals(10, decoded.standardDate.month); // october
		assertEquals(0, decoded.standardDate.dayOfWeek); // sunday
		assertEquals(5, decoded.standardDate.day); // last
		assertEquals(3, decoded.standardDate.hour);
		assertEquals(0, decoded.standardDate.minute);
		assertEquals(0, decoded.standardDate.second);
		assertEquals(0, decoded.standardDate.ms);

		assertEquals(0, decoded.daylightDate.year);
		assertEquals(3, decoded.daylightDate.month); // march
		assertEquals(0, decoded.daylightDate.dayOfWeek); // sunday
		assertEquals(5, decoded.daylightDate.day); // last
		assertEquals(2, decoded.daylightDate.hour);
		assertEquals(0, decoded.daylightDate.minute);
		assertEquals(0, decoded.daylightDate.second);
		assertEquals(0, decoded.daylightDate.ms);

		TimeZone tz = TimeZone.getTimeZone("Europe/Paris");
		EASTimeZone easTz = EASTimeZoneHelper.from(tz);
		System.out.println(easTz.toString());

		assertEquals(decoded.standardDate, easTz.standardDate);
		assertEquals(decoded.daylightDate, easTz.daylightDate);
	}

	private void compare(String mobileTimezone, String timezone) {
		EASTimeZone decoded = TimeZoneCodec.decode(mobileTimezone);
		System.out.println(decoded.toString());

		TimeZone javaTz = TimeZone.getTimeZone(timezone);
		EASTimeZone easTz = EASTimeZoneHelper.from(javaTz);
		System.out.println(easTz.toString());

		assertEquals(decoded.standardDate, easTz.standardDate);
		assertEquals(decoded.daylightDate, easTz.daylightDate);
	}

}

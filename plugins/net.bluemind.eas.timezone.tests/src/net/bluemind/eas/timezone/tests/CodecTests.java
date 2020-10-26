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

import org.junit.Test;

import net.bluemind.eas.timezone.EASTimeZone;
import net.bluemind.eas.timezone.TimeZoneCodec;

public class CodecTests {

	@Test
	public void parseHardcodedTz() {
		String hardcoded = "xP///wAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAoAAAAFAAMAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAMAAAAFAAIAAAAAAAAAxP///w==";

		EASTimeZone decoded = TimeZoneCodec.decode(hardcoded);
		assertNotNull(decoded);
		assertEquals(-60, decoded.bias);
		assertEquals("", decoded.standardName);
		assertEquals("", decoded.daylightName);
		System.out.println("tz: " + decoded.toString());
	}

	@Test
	public void parseFromSpec() {
		String hardcoded = "4AEAAFAAYQBjAGkAZgBpAGMAIABTAHQAYQBuAGQAYQByAGQAIABUAGkAbQBlAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAsAAAABAAIAAAAAAAAAAAAAAFAAYQBjAGkAZgBpAGMAIABEAGEAeQBsAGkAZwBoAHQAIABUAGkAbQBlAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAMAAAACAAIAAAAAAAAAxP///w==";

		EASTimeZone decoded = TimeZoneCodec.decode(hardcoded);
		assertNotNull(decoded);

		assertEquals(480, decoded.bias);

		assertEquals("Pacific Standard Time", decoded.standardName);
		assertEquals(0, decoded.standardDate.year);
		assertEquals(11, decoded.standardDate.month); // november
		assertEquals(0, decoded.standardDate.dayOfWeek); // sunday
		assertEquals(1, decoded.standardDate.day); // 1st
		assertEquals(2, decoded.standardDate.hour);
		assertEquals(0, decoded.standardDate.minute);
		assertEquals(0, decoded.standardDate.second);
		assertEquals(0, decoded.standardDate.ms);

		assertEquals("Pacific Daylight Time", decoded.daylightName);
		assertEquals(0, decoded.daylightDate.year);
		assertEquals(3, decoded.daylightDate.month); // march
		assertEquals(0, decoded.daylightDate.dayOfWeek); // sunday
		assertEquals(2, decoded.daylightDate.day); // 2nd
		assertEquals(2, decoded.daylightDate.hour);
		assertEquals(0, decoded.daylightDate.minute);
		assertEquals(0, decoded.daylightDate.second);
		assertEquals(0, decoded.daylightDate.ms);

		assertEquals(-60, decoded.daylightBias);

		System.out.println("tz: " + decoded.toString());
	}

	@Test
	public void parseAndRewrite() {
		String hardcoded = "4AEAAFAAYQBjAGkAZgBpAGMAIABTAHQAYQBuAGQAYQByAGQAIABUAGkAbQBlAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAsAAAABAAIAAAAAAAAAAAAAAFAAYQBjAGkAZgBpAGMAIABEAGEAeQBsAGkAZwBoAHQAIABUAGkAbQBlAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAMAAAACAAIAAAAAAAAAxP///w==";

		EASTimeZone decoded = TimeZoneCodec.decode(hardcoded);
		assertNotNull(decoded);
		String recoded = decoded.toBase64();
		assertNotNull(recoded);
		System.out.println("recoded: " + recoded);
		assertEquals(hardcoded, recoded);
	}

	@Test
	public void parseOutlookForMobileTz() {
		String hardcoded = "xP///ygAVQBUAEMAKwAwADEAOgAwADAAKQAgAEIAcgB1AHMAcwBlAGwAcwAsACAAQwBvAHAAZQBuAGgAYQBnAGUAbgAAAAoAAAAFAAMAAAAAAAAAAAAAACgAVQBUAEMAKwAwADEAOgAwADAAKQAgAEIAcgB1AHMAcwBlAGwAcwAsACAAQwBvAHAAZQBuAGgAYQBnAGUAbgAAAAMAAAAFAAIAAAAAAAAAxP///w==";

		EASTimeZone decoded = TimeZoneCodec.decode(hardcoded);
		assertNotNull(decoded);

		assertEquals(-60, decoded.bias);

		assertEquals("(UTC+01:00) Brussels, Copenhagen", decoded.standardName);
		assertEquals(0, decoded.standardDate.year);
		assertEquals(10, decoded.standardDate.month); // october
		assertEquals(0, decoded.standardDate.dayOfWeek); // sunday
		assertEquals(5, decoded.standardDate.day); // last
		assertEquals(3, decoded.standardDate.hour);
		assertEquals(0, decoded.standardDate.minute);
		assertEquals(0, decoded.standardDate.second);
		assertEquals(0, decoded.standardDate.ms);

		assertEquals("(UTC+01:00) Brussels, Copenhagen", decoded.daylightName);
		assertEquals(0, decoded.daylightDate.year);
		assertEquals(3, decoded.daylightDate.month); // march
		assertEquals(0, decoded.daylightDate.dayOfWeek); // sunday
		assertEquals(5, decoded.daylightDate.day); // last
		assertEquals(2, decoded.daylightDate.hour);
		assertEquals(0, decoded.daylightDate.minute);
		assertEquals(0, decoded.daylightDate.second);
		assertEquals(0, decoded.daylightDate.ms);

		assertEquals(-60, decoded.daylightBias);

		System.out.println("tz: " + decoded.toString());

	}

	@Test
	public void parseAmericaTorontoTz() {
		// from iOS
		String hardcoded = "LAEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAsAAAABAAIAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAMAAAACAAIAAAAAAAAAxP///w==";

		EASTimeZone decoded = TimeZoneCodec.decode(hardcoded);
		assertNotNull(decoded);

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

		System.out.println("tz: " + decoded.toString());

	}

}

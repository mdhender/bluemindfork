/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.core.api.date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;

public class BmDateTimeValidatorTest {

	private BmDateTimeValidator validator = new BmDateTimeValidator();

	@Test
	public void testNullOrEmptyIso8601() {
		BmDateTime date = new BmDateTime(null, "Europe/Paris", Precision.Date);

		try {
			validator.validate(date);
			fail("should fail: iso8601 is null");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.INVALID_PARAMETER, e.getCode());
		}

		date = new BmDateTime("", "Europe/Paris", Precision.Date);

		try {
			validator.validate(date);
			fail("should fail: iso8601 is null");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.INVALID_PARAMETER, e.getCode());
		}

	}

	@Test
	public void testInvalidIso8601Date() {
		BmDateTime date = new BmDateTime("toto", null, Precision.Date);
		try {
			validator.validate(date);
			fail("should fail 'toto' is not iso8601");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.INVALID_PARAMETER, e.getCode());
		}

		date = new BmDateTime("1982-02-13", "Europe/Paris", Precision.Date);
		try {
			validator.validate(date);
			fail("should fail precision date but timezone is set");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.INVALID_PARAMETER, e.getCode());
		}

		date = new BmDateTime("1983-02-13T21:00:00+01:00", null, Precision.Date);
		try {
			validator.validate(date);
			fail("should fail precision date but time is set");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.INVALID_PARAMETER, e.getCode());
		}
	}

	@Test
	public void testInvalidIso8601DateTime() {

		BmDateTime date = new BmDateTime("toto", "Europe/Paris", Precision.DateTime);
		try {
			validator.validate(date);
			fail("should fail 'toto' is not iso8601");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.INVALID_PARAMETER, e.getCode());
		}

		date = new BmDateTime("13/02/1983", "Europe/Paris", Precision.DateTime);
		try {
			validator.validate(date);
			fail("should fail '13/02/1983' is not iso8601");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.INVALID_PARAMETER, e.getCode());
		}

		date = new BmDateTime("1983-02-13", "Europe/Paris", Precision.DateTime);
		try {
			validator.validate(date);
			fail("should fail time is missing");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.INVALID_PARAMETER, e.getCode());
		}

	}

	@Test
	public void testInvalidTZ() {
		BmDateTime date = new BmDateTime("1983-02-13T21:00:00+01:00", "Pacific/Honolulu", Precision.DateTime);
		try {
			validator.validate(date);
			fail("invalid tz");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.INVALID_PARAMETER, e.getCode());
		}

		date = new BmDateTime("1983-02-13T21:00:00+12:00", "Romance Standard Time", Precision.DateTime);
		try {
			validator.validate(date);
			fail("invalid tz");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.INVALID_PARAMETER, e.getCode());
		}

		date = new BmDateTime("1983-02-13T21:00:00+02:00", "SE Asia Standard Time", Precision.DateTime);
		try {
			validator.validate(date);
			fail("invalid tz");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.INVALID_PARAMETER, e.getCode());
		}

		date = new BmDateTime("1983-02-13T21:00:00+01:00", "WAT", Precision.DateTime);
		try {
			validator.validate(date);
			fail("invalid tz");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.INVALID_PARAMETER, e.getCode());
		}

	}

	@Test
	public void testValidTZ() {

		BmDateTime date = new BmDateTime("1983-02-13T21:00:00+01:00", "Romance Standard Time", Precision.DateTime);
		try {
			validator.validate(date);
		} catch (ServerFault e) {
			fail("1983-02-13T21:00:00+01:00 is in Romance Standard Time");
		}

		date = new BmDateTime("1983-02-13T21:00:00+07:00", "SE Asia Standard Time", Precision.DateTime);
		try {
			validator.validate(date);
		} catch (ServerFault e) {
			fail("1983-02-13T21:00:00+07:00 is in SE Asia Standard Time");
		}

		date = new BmDateTime("1983-02-13T21:00:00+01:00", null, Precision.DateTime);
		try {
			validator.validate(date);
		} catch (ServerFault e) {
			fail("1983-02-13T21:00:00+01:00 DatTime without TZ is valid");
		}

		date = new BmDateTime("1983-02-13T21:00:00+01:00", "", Precision.DateTime);
		try {
			validator.validate(date);
		} catch (ServerFault e) {
			fail("1983-02-13T21:00:00+01:00 DatTime without TZ is valid");
		}

	}

	@Test
	public void testEmptyPrecision() {
		BmDateTime date = new BmDateTime("1983-02-13T21:00:00+01:00", null, null);
		try {
			validator.validate(date);
			fail("should fail precision is null");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.INVALID_PARAMETER, e.getCode());
		}
	}

	@Test
	public void validDateTime() {
		BmDateTime date = new BmDateTime("1983-02-13T21:00:00+01:00", "Europe/Paris", Precision.DateTime);
		try {
			validator.validate(date);
		} catch (ServerFault e) {
			fail("should not fail '1983-02-13T21:00:00+01:00' is valid");
		}

		date = new BmDateTime("1983-02-13T21:00:00+01:00", "Europe/Berlin", Precision.DateTime);
		try {
			validator.validate(date);
		} catch (ServerFault e) {
			fail("should not fail '1983-02-13T21:00:00+01:00' is valid");
		}

	}

	@Test
	public void validDate() {
		BmDateTime date = new BmDateTime("1983-02-13", null, Precision.Date);
		try {
			validator.validate(date);
		} catch (ServerFault e) {
			fail("should not fail '1983-02-13' is valid");
		}
	}
}

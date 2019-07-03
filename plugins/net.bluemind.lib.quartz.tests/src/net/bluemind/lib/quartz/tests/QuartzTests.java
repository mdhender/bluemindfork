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
package net.bluemind.lib.quartz.tests;

import java.text.ParseException;
import java.util.Date;

import org.quartz.CronExpression;

import junit.framework.TestCase;

public class QuartzTests extends TestCase {

	public static final String EVERY_MINUTE = "0 * * * * ?";
	public static final String AT_MIDNIGHT = "0 59 23 * * ?";

	public void testClasspathIsOk() {
		verifyCronString("0 * * * * ?");
	}

	public void testCronConstants() {
		verifyCronString(AT_MIDNIGHT);
		verifyCronString(EVERY_MINUTE);
	}

	private void verifyCronString(String c) {
		try {
			CronExpression ce = new CronExpression(c);
			assertNotNull(ce);
			Date d = new Date();
			Date nextValid = ce.getNextValidTimeAfter(d);
			assertNotNull(nextValid);
			System.out.println("Next valid time after " + d + " is " + nextValid);
		} catch (ParseException e) {
			fail("A cron expression was refused: " + c + " (" + e.getMessage() + ")");
		}
	}

	public void testsCrons() {
		verifyCronString("0 */80 * * * ?");
	}

}

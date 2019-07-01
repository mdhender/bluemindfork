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
package net.bluemind.reminder.job;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.icalendar.api.ICalendarElement;

public class ExtensionLookupTests {

	@Test
	public void testLookup() {
		Assert.assertEquals(2, getJobImplementations().size());
	}

	<T extends ICalendarElement> List<IAlarmSupport<T>> getJobImplementations() {
		RunnableExtensionLoader<IAlarmSupport<T>> epLoader = new RunnableExtensionLoader<>();
		List<IAlarmSupport<T>> extensions = epLoader.loadExtensions("net.bluemind.reminder", "job", "job_provider",
				"implementation");
		return extensions;
	}

}

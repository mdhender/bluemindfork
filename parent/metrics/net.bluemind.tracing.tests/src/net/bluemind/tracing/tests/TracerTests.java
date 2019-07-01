/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
 *
 * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License)
 * or the CeCILL as published by CeCILL.info (version 2 of the License).
 *
 * There are special exceptions to the terms and conditions of the
 * licenses as they are applied to this program. See LICENSE.txt in
 * the directory of this program distribution.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.tracing.tests;

import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;

import io.opentracing.Span;
import io.opentracing.Tracer;
import net.bluemind.tracing.TracingSupport;

public class TracerTests {

	@BeforeClass
	public static void beforeClass() {
		System.setProperty("net.bluemind.property.product", "tracing-junit");
	}

	@Test
	public void loadTracer() {
		Tracer tracer = TracingSupport.tracer();
		assertNotNull(tracer);
		Span currentSpan = tracer.activeSpan();
		System.out.println("current: " + currentSpan);
	}

}

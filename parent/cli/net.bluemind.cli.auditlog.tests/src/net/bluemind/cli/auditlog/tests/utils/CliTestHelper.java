/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
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

package net.bluemind.cli.auditlog.tests.utils;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class CliTestHelper {

	private ByteArrayOutputStream outAndErr;
	private PrintStream origOut;
	private PrintStream origErr;

	public CliTestHelper() {
		this.outAndErr = new ByteArrayOutputStream();
		this.origOut = System.out;
		this.origErr = System.err;

	}

	public String outputAndReset() {
		String out = new String(outAndErr.toByteArray());
		outAndErr.reset();
		return out;
	}

	public void beforeTest() throws Exception {
		System.err.println("Starting redirect");
		PrintStream globalCopy = new PrintStream(outAndErr);
		System.setOut(new TeeStream(System.out, globalCopy));
		System.setErr(new TeeStream(System.err, globalCopy));
	}

	public void afterTest() throws Exception {
		outAndErr.reset();
	}

	public void afterClassTest() throws Exception {
		System.setOut(origOut);
		System.setErr(origErr);
		outAndErr.reset();
	}

}

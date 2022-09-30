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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.utils.tests;

import org.junit.Assert;
import org.junit.Test;

import net.bluemind.utils.ByteSizeUnit;

public class ByteSizeUnitTests {

	static final long C0 = 1L;
	static final long C1 = C0 * 1024L;
	static final long C2 = C1 * 1024L;
	static final long C3 = C2 * 1024L;
	static final long C4 = C3 * 1024L;
	static final long C5 = C4 * 1024L;

	@Test
	public void testByteFrom() {

		long result = 0L;
		result = ByteSizeUnit.BYTES.fromBytes(C0);
		Assert.assertEquals(C0, result);

		result = ByteSizeUnit.BYTES.fromKB(C0);
		Assert.assertEquals(C1, result);

		result = ByteSizeUnit.BYTES.fromMB(C0);
		Assert.assertEquals(C2, result);

		result = ByteSizeUnit.BYTES.fromGB(C0);
		Assert.assertEquals(C3, result);

		result = ByteSizeUnit.BYTES.fromTB(C0);
		Assert.assertEquals(C4, result);

		result = ByteSizeUnit.BYTES.fromPB(C0);
		Assert.assertEquals(C5, result);
	}

	@Test
	public void testByteTo() {

		long result = 0L;

		result = ByteSizeUnit.BYTES.toBytes(C0);
		Assert.assertEquals(C0, result);

		result = ByteSizeUnit.BYTES.toKB(C1);
		Assert.assertEquals(C0, result);

		result = ByteSizeUnit.BYTES.toMB(C2);
		Assert.assertEquals(C0, result);

		result = ByteSizeUnit.BYTES.toGB(C3);
		Assert.assertEquals(C0, result);

		result = ByteSizeUnit.BYTES.toTB(C4);
		Assert.assertEquals(C0, result);

		result = ByteSizeUnit.BYTES.toPB(C5);
		Assert.assertEquals(C0, result);
	}

	@Test
	public void testKBFrom() {

		long result = 0L;
		result = ByteSizeUnit.KB.fromBytes(C1);
		Assert.assertEquals(C0, result);

		result = ByteSizeUnit.KB.fromKB(C0);
		Assert.assertEquals(C0, result);

		result = ByteSizeUnit.KB.fromMB(C0);
		Assert.assertEquals(C1, result);

		result = ByteSizeUnit.KB.fromGB(C0);
		Assert.assertEquals(C2, result);

		result = ByteSizeUnit.KB.fromTB(C0);
		Assert.assertEquals(C3, result);

		result = ByteSizeUnit.KB.fromPB(C0);
		Assert.assertEquals(C4, result);
	}

	@Test
	public void testKBTo() {

		long result = 0L;

		result = ByteSizeUnit.KB.toBytes(C0);
		Assert.assertEquals(C1, result);

		result = ByteSizeUnit.KB.toKB(C0);
		Assert.assertEquals(C0, result);

		result = ByteSizeUnit.KB.toMB(C1);
		Assert.assertEquals(C0, result);

		result = ByteSizeUnit.KB.toGB(C2);
		Assert.assertEquals(C0, result);

		result = ByteSizeUnit.KB.toTB(C3);
		Assert.assertEquals(C0, result);

		result = ByteSizeUnit.KB.toPB(C4);
		Assert.assertEquals(C0, result);
	}

	@Test
	public void testMBFrom() {

		long result = 0L;
		result = ByteSizeUnit.MB.fromBytes(C2);
		Assert.assertEquals(C0, result);

		result = ByteSizeUnit.MB.fromKB(C1);
		Assert.assertEquals(C0, result);

		result = ByteSizeUnit.MB.fromMB(C0);
		Assert.assertEquals(C0, result);

		result = ByteSizeUnit.MB.fromGB(C0);
		Assert.assertEquals(C1, result);

		result = ByteSizeUnit.MB.fromTB(C0);
		Assert.assertEquals(C2, result);

		result = ByteSizeUnit.MB.fromPB(C0);
		Assert.assertEquals(C3, result);
	}

	@Test
	public void testMBTo() {

		long result = 0L;

		result = ByteSizeUnit.MB.toBytes(C0);
		Assert.assertEquals(C2, result);

		result = ByteSizeUnit.MB.toKB(C0);
		Assert.assertEquals(C1, result);

		result = ByteSizeUnit.MB.toMB(C0);
		Assert.assertEquals(C0, result);

		result = ByteSizeUnit.MB.toGB(C1);
		Assert.assertEquals(C0, result);

		result = ByteSizeUnit.MB.toTB(C2);
		Assert.assertEquals(C0, result);

		result = ByteSizeUnit.MB.toPB(C3);
		Assert.assertEquals(C0, result);
	}

	@Test
	public void testGBFrom() {

		long result = 0L;
		result = ByteSizeUnit.GB.fromBytes(C3);
		Assert.assertEquals(C0, result);

		result = ByteSizeUnit.GB.fromKB(C2);
		Assert.assertEquals(C0, result);

		result = ByteSizeUnit.GB.fromMB(C1);
		Assert.assertEquals(C0, result);

		result = ByteSizeUnit.GB.fromGB(C0);
		Assert.assertEquals(C0, result);

		result = ByteSizeUnit.GB.fromTB(C0);
		Assert.assertEquals(C1, result);

		result = ByteSizeUnit.GB.fromPB(C0);
		Assert.assertEquals(C2, result);
	}

	@Test
	public void testGBTo() {

		long result = 0L;

		result = ByteSizeUnit.GB.toBytes(C0);
		Assert.assertEquals(C3, result);

		result = ByteSizeUnit.GB.toKB(C0);
		Assert.assertEquals(C2, result);

		result = ByteSizeUnit.GB.toMB(C0);
		Assert.assertEquals(C1, result);

		result = ByteSizeUnit.GB.toGB(C0);
		Assert.assertEquals(C0, result);

		result = ByteSizeUnit.GB.toTB(C1);
		Assert.assertEquals(C0, result);

		result = ByteSizeUnit.GB.toPB(C2);
		Assert.assertEquals(C0, result);
	}

	@Test
	public void testTBFrom() {

		long result = 0L;
		result = ByteSizeUnit.TB.fromBytes(C4);
		Assert.assertEquals(C0, result);

		result = ByteSizeUnit.TB.fromKB(C3);
		Assert.assertEquals(C0, result);

		result = ByteSizeUnit.TB.fromMB(C2);
		Assert.assertEquals(C0, result);

		result = ByteSizeUnit.TB.fromGB(C1);
		Assert.assertEquals(C0, result);

		result = ByteSizeUnit.TB.fromTB(C0);
		Assert.assertEquals(C0, result);

		result = ByteSizeUnit.TB.fromPB(C0);
		Assert.assertEquals(C1, result);
	}

	@Test
	public void testTBTo() {

		long result = 0L;

		result = ByteSizeUnit.TB.toBytes(C0);
		Assert.assertEquals(C4, result);

		result = ByteSizeUnit.TB.toKB(C0);
		Assert.assertEquals(C3, result);

		result = ByteSizeUnit.TB.toMB(C0);
		Assert.assertEquals(C2, result);

		result = ByteSizeUnit.TB.toGB(C0);
		Assert.assertEquals(C1, result);

		result = ByteSizeUnit.TB.toTB(C0);
		Assert.assertEquals(C0, result);

		result = ByteSizeUnit.TB.toPB(C1);
		Assert.assertEquals(C0, result);
	}

	@Test
	public void testPBFrom() {

		long result = 0L;
		result = ByteSizeUnit.PB.fromBytes(C5);
		Assert.assertEquals(C0, result);

		result = ByteSizeUnit.PB.fromKB(C4);
		Assert.assertEquals(C0, result);

		result = ByteSizeUnit.PB.fromMB(C3);
		Assert.assertEquals(C0, result);

		result = ByteSizeUnit.PB.fromGB(C2);
		Assert.assertEquals(C0, result);

		result = ByteSizeUnit.PB.fromTB(C1);
		Assert.assertEquals(C0, result);

		result = ByteSizeUnit.PB.fromPB(C0);
		Assert.assertEquals(C0, result);
	}

	@Test
	public void testPBTo() {

		long result = 0L;

		result = ByteSizeUnit.PB.toBytes(C0);
		Assert.assertEquals(C5, result);

		result = ByteSizeUnit.PB.toKB(C0);
		Assert.assertEquals(C4, result);

		result = ByteSizeUnit.PB.toMB(C0);
		Assert.assertEquals(C3, result);

		result = ByteSizeUnit.PB.toGB(C0);
		Assert.assertEquals(C2, result);

		result = ByteSizeUnit.PB.toTB(C0);
		Assert.assertEquals(C1, result);

		result = ByteSizeUnit.PB.toPB(C0);
		Assert.assertEquals(C0, result);
	}

}

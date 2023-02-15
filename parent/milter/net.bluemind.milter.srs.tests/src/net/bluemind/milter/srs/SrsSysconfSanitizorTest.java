/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.milter.srs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import net.bluemind.system.api.SysConfKeys;

public class SrsSysconfSanitizorTest {
	@Test
	public void noSysconfValue() {
		Map<String, String> sysconfs = new HashMap<>();

		new SrsSysconfSanitizor().sanitize(null, sysconfs);
		assertFalse(sysconfs.containsKey(SysConfKeys.srs_disabled.name()));
	}

	@Test
	public void falseValue() {
		Map<String, String> sysconfs = new HashMap<>();
		sysconfs.put(SysConfKeys.srs_disabled.name(), Boolean.FALSE.toString());

		new SrsSysconfSanitizor().sanitize(null, sysconfs);
		assertEquals(Boolean.FALSE.toString(), sysconfs.get(SysConfKeys.srs_disabled.name()));
	}

	@Test
	public void invalidIsFalse() {
		Map<String, String> sysconfs = new HashMap<>();
		sysconfs.put(SysConfKeys.srs_disabled.name(), "invalid");

		new SrsSysconfSanitizor().sanitize(null, sysconfs);
		assertEquals(Boolean.FALSE.toString(), sysconfs.get(SysConfKeys.srs_disabled.name()));

		sysconfs = new HashMap<>();
		sysconfs.put(SysConfKeys.srs_disabled.name(), "");

		new SrsSysconfSanitizor().sanitize(null, sysconfs);
		assertEquals(Boolean.FALSE.toString(), sysconfs.get(SysConfKeys.srs_disabled.name()));
	}

	@Test
	public void nullIsFalse() {
		Map<String, String> sysconfs = new HashMap<>();
		sysconfs.put(SysConfKeys.srs_disabled.name(), null);

		new SrsSysconfSanitizor().sanitize(null, sysconfs);
		assertEquals(Boolean.FALSE.toString(), sysconfs.get(SysConfKeys.srs_disabled.name()));
	}

	@Test
	public void trueValue() {
		Map<String, String> sysconfs = new HashMap<>();
		sysconfs.put(SysConfKeys.srs_disabled.name(), Boolean.TRUE.toString());

		new SrsSysconfSanitizor().sanitize(null, sysconfs);
		assertEquals(Boolean.TRUE.toString(), sysconfs.get(SysConfKeys.srs_disabled.name()));
	}
}

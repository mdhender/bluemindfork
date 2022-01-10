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
package net.bluemind.backend.cyrus.replication.testhelper;

import java.util.Random;
import java.util.UUID;

import com.google.common.base.Strings;

public class CyrusGUID {

	private static final Random r = new Random();

	public static String randomGuid() {
		String left = UUID.randomUUID().toString().replace("-", "");
		String right = Strings.padStart(Integer.toHexString(r.nextInt()), 8, '0');
		return left + right;
	}

}

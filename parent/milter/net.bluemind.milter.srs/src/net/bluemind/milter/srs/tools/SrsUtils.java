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
package net.bluemind.milter.srs.tools;

import java.util.Optional;

import com.google.common.base.Strings;

public class SrsUtils {
	public static Optional<String> getDomainFromEmail(String email) {
		return Optional.ofNullable(email).map(e -> e.split("@")).filter(parts -> parts.length == 2)
				.map(parts -> parts[1]);
	}

	public static Optional<String> getLeftPartFromEmail(String email) {
		return Optional.ofNullable(email).map(e -> e.split("@")).map(parts -> parts[0])
				.filter(leftPart -> !Strings.isNullOrEmpty(leftPart));
	}
}

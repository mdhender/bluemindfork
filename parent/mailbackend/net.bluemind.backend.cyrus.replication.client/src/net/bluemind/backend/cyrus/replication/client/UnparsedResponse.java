/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
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
package net.bluemind.backend.cyrus.replication.client;

import java.util.List;

import com.google.common.collect.ImmutableList;

public class UnparsedResponse {
	public final List<String> dataLines;
	public final String statusResponse;

	public UnparsedResponse(String statusResponse, List<String> dataLines) {
		this.statusResponse = statusResponse;
		this.dataLines = ImmutableList.copyOf(dataLines);
	}

	public boolean isOk() {
		return statusResponse.startsWith("OK ") || statusResponse.startsWith("* OK ");
	}

	public String toString() {
		return String.format("[resp '%s' with %d frame(s)]", statusResponse, dataLines.size());
	}
}
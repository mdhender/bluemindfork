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
package net.bluemind.backend.cyrus.replication.server;

import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;

import com.google.common.base.MoreObjects;

public class ReplicationFrameBuilder {

	private final LinkedList<Token> tokens;

	private int openParens;
	private int closingParens;
	private boolean lastIsBinary;
	private long frameId;
	private CompletableFuture<Void> asyncPart;

	private boolean inQuotedString;

	public ReplicationFrameBuilder(long frameId) {
		openParens = 0;
		closingParens = 0;
		inQuotedString = false;
		this.frameId = frameId;
		tokens = new LinkedList<>();
		asyncPart = CompletableFuture.completedFuture(null);
	}

	public void add(Token t) {
		tokens.add(t);
		asyncPart = asyncPart.thenCompose(v -> t.asyncComponent());
		if (t.isBinary()) {
			lastIsBinary = true;
		} else {
			lastIsBinary = false;
			String asStr = t.value();

			asStr.chars().forEach(c -> {
				if (c == '"') {
					inQuotedString = !inQuotedString;
				}
				if (!inQuotedString) {
					if (c == '(') {
						openParens++;
					} else if (c == ')') {
						closingParens++;
					}
				}
			});
		}
	}

	public String toString() {
		return MoreObjects.toStringHelper(ReplicationFrameBuilder.class)//
				.add("lastIsBin", lastIsBinary)//
				.add("openParens", openParens)//
				.add("closingParens", closingParens)//
				.add("tokens", tokens).toString();
	}

	public boolean complete() {
		return !lastIsBinary && openParens == closingParens;
	}

	public ReplicationFrame build() {
		return new ReplicationFrame(frameId, tokens, asyncPart);
	}

}

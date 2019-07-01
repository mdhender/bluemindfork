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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

public class ReplicationFrame {

	private static final Logger logger = LoggerFactory.getLogger(ReplicationFrame.class);
	private LinkedList<Token> tokens;
	private long id;
	private CompletableFuture<ReplicationFrame> asyncPart;

	public ReplicationFrame(long frameId, LinkedList<Token> tokenQueue, CompletableFuture<Void> asyncPart) {
		this.id = frameId;
		this.tokens = mergeTextTokens(tokenQueue);
		this.asyncPart = asyncPart.thenApply(v -> this);
	}

	public CompletableFuture<ReplicationFrame> asyncComponent() {
		return asyncPart;
	}

	private LinkedList<Token> mergeTextTokens(LinkedList<Token> tokenQueue) {
		long time = System.currentTimeMillis();
		Token prev = null;
		LinkedList<Token> merged = new LinkedList<>();
		for (Token t : tokenQueue) {
			if (prev != null) {
				if (t.isBinary()) {
					merged.add(t);
					prev = null;
				} else {
					prev.merge(t);
				}
			} else {
				// can the first one be a binary ?
				if (t.isBinary()) {
					merged.add(t);
				} else {
					merged.add(t);
					prev = t;
				}
			}
		}
		time = System.currentTimeMillis() - time;
		if (time > 100) {
			logger.info("Merged token(s) in {}ms.", time);
		}
		return merged;
	}

	public int size() {
		return tokens.size();
	}

	public Token next() {
		return tokens.poll();
	}

	public String frameId() {
		return "[frame-" + Strings.padStart(Long.toString(id), 8, '0') + "]";
	}

	public String toString() {
		if (tokens.size() == 1) {
			Token merged = tokens.getFirst();
			String valToPrint = merged.isBinary() ? "[LITERAL " + merged.length() + "bytes]" : merged.value();
			if (valToPrint.length() > 2048) {
				valToPrint = valToPrint.substring(0, 2048) + "... [truncated]";
			}
			StringBuilder sb = new StringBuilder();
			sb.append(frameId()).append(": ");
			sb.append(valToPrint);
			return sb.toString();
		} else {
			StringBuilder sb = new StringBuilder();
			sb.append(frameId() + ": ");
			for (Token t : tokens) {
				sb.append('\n');
				if (t.isBinary()) {
					sb.append("[LITERAL " + t.length() + "bytes]");
				} else {
					sb.append(t.value());
				}
			}
			return sb.toString();
		}
	}

	public boolean hasNext() {
		return tokens.peek() != null;
	}

}

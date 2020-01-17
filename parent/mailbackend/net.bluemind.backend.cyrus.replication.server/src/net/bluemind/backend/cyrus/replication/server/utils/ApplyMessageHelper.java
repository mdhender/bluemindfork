/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.backend.cyrus.replication.server.utils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import io.vertx.core.buffer.Buffer;
import net.bluemind.backend.cyrus.replication.server.Token;
import net.bluemind.backend.cyrus.replication.server.state.MailboxMessage;
import net.bluemind.backend.cyrus.replication.server.state.MailboxMessage.MailboxMessageBuilder;

public class ApplyMessageHelper {

	private static final Splitter SPACES = Splitter.on(' ').omitEmptyStrings();
	private static final Logger logger = LoggerFactory.getLogger(ApplyMessageHelper.class);

	private static final int CHUNK_SIZE = 4;

	public static class MessagesBatch {
		private static final MailboxMessage[] EMPTY = new MailboxMessage[0];
		public final MailboxMessage[] toProcess;

		public MessagesBatch(List<MailboxMessage> msgs) {
			toProcess = msgs.toArray(EMPTY);
		}
	}

	public static Stream<MessagesBatch> process(String allTokens) {
		List<String> withoutNILNodes = SPACES.splitToList(allTokens).stream().filter(s -> !"NIL".equals(s))
				.collect(Collectors.toList());
		List<List<String>> partitionned = Lists.partition(withoutNILNodes, 3);
		List<MailboxMessage> toApply = partitionned.stream().map(threeElems -> {
			if (threeElems.size() != 3) {
				logger.error("Unbalanced list: {}", threeElems);
				return null;
			}
			// %{vagrant_vmw
			String prefixedPart = threeElems.get(0);
			String partition = prefixedPart.substring(2);

			// dd3b1e83bb56d757ed6d112252bbf4a959aaa032
			String guid = threeElems.get(1);

			// 1068}{tok1483204110572-1.bin}
			String lenAndTokenRef = threeElems.get(2);
			int lenEnd = lenAndTokenRef.indexOf('}');
			int len = Integer.parseInt(lenAndTokenRef.substring(0, lenEnd));
			int tokEnd = lenAndTokenRef.indexOf('}', lenEnd + 1);
			String tokRef = lenAndTokenRef.substring(lenEnd + 1, tokEnd + 1);

			MailboxMessageBuilder builder = MailboxMessage.builder();
			builder.partition(partition);
			builder.guid(guid);
			builder.length(len);
			builder.content(Token.of(Buffer.buffer(tokRef), false, null));
			return builder.build();
		}).filter(Objects::nonNull).collect(Collectors.toList());

		return Lists.partition(toApply, CHUNK_SIZE).stream().map(MessagesBatch::new);
	}

}

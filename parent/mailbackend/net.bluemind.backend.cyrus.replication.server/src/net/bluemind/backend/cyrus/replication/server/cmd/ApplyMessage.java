/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2016
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
package net.bluemind.backend.cyrus.replication.server.cmd;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.cyrus.replication.observers.IReplicationObserver;
import net.bluemind.backend.cyrus.replication.server.ReplicationFrame;
import net.bluemind.backend.cyrus.replication.server.ReplicationSession;
import net.bluemind.backend.cyrus.replication.server.Token;
import net.bluemind.backend.cyrus.replication.server.state.MailboxMessage;
import net.bluemind.backend.cyrus.replication.server.state.ReplicationState;
import net.bluemind.backend.cyrus.replication.server.utils.ApplyMessageHelper;

/**
 * APPLY MESSAGE (%{vagrant_vmw dd3b1e83bb56d757ed6d112252bbf4a959aaa032
 * 1068}{tok1483204110572-1.bin} %{vagrant_vmw
 * ddb2149aa1f99e80b2eed058704bf2f410d30c39 1299}{tok1483204110572-2.bin})
 * 
 * 
 */
public class ApplyMessage implements IAsyncReplicationCommand {

	private static final Logger logger = LoggerFactory.getLogger(ApplyMessage.class);
	private final List<IReplicationObserver> observers;

	public ApplyMessage(List<IReplicationObserver> observers) {
		this.observers = observers;
	}

	private static class Holder<T> {
		public T content;

		public Holder(T t) {
			this.content = t;
		}

		public T replace(Function<T, T> transformOld) {
			return (content = transformOld.apply(content));
		}
	}

	@Override
	public CompletableFuture<CommandResult> doIt(ReplicationSession session, Token t, ReplicationFrame frame) {
		long time = System.currentTimeMillis();
		String withVerb = t.value();
		String msgHeader = withVerb.substring("APPLY MESSAGE (".length());

		Stream<MailboxMessage> theStream = ApplyMessageHelper.process(msgHeader);
		CompletableFuture<Void> root = CompletableFuture.completedFuture(null);
		final Holder<CompletableFuture<Void>> rootRef = new Holder<>(root);
		final ReplicationState state = session.state();
		AtomicInteger count = new AtomicInteger();
		theStream.forEach(msg -> rootRef.replace(prev -> {
			count.incrementAndGet();
			return prev.thenCompose(v -> state.addMessage(msg));
		}));

		return rootRef.content.thenApply(v -> {
			long elapsed = System.currentTimeMillis() - time;
			logger.info("Finished ApplyMessage in {}ms.", elapsed);
			int total = count.get();
			observers.stream().forEach(ob -> ob.onApplyMessages(total));
			return CommandResult.success();
		});

	}

}

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
package net.bluemind.dataprotect.todolist.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerQuery;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.task.service.BlockingServerTask;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.dataprotect.api.DataProtectGeneration;
import net.bluemind.dataprotect.api.Restorable;
import net.bluemind.dataprotect.common.email.SendIcs;
import net.bluemind.dataprotect.service.BackupDataProvider;
import net.bluemind.dataprotect.service.action.EmailData;
import net.bluemind.dataprotect.service.action.IRestoreActionData;
import net.bluemind.dataprotect.service.action.RestoreActionExecutor;
import net.bluemind.lib.vertx.Result;
import net.bluemind.todolist.api.ITodoUids;
import net.bluemind.todolist.api.IVTodo;

public class SendUserTodolistsICSTasks extends BlockingServerTask implements IServerTask {
	private static final Logger logger = LoggerFactory.getLogger(SendUserTodolistsICSTasks.class);

	private final DataProtectGeneration backup;
	private final Restorable item;
	private final RestoreActionExecutor<EmailData> executor;
	private final ResourceBundle bundle;

	@SuppressWarnings("unchecked")
	public SendUserTodolistsICSTasks(DataProtectGeneration backup, Restorable item,
			RestoreActionExecutor<? extends IRestoreActionData> executor) {
		this.backup = backup;
		this.item = item;
		this.executor = (RestoreActionExecutor<EmailData>) executor;
		this.bundle = ResourceBundle.getBundle("OSGI-INF/l10n/RestoreTodo", Locale.of(ServerSideServiceProvider
				.getProvider(SecurityContext.SYSTEM).getContext().getSecurityContext().getLang()));
	}

	public static final SecurityContext as(String uid, String domainContainerUid) throws ServerFault {
		SecurityContext userContext = new SecurityContext(UUID.randomUUID().toString(), uid, Arrays.<String>asList(),
				Arrays.<String>asList(), Collections.emptyMap(), domainContainerUid, "en",
				"SendUserTodolistsICSTasks.as");
		Sessions.get().put(userContext.getSessionId(), userContext);
		return userContext;
	}

	@Override
	public void run(IServerTaskMonitor monitor) throws Exception {
		monitor.begin(10, String.format("Starting restore for uid %s", item.entryUid));
		SendIcs sendEmail = new SendIcs(item, executor, monitor);

		SecurityContext backUserContext = as(item.entryUid, item.domainUid);
		try (BackupDataProvider bdp = new BackupDataProvider(null, backUserContext, monitor)) {
			IServiceProvider back = bdp.createContextWithData(backup, item).provider();

			IContainers containersService = back.instance(IContainers.class);
			ContainerQuery cq = ContainerQuery.ownerAndType(backUserContext.getSubject(), ITodoUids.TYPE);
			List<ContainerDescriptor> lists = containersService.all(cq);

			Map<String, String> allIcs = new HashMap<String, String>(lists.size());
			for (ContainerDescriptor list : lists) {
				IVTodo service = back.instance(IVTodo.class, list.uid);
				allIcs.put(list.name, getIcs(service.exportAll()));
			}

			sendEmail.sendMessage(allIcs, bundle.getString("send.todo.restore.message"),
					bundle.getString("send.todo.restore.subject"));

		} catch (Exception e) {
			logger.error("Error while sending user Todo lists", e);
			monitor.end(false, "finished with errors : " + e.getMessage(), "[]");
			return;
		}

		monitor.end(true, "finished.", "[]");
	}

	private String getIcs(Stream stream) {
		return streamToString(stream);
	}

	private String streamToString(Stream stream) {
		final CountDownLatch latch = new CountDownLatch(1);
		final ReadStream<Buffer> reader = VertxStream.read(stream);
		final AccumulatorStream writer = new AccumulatorStream();

		reader.pipeTo(writer, h -> latch.countDown());

		reader.resume();
		try {
			latch.await();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		return writer.buffer().toString();
	}

	private static class AccumulatorStream implements WriteStream<Buffer> {

		private Buffer buffer = Buffer.buffer();

		@Override
		public AccumulatorStream exceptionHandler(Handler<Throwable> handler) {
			return this;
		}

		@Override
		public AccumulatorStream setWriteQueueMaxSize(int maxSize) {
			return this;
		}

		@Override
		public boolean writeQueueFull() {
			return false;
		}

		@Override
		public AccumulatorStream drainHandler(Handler<Void> handler) {
			return this;
		}

		@Override
		public Future<Void> write(Buffer data) {
			buffer.appendBuffer(data);
			return Future.succeededFuture();

		}

		public Buffer buffer() {
			return buffer;
		}

		@Override
		public void write(Buffer data, Handler<AsyncResult<Void>> handler) {
			write(data);
			handler.handle(Result.success());
		}

		@Override
		public Future<Void> end() {
			return Future.succeededFuture();
		}

		@Override
		public void end(Handler<AsyncResult<Void>> handler) {
			handler.handle(Result.success());
		}
	}

}

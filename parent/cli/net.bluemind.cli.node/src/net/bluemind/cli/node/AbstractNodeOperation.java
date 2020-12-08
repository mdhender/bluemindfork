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
package net.bluemind.cli.node;

import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.config.InstallationId;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import picocli.CommandLine.Option;

public abstract class AbstractNodeOperation implements ICmdLet, Runnable {

	@Override
	public final Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}

	protected CliContext ctx;

	@Option(names = "--tag", description = "select servers tagged X")
	public String tag;

	@Option(names = "--uid", description = "select server with given uid")
	public String uid;

	@Option(names = "--addr", description = "select server with given address")
	public String address;

	@Option(names = "--workers", description = "run with X workers")
	public int workers = 1;

	@Override
	public final void run() {
		IServer serversApi = ctx.adminApi().instance(IServer.class, InstallationId.getIdentifier());
		List<ItemValue<Server>> allServers = serversApi.allComplete();
		Stream<ItemValue<Server>> stream = allServers.stream();
		if (tag != null) {
			stream = stream.filter(iv -> iv.value.tags.contains(tag));
		}
		if (uid != null) {
			stream = stream.filter(iv -> uid.equals(iv.uid));
		}
		if (address != null) {
			stream = stream.filter(iv -> address.equals(iv.value.address()));
		}
		List<ItemValue<Server>> serversList = stream.collect(Collectors.toList());
		// create executor & completion service with workers thread
		ExecutorService pool = Executors.newFixedThreadPool(workers);
		CompletionService<Void> opsWatcher = new ExecutorCompletionService<>(pool);

		for (ItemValue<Server> srv : serversList) {
			opsWatcher.submit(() -> {
				synchronousServerOperation(serversApi, srv);
				return null;
			});
		}
		serversList.forEach(de -> {
			try {
				opsWatcher.take().get();
			} catch (Exception e) {
				ctx.error(e.getMessage());
			}
		});
	}

	protected abstract void synchronousServerOperation(IServer serversApi, ItemValue<Server> srv);

}

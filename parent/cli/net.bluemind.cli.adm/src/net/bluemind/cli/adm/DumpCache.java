/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.cli.adm;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.streams.ReadStream;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.system.api.ICacheMgmt;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "dump-cache", description = "Dump core caches to a json file")
public class DumpCache implements ICmdLet, Runnable {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("maintenance");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return DumpCache.class;
		}
	}

	@Parameters(paramLabel = "output.json", description = "path to write the output")
	public File indexPath;

	private CliContext ctx;

	@Override
	public void run() {
		ICacheMgmt mgmtApi = ctx.adminApi().instance(ICacheMgmt.class);
		ReadStream<Buffer> stream = VertxStream.read(mgmtApi.dumpContent());
		Vertx vertx = VertxPlatform.getVertx();
		AsyncFile output = vertx.fileSystem().openBlocking(indexPath.getAbsolutePath(),
				new OpenOptions().setCreate(true).setTruncateExisting(true).setWrite(true));
		try {
			stream.pipeTo(output).toCompletionStage().toCompletableFuture().get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (ExecutionException e) {
			ctx.error(e.getMessage(), e);
		}
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}

}

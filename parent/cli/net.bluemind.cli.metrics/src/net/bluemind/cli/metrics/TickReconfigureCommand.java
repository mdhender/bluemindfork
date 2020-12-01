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
package net.bluemind.cli.metrics;

import java.util.Arrays;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.Tasks;
import net.bluemind.config.InstallationId;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.metrics.alerts.api.ITickConfiguration;
import net.bluemind.network.topology.Topology;
import net.bluemind.server.api.CommandStatus;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.TagDescriptor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "reconfigure", description = "update the TICK configuration on all servers")
public class TickReconfigureCommand implements ICmdLet, Runnable {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("tick");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return TickReconfigureCommand.class;
		}

	}

	private static final Logger logger = LoggerFactory.getLogger(TickReconfigureCommand.class);

	private CliContext ctx;

	@Option(names = "--dry", description = "Dry-run (do nothing)")
	public boolean dry = false;

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}

	public void run() {
		if (dry) {
			logger.warn("Dry mode does nothing");
		} else {
			ITickConfiguration tickApi = ctx.adminApi().instance(ITickConfiguration.class);
			TaskRef ref = tickApi.reconfigure();
			Tasks.follow(ctx, ref, "Fail to update tick configuration");
			IServer srvApi = ctx.adminApi().instance(IServer.class, InstallationId.getIdentifier());

			reload(srvApi, TagDescriptor.bm_nginx);
			reload(srvApi, TagDescriptor.bm_nginx_edge);
		}
	}

	private void reload(IServer srvApi, TagDescriptor nginxTag) {
		Topology.getIfAvailable().flatMap(t -> t.anyIfPresent(nginxTag.getTag())).ifPresent(srv -> {
			ctx.info("Reloading nginx " + srv + "...");
			CommandStatus status = srvApi.submitAndWait(srv.uid, "service bm-nginx reload");
			if (!status.successful) {
				ctx.error(Arrays.toString(status.output.toArray()));
			}
		});
	}

}

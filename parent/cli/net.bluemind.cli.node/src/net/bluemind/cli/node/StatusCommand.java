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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.metrics.alerts.api.AlertInfo;
import net.bluemind.metrics.alerts.api.AlertLevel;
import net.bluemind.metrics.alerts.api.IMonitoring;
import net.bluemind.node.api.ExitList;
import net.bluemind.node.api.FileDescription;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.node.shared.ActiveExecQuery;
import net.bluemind.node.shared.ExecDescriptor;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import picocli.CommandLine.Command;

/**
 * This is the defaut command on node related stuff to ensure we don't run a
 * destructive op by default
 *
 */
@Command(name = "status", description = "Show node(s) availability")
public class StatusCommand extends AbstractNodeOperation {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("node");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return StatusCommand.class;
		}

	}

	@Override
	protected void globalStatus() {
		IMonitoring monApi = ctx.adminApi().instance(IMonitoring.class);
		List<AlertInfo> lastDays = monApi.getAlerts(3, false, Arrays.asList(AlertLevel.WARNING, AlertLevel.CRITICAL));
		if (lastDays.isEmpty()) {
			ctx.info("No alerts in the last 3 day(s)");
		} else {
			ctx.info("Alerts for 3 days (some might be resolved)");
			for (AlertInfo ai : lastDays) {
				switch (ai.level) {
				case CRITICAL:
					ctx.error("  * [" + ai.host + "] " + ai.product + " / " + ai.id + ": " + ai.message + " @ "
							+ new BmDateTimeWrapper(ai.time).toDate());
					break;
				case WARNING:
					ctx.warn("  * [" + ai.host + "] " + ai.id + ": " + ai.message + " @ "
							+ new BmDateTimeWrapper(ai.time).toDate());
					break;
				default:
					break;

				}
			}
		}

	}

	@Override
	protected void synchronousServerOperation(IServer serversApi, ItemValue<Server> srv) {
		try {
			byte[] content = serversApi.readFile(srv.uid, "/etc/bm/bm.ini");
			if (content != null && content.length > 0) {
				reportSuccess(srv);
			} else {
				reportFailure(srv);
			}
			List<IStatusProvider> providers = loadProviders();
			extendedChecks(srv, providers);
		} catch (ServerFault sf) {
			reportFailure(srv);
		}
	}

	private List<IStatusProvider> loadProviders() {
		RunnableExtensionLoader<IStatusProvider> rel = new RunnableExtensionLoader<>();
		return rel.loadExtensions("net.bluemind.cli.node", "status", "status", "provider");
	}

	private void extendedChecks(ItemValue<Server> srv, List<IStatusProvider> providers) {
		INodeClient nc = NodeActivator.get(srv.value.address());
		checkHprofs(nc);
		checkIfBackupIsRunning(nc);
		if (srv.value.tags.contains("mail/imap")) {
			checkStickyReplicationLogs(nc);
		}

		for (IStatusProvider sp : providers) {
			sp.report(ctx, srv, nc);
		}
	}

	private void checkStickyReplicationLogs(INodeClient nc) {
		ExitList logsSize = NCUtils.exec(nc, "du -s /var/lib/cyrus/sync/core", 10, TimeUnit.SECONDS);
		// 52 /var/lib/cyrus/sync/core
		Pattern sizeKB = Pattern.compile("^([0-9]+).*$");
		for (String l : logsSize) {
			Matcher m = sizeKB.matcher(l);
			if (m.find()) {
				int kb = Integer.parseInt(m.group(1));
				if (kb > 32) {
					ctx.warn("  * " + kb + "KB of replication logs are sitting in /var/lib/cyrus/sync/core/");
				}
			}
		}
	}

	private void checkIfBackupIsRunning(INodeClient nc) {
		List<ExecDescriptor> active = nc.getActiveExecutions(ActiveExecQuery.byGroup("dataprotect"));
		for (ExecDescriptor ed : active) {
			ctx.warn(" * Command in 'dataprotect' group is running {}", ed.command);
		}
	}

	private void checkHprofs(INodeClient nc) {
		List<FileDescription> hprofs = nc.listFiles("/var/log", "hprof");
		if (!hprofs.isEmpty()) {
			Pattern hprof = Pattern.compile("java_pid([0-9]+).hprof");
			ctx.warn("  * " + hprofs.size() + " hprofs in /var/log/");
			for (FileDescription fd : hprofs) {
				Matcher match = hprof.matcher(fd.getName());
				if (match.find()) {
					long pid = Long.parseLong(match.group(1));
					ExitList exitCode = NCUtils.exec(nc, "kill -0 " + pid, 1, TimeUnit.SECONDS);
					if (exitCode.getExitCode() == 0) {
						ctx.error("    * /var/log/" + fd.getName() + " exists AND pid " + pid + " is active.");
					}
				} else {
					ctx.info(fd.getName() + " does not match.");
				}
			}
		}
	}

	private void reportFailure(ItemValue<Server> srv) {
		ctx.info(ctx.ansi().a(buildResult(srv)).fgBrightRed().a("FAILED").reset().toString());
	}

	private void reportSuccess(ItemValue<Server> srv) {
		ctx.info(ctx.ansi().a(buildResult(srv)).fgBrightGreen().a("OK").reset().toString());
	}

	private String buildResult(ItemValue<Server> srv) {
		return "Server " + srv.value.address() + " (" + srv.uid + " " + srv.displayName
				+ srv.value.tags.stream().collect(Collectors.joining(", ", " [", "]")) + ") ";
	}

}

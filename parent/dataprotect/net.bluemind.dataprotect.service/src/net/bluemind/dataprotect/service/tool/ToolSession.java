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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */

package net.bluemind.dataprotect.service.tool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.dataprotect.api.PartGeneration;
import net.bluemind.dataprotect.service.BackupPath;
import net.bluemind.dataprotect.service.IDPContext;
import net.bluemind.dataprotect.service.IDPContext.IToolConfig;
import net.bluemind.dataprotect.service.IDPContext.IToolSession;
import net.bluemind.node.api.ExitList;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.node.api.ProcessHandler;
import net.bluemind.node.shared.ExecRequest;
import net.bluemind.node.shared.ExecRequest.Options;

public class ToolSession implements IToolSession {

	private static final Logger logger = LoggerFactory.getLogger(ToolSession.class);
	private final IToolConfig cfg;
	private final IDPContext ctx;

	/**
	 * @param ctx
	 * @param cfg
	 */
	public ToolSession(IDPContext ctx, IToolConfig cfg) {
		if (cfg == null) {
			throw new NullPointerException("toolConfig can't be null");
		}
		if (cfg.getSource() == null) {
			throw new NullPointerException("cfg.source can't be null");
		}
		this.cfg = cfg;
		this.ctx = ctx;
	}

	private StringBuilder appendDir(StringBuilder cmd) {
		cmd.append(BackupPath.get(cfg.getSource(), cfg.getTag()) + "/");
		return cmd;
	}

	private INodeClient nc() {
		return NodeActivator.get(cfg.getSource().value.address());
	}

	private static class BackCommand {
		public BackCommand(String cmd, String dir) {
			this.cmd = cmd;
			this.dir = dir;
		}

		public final String cmd;
		public final String dir;

	}

	public PartGeneration backup(PartGeneration previous, PartGeneration next) throws ServerFault {
		StringBuilder bd = new StringBuilder();
		appendDir(bd).append(next.id).append('/');
		String backupDir = bd.toString();
		INodeClient nc = nc();
		NCUtils.execNoOut(nc, "mkdir -p " + backupDir);
		NCUtils.execNoOut(nc, "chmod +x /usr/share/bm-node/rsync-backup.sh");

		List<BackCommand> toRun = cfg.getDirs().stream().map(dir -> makeBackupCommand(nc, previous, next, dir))
				.filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());

		int nbProcs = Runtime.getRuntime().availableProcessors() - 1;
		Semaphore sem = new Semaphore(Math.max(3, nbProcs));
		CompletableFuture<?>[] toJoin = toRun.stream().map(cmd -> runBackupCommand(nc, next, cmd, sem))
				.toArray(CompletableFuture[]::new);
		ctx.info("en", "Waiting for rsync completions...");
		CompletableFuture.allOf(toJoin).join();

		long size = 0;
		for (String dir : cfg.getDirs()) {
			size += computeSize(dir, nc);
		}
		next.size = size;

		return next;
	}

	private long computeSize(String backupDir, INodeClient nc) {
		logger.info("Detecting backup size of {}", backupDir);
		long size = 0L;
		ExitList output = NCUtils.exec(nc, "du -sBM " + backupDir);
		StringBuilder duOut = new StringBuilder();
		for (String out : output) {
			duOut.append(out);
		}
		String duOutput = duOut.toString();

		logger.info("Output of {}:{}", backupDir, duOutput);
		if (!duOutput.trim().isEmpty()) {
			int mCommeMa = duOutput.indexOf('M');
			if (mCommeMa != -1) {
				duOutput = duOutput.substring(0, mCommeMa);
				size = Long.parseLong(duOutput);
			}
		} else {
			logger.info("du -sBM returned no output for {}", backupDir);
		}
		return size;
	}

	public void interrupt() {
		Thread.currentThread().interrupt();
	}

	private CompletableFuture<Integer> runBackupCommand(INodeClient nc, PartGeneration next, BackCommand cmd,
			Semaphore sem) {
		ctx.info("en", "RSYNC: (permits " + sem.availablePermits() + ") " + cmd.cmd);
		CompletableFuture<Integer> ret = new CompletableFuture<>();

		try {
			sem.acquire();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			ret.completeExceptionally(e);
			return ret;
		}

		ProcessHandler handler = new ProcessHandler() {

			@Override
			public void log(String l) {
				if (!StringUtils.isBlank(l)) {
					ctx.info("en", "RSYNC: " + l);
				}
			}

			@Override
			public void completed(int exitCode) {
				sem.release();
				ret.complete(exitCode);
			}

			@Override
			public void starting(String taskRef) {
				ctx.info("en", "RSYNC: " + taskRef + " started.");
			}

		};
		nc.asyncExecute(ExecRequest.named("dataprotect", cmd.dir, cmd.cmd, Options.FAIL_IF_EXISTS), handler);

		return ret;
	}

	private Optional<BackCommand> makeBackupCommand(INodeClient nc, PartGeneration previous, PartGeneration next,
			String dir) {
		// check if file exists
		String command = String.format("/usr/bin/test -d %s", dir);
		if (NCUtils.exec(nc, command).getExitCode() != 0) {
			logger.warn("Skipping non-existing directory {}", dir);
			return Optional.empty();
		}

		StringBuilder cmd = new StringBuilder();
		cmd.append(
				"/usr/bin/rsync --exclude-from=/etc/bm-node/rsync.excludes -rltDH --delete --numeric-ids --relative --delete-excluded");
		if (previous != null) {
			cmd.append(" --link-dest=");
			appendDir(cmd).append(previous.id).append('/');
		}

		cmd.append(' ').append(dir.endsWith("/") ? dir : dir + "/").append(' ');

		appendDir(cmd).append(next.id).append('/');

		return Optional.of(new BackCommand(cmd.toString(), dir));
	}

	@Override
	public void restore(int generation, Set<String> what) throws ServerFault {
		restore(generation, what, "/");
	}

	@Override
	public void restore(int generation, Set<String> what, String to) throws ServerFault {
		doRestore(generation, what, to, "scripts/restore.sh");
	}

	@Override
	public void restoreOneFolder(int generation, String what, String to) {
		doRestore(generation, ImmutableSet.<String>builder().add(what).build(), to, "scripts/restoreOneFolder.sh");
	}

	private void doRestore(int generation, Set<String> what, String to, String script) throws ServerFault {
		INodeClient nc = nc();
		String restoreScriptName = System.nanoTime() + ".restore.sh";
		String restoreScript = "/var/backups/bluemind/temp/" + restoreScriptName;
		String restoreLogFile = "/tmp/" + restoreScriptName + ".log";

		InputStream in = ToolSession.class.getClassLoader().getResourceAsStream(script);
		nc.writeFile(restoreScript, in);
		NCUtils.execNoOut(nc, "chmod +x " + restoreScript);

		StringBuilder rdb = new StringBuilder();
		String r = appendDir(rdb).append(generation).append('/').toString();
		for (String src : what) {
			StringBuilder args = new StringBuilder(1024);
			args.append(restoreScript).append(' ');
			args.append(r);
			// remove starting slash
			args.append(' ').append(src.substring(1));
			args.append(' ').append(to);
			String theCmd = args.toString();
			ExitList output = NCUtils.exec(nc, theCmd);
			logger.info("Running {}", theCmd);
			int eCode = output.getExitCode();
			for (String s : output) {
				if (eCode == 0) {
					ctx.info("en", "RESTORE[" + generation + "]: " + s);
				} else {
					ctx.error("en", "RESTORE[" + generation + "]: " + s);
				}
			}

			try (BufferedReader br = new BufferedReader(new InputStreamReader(nc.openStream(restoreLogFile)))) {
				String line = null;

				while ((line = br.readLine()) != null) {
					ctx.info("en", line);
					ctx.info("fr", line);
				}
			} catch (IOException e) {
				ctx.error("en", String.format("Unable to get restore log file %s, from %s: %s", restoreLogFile,
						cfg.getSource().value.address(), e.getMessage()));
			}

			if (eCode > 0) {
				NCUtils.execNoOut(nc, "rm -f " + restoreScript);
				throw new ServerFault("Error on '" + theCmd + "'");
			}
		}

		NCUtils.execNoOut(nc, "rm -f " + restoreScript);
	}

	@Override
	public String tmpDirectory() throws ServerFault {
		String ret = "/var/backups/bluemind/temp/" + System.nanoTime();
		NCUtils.execNoOut(nc(), "mkdir -p " + ret);
		return ret;
	}

	@Override
	public void clean(List<Integer> validPartIds) {
		new RemoveForgottenParts(nc(), validPartIds).execute();
	}

}

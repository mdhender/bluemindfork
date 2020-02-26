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
package net.bluemind.node.server.busmod;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.node.shared.ExecRequest;
import net.bluemind.node.shared.ExecRequest.Options;

public class SysCommand extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(SysCommand.class);

	private static final Map<Long, RunningCommand> active = new ConcurrentHashMap<>();
	private static final Map<Long, RunningCommand> activeUnwatched = new ConcurrentHashMap<>();
	private static final AtomicLong pid = new AtomicLong();

	public void start() {
		EventBus eb = vertx.eventBus();
		Handler<Message<JsonObject>> crHandler = new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				newRequest(event);
			}
		};
		eb.consumer("cmd.request", crHandler);

		Handler<Message<JsonObject>> csHandler = new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				reqStatus(event);
			}
		};
		eb.consumer("cmd.status", csHandler);

		Handler<Message<JsonObject>> stopHandler = new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				interrupt(event);
			}

		};
		eb.consumer("cmd.interrupt", stopHandler);

		Handler<Message<JsonObject>> queryHandler = new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				executions(event);
			}
		};
		eb.consumer("cmd.executions", queryHandler);

		setupStaleWatcher();
	}

	/**
	 * The state watcher clears command the client forgot to check
	 */
	private void setupStaleWatcher() {
		vertx.setPeriodic(30000, new Handler<Long>() {

			@Override
			public void handle(Long event) {
				int count = 0;
				long cur = System.currentTimeMillis();
				for (Entry<Long, RunningCommand> entry : active.entrySet()) {
					RunningCommand rc = entry.getValue();
					if (rc != null) {
						long et = rc.getLastCheck();
						if (et > 0 && (cur - et > 30000)) {
							logger.warn("[{}] unchecked for 30sec, dropped.", rc.getPid());
							interrupt(entry.getKey());
							count++;
						}
					}
				}
				if (count > 0) {
					logger.warn("Stale commands: {}", count);
				}
			}
		});
	}

	private void interrupt(Message<JsonObject> event) {
		JsonObject interruptReq = event.body();
		logger.info("INTERRUPT {}", interruptReq.encodePrettily());
		long pid = interruptReq.getLong("pid", 0L);
		interrupt(pid);
		event.reply(new JsonObject());
	}

	private void interrupt(long pid) {
		RunningCommand rc = Optional.ofNullable(active.get(pid)).orElse(activeUnwatched.get(pid));
		if (rc != null) {
			Process ps = rc.getProcess();
			if (ps.isAlive()) {
				try {
					ps.destroyForcibly().waitFor();
					logger.info("Interrupted {}", rc);
				} catch (InterruptedException ie) {
					logger.info("interrupt got interrupted {}", ie.getMessage());
				}
			}
			active.remove(pid);
			activeUnwatched.remove(pid);
		}
	}

	private void executions(Message<JsonObject> event) {
		JsonObject req = event.body();
		logger.info("req: {}", req.encodePrettily());
		JsonObject js = new JsonObject();
		JsonArray execs = new JsonArray();
		js.put("descriptors", execs);
		String group = req.getString("group");
		String name = req.getString("name");
		Predicate<RunningCommand> match = matcher(group, name);

		active.values().stream().filter(match).forEach(cmd -> {
			JsonObject desc = new JsonObject();
			desc.put("group", cmd.group);
			desc.put("name", cmd.name);
			desc.put("command", cmd.cmd);
			desc.put("pid", Long.toString(cmd.getPid()));
			execs.add(desc);
		});
		event.reply(js);
	}

	private Predicate<RunningCommand> matcher(String group, String name) {
		Predicate<RunningCommand> match = null;
		if (group == null && name == null) {
			match = cmd -> true;
		} else if (group != null && name == null) {
			match = cmd -> group.equals(cmd.group);
		} else {
			match = cmd -> group.equals(cmd.group) && name.equals(cmd.name);
		}
		return match;
	}

	public static class WsEndpoint {

		public WsEndpoint(String wsWriteAddress, long wsRid) {
			this.writeAddress = wsWriteAddress;
			this.rid = wsRid;
		}

		String writeAddress;
		long rid;

		public WsEndpoint write(String kind, JsonObject js) {
			js.put("ws-rid", rid).put("kind", kind);
			VertxPlatform.eventBus().send(writeAddress, js.encode());
			return this;
		}

		public void complete(long pid) {
			activeUnwatched.remove(pid);
		}
	}

	private void newRequest(Message<JsonObject> event) {
		JsonObject jso = event.body();
		logger.info("run: {}", jso.encodePrettily());
		String cmd = jso.getString("command");
		JsonArray optionsJs = jso.getJsonArray("options");
		Set<Options> options = new HashSet<>();
		if (optionsJs != null) {
			for (int i = 0; i < optionsJs.size(); i++) {
				options.add(ExecRequest.Options.valueOf(optionsJs.getString(i)));
			}
		} else if (jso.containsKey("withOutput")) {
			// the old execute request format
			boolean recordOutput = jso.getBoolean("withOutput");
			if (!recordOutput) {
				options.add(Options.DISCARD_OUTPUT);
			}
		}
		String group = jso.getString("group", "not_grouped");
		String name = jso.getString("name");
		String wsWriteAddress = jso.getString("ws-target");
		long wsRid = jso.getLong("ws-rid", 0L);
		WsEndpoint wsEP = null;
		if (wsWriteAddress != null) {
			wsEP = new WsEndpoint(wsWriteAddress, wsRid);
		}
		RunningCommand rc = startCommand(group, name, cmd, options, wsEP);
		if (rc != null) {
			event.reply(rc.getPid());
			// do not setup staled command watcher
			if (wsEP == null) {
				active.put(rc.getPid(), rc);
			} else {
				activeUnwatched.put(rc.getPid(), rc);
			}
			logger.info("[{}][options: {}] cmd: {}", rc.getPid(), options, cmd);
		} else {
			logger.error("[FAILED] cmd: {}", cmd);
			event.reply(-1l);
		}
	}

	private void reqStatus(Message<JsonObject> event) {
		JsonObject jso = event.body();
		long pid = jso.getLong("pid");
		logger.debug("[{}] status check over {} active", pid, active.size());
		RunningCommand rc = active.get(pid);
		JsonObject rep = new JsonObject();
		if (rc == null) {
			rep.put("complete", true);
			rep.put("successful", true);
			logger.warn("Status on expired command {}", pid);
		} else {
			Integer ev = rc.getExitValue();
			boolean comp = ev != null;
			rep.put("complete", comp);
			if (comp) {
				logger.info("[{}] finished, exitCode: {}", pid, ev);
				rep.put("exitCode", ev);
				active.remove(pid);
				// that's what ncutils.waitFor expects
				rep.put("successful", true);
			} else {
				rep.put("successful", false);
			}
			rc.setLastCheck(System.currentTimeMillis());
			JsonArray out = rc.drainOutput();
			logger.debug("Drained {} output lines.", out.size());
			rep.put("output", out);
		}
		event.reply(rep);
	}

	private Stream<RunningCommand> activeCommands() {
		return Stream.concat(active.values().stream(), activeUnwatched.values().stream());
	}

	private RunningCommand startCommand(String group, String name, String cmd, Set<Options> options, WsEndpoint wsEP) {
		if (options.contains(Options.FAIL_IF_EXISTS)) {
			boolean hasMatch = activeCommands().anyMatch(matcher(group, name));
			if (hasMatch) {
				logger.info("Preventing execution of [{} {} '{}'] because of FAIL_IF_EXISTS", group, name, cmd);
				return null;
			}
		} else if (options.contains(Options.FAIL_IF_GROUP_EXISTS)) {
			boolean hasMatch = activeCommands().anyMatch(matcher(group, null));
			if (hasMatch) {
				logger.info("Preventing execution of [{} {} '{}'] because of FAIL_IF_GROUP_EXISTS", group, name, cmd);
				return null;
			}
		} else if (options.contains(Options.REPLACE_IF_EXISTS)) {
			List<RunningCommand> matching = activeCommands().filter(matcher(group, name)).collect(Collectors.toList());
			if (!matching.isEmpty()) {
				logger.info("REPLACE_IF_EXISTS {} {} will interrupt {} task(s)", group, name, matching.size());
				for (RunningCommand rc : matching) {
					interrupt(rc.getPid());
				}
			}
		}
		String[] cmdAndArgs = CmdParser.args(cmd);
		ProcessBuilder ps = new ProcessBuilder(cmdAndArgs);
		ps.redirectErrorStream(true);
		long procId = pid.incrementAndGet();
		try {
			RunningCommand rc = new RunningCommand(group, name == null ? "cmd_" + procId : name, cmd, procId);
			Process proc = ps.start();
			rc.setProcess(proc);
			if (wsEP != null) {
				wsEP.write("start", new JsonObject().put("task", procId));
			}
			boolean recordOutput = !options.contains(Options.DISCARD_OUTPUT);
			final StdoutPump pump = new StdoutPump(proc, rc, recordOutput, wsEP);
			Thread pt = new Thread(pump);
			pt.setDaemon(true);
			pt.setName(cmd + ":" + procId);
			pt.start();
			return rc;
		} catch (Exception t) {
			logger.error(t.getMessage());
			return null;
		}
	}
}

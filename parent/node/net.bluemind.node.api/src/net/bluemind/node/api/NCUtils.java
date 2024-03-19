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
package net.bluemind.node.api;

import java.io.File;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.util.concurrent.DefaultThreadFactory;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.node.api.ProcessHandler.BlockingHandler;
import net.bluemind.node.api.ProcessHandler.NoOutBlockingHandler;
import net.bluemind.node.shared.ExecDescriptor;
import net.bluemind.node.shared.ExecRequest;

/**
 * Helper methods for monitoring tasks running a remote node using
 * {@link INodeClient}
 * 
 */
public final class NCUtils {

	private static final Logger logger = LoggerFactory.getLogger(NCUtils.class);

	private static final Executor forgottenTasks = Executors.newFixedThreadPool(1,
			new DefaultThreadFactory("node-task-cleanup"));

	/**
	 * Waits for a tasks to complete, returning its output.
	 * 
	 * @param nc
	 * @param cmd
	 * @return the command output
	 * @throws ServerFault
	 */
	public static ExitList exec(INodeClient nc, List<String> argv) throws ServerFault {
		TaskRef taskRef = nc.executeCommand(argv);
		return waitFor(nc, taskRef, false);
	}

	public static ExitList exec(INodeClient nc, String... argv) throws ServerFault {
		return exec(nc, List.of(argv));
	}

	/**
	 * The command runs over a websocket
	 * 
	 * @param nc
	 * @param cmd
	 * @param delay
	 * @param unit
	 * @return
	 * @throws ServerFault
	 */
	public static ExitList exec(INodeClient nc, List<String> argv, long delay, TimeUnit unit) throws ServerFault {
		BlockingHandler handle = new ProcessHandler.BlockingHandler();
		nc.asyncExecute(ExecRequest.anonymous(argv), handle);
		return handle.get(delay, unit);
	}

	public static ExitList exec(INodeClient nc, long delay, TimeUnit unit, String... argv) throws ServerFault {
		return exec(nc, List.of(argv), delay, unit);
	}

	/**
	 * Waits for a tasks to complete, don't retrieve its output.
	 * 
	 * @param nc
	 * @param cmd
	 * @throws ServerFault
	 */
	public static void execNoOut(INodeClient nc, List<String> argv) throws ServerFault {
		TaskRef taskRef = nc.executeCommandNoOut(argv);
		waitFor(nc, taskRef, true);
	}

	public static void execNoOut(INodeClient nc, String... argv) throws ServerFault {
		execNoOut(nc, List.of(argv));
	}

	/**
	 * The commands runs over a websocket
	 * 
	 * @param nc
	 * @param cmd
	 * @param delay
	 * @param unit
	 * @throws ServerFault
	 */
	public static void execNoOut(INodeClient nc, List<String> argv, long delay, TimeUnit unit) throws ServerFault {
		NoOutBlockingHandler handle = new ProcessHandler.NoOutBlockingHandler();
		nc.asyncExecute(ExecRequest.anonymous(argv), handle);
		handle.get(delay, unit);
	}

	public static void execNoOut(INodeClient nc, long delay, TimeUnit unit, String... argv) throws ServerFault {
		execNoOut(nc, List.of(argv), delay, unit);
	}

	/**
	 * Waits for a tasks to complete, throws ServerFault if task fails
	 * 
	 * @param nc
	 * @param cmd
	 * @throws ServerFault
	 */
	public static void execOrFail(INodeClient nc, List<String> argv) throws ServerFault {
		TaskRef taskRef = nc.executeCommandNoOut(argv);
		ExitList ret = waitFor(nc, taskRef, true);
		if (ret.getExitCode() != 0) {
			throw new ServerFault("Fail to execute command '" + argv.stream().collect(Collectors.joining(" ")) + "'");
		}
	}

	public static void execOrFail(INodeClient nc, String... argv) throws ServerFault {
		execOrFail(nc, List.of(argv));
	}

	/**
	 * Spawns a tasks. Use it when you don't care about the task result or
	 * completion.
	 * 
	 * @param nc
	 * @param cmd
	 * @throws ServerFault
	 */
	public static void forget(final INodeClient nc, final List<String> argv) {
		forgottenTasks.execute(() -> {
			TaskRef taskRef;
			try {
				taskRef = nc.executeCommandNoOut(argv);
				waitFor(nc, taskRef, true);
			} catch (ServerFault e) {
				logger.error(e.getMessage(), e);
			}
		});
	}

	public static void forget(final INodeClient nc, final String... argv) {
		forget(nc, List.of(argv));
	}

	/**
	 * Waits for a tasks to complete and returns its output
	 * 
	 * @param nc
	 * @param ref
	 * @return
	 * @throws ServerFault
	 */
	public static ExitList waitFor(INodeClient nc, TaskRef ref) throws ServerFault {
		return waitFor(nc, ref, false);
	}

	/**
	 * When trashOut is true, output lines from {@link TaskStatus} is ignored and an
	 * empty list is returned.
	 * 
	 * @param nc
	 * @param copy
	 * @param trashOut
	 * @return
	 * @throws ServerFault
	 */
	private static ExitList waitFor(INodeClient nc, TaskRef ref, boolean trashOut) throws ServerFault {
		TaskRef copy = TaskRef.create(ref.id);
		TaskStatus ts = null;

		ExitList output = new ExitList();
		long count = 1;
		do {
			try {
				long nap = Math.min(1000, 10 * count++);
				Thread.sleep(nap);
			} catch (InterruptedException e) {
				logger.warn("Task has been interrupted, cancelling execution of {}", ref.id);
				nc.interrupt(ExecDescriptor.forTask(ref.id));
				Thread.currentThread().interrupt();
				throw new ServerFault("Task has been interrupted.");
			}
			ts = nc.getExecutionStatus(copy);

			if (!trashOut && ts.lastLogEntry != null && !ts.lastLogEntry.isEmpty()) {
				output.add(ts.lastLogEntry);
			}
		} while (!ts.state.ended);
		output.setExitCode(Integer.parseInt(ts.result));

		if (!ts.state.succeed) {
			for (String s : output) {
				logger.warn("FAILED TASK {}: {} ", copy.id, s);
			}
			throw new ServerFault("Task " + copy.id + " failed.");
		}

		return output;
	}

	public static boolean connectedToMyself(INodeClient nc) throws ServerFault {
		boolean ret = false;
		try {
			File f = File.createTempFile("nodeclient", ".myself");
			f.setLastModified(System.currentTimeMillis());
			List<FileDescription> list = nc.listFiles(f.getAbsolutePath());
			f.delete();
			if (list != null && list.size() == 1) {
				ret = true;
			}
		} catch (Exception e) {
		}
		return ret;
	}
}

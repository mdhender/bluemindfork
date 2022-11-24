/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.node.api;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.api.TaskStatus.State;
import net.bluemind.node.shared.ActiveExecQuery;
import net.bluemind.node.shared.ExecDescriptor;
import net.bluemind.node.shared.ExecRequest;

public class LocalNodeClient implements INodeClient {

	private static final Logger logger = LoggerFactory.getLogger(LocalNodeClient.class);

	@Override
	public void ping() throws ServerFault {
		logger.info("PING");
	}

	@Override
	public byte[] read(String path) throws ServerFault {
		Path asPath = Paths.get(path);

		try {
			if (Files.exists(asPath)) {
				return Files.readAllBytes(asPath);
			} else {
				return new byte[0];
			}
		} catch (IOException e) {
			throw new ServerFault(e);
		}
	}

	@Override
	public boolean exists(String path) {
		Path asPath = Paths.get(path);
		return Files.exists(asPath);
	}

	@Override
	public InputStream openStream(String path) throws ServerFault {
		try {
			return Files.newInputStream(Paths.get(path));
		} catch (IOException e) {
			throw new ServerFault(e);
		}
	}

	@Override
	public void writeFile(String path, InputStream content) throws ServerFault {
		Path asPath = Paths.get(path);
		try {
			Files.createDirectories(asPath.getParent());
		} catch (IOException e) {
			throw new ServerFault(e);
		}
		try (OutputStream out = Files.newOutputStream(asPath)) {
			ByteStreams.copy(content, out);
		} catch (IOException e) {
			throw new ServerFault(e);
		}
	}

	@Override
	public TaskRef executeCommand(String cmd) throws ServerFault {
		throw new ServerFault("unsupported op (" + cmd + ")");
	}

	@Override
	public TaskRef executeCommand(ExecRequest tsk) throws ServerFault {
		throw new ServerFault("unsupported op (" + tsk + ")");
	}

	@Override
	public void asyncExecute(ExecRequest req, ProcessHandler ph) {
		logger.warn("skip {}", req);
		ph.completed(0);
	}

	@Override
	public List<ExecDescriptor> getActiveExecutions(ActiveExecQuery query) {
		throw new ServerFault("unsupported op (" + query + ")");
	}

	@Override
	public void interrupt(ExecDescriptor runningTask) {
		throw new ServerFault("unsupported op (" + runningTask + ")");
	}

	private static final TaskRef THE_TASK = new TaskRef();

	@Override
	public TaskRef executeCommandNoOut(String cmd) throws ServerFault {
		logger.warn("skip '{}'", cmd);
		return THE_TASK;
	}

	@Override
	public TaskStatus getExecutionStatus(TaskRef task) throws ServerFault {
		TaskStatus ts = new TaskStatus();
		ts.state = State.Success;
		ts.result = "0";
		return ts;
	}

	@Override
	public List<FileDescription> listFiles(String path, String extensionPattern) throws ServerFault {
		File dir = new File(path);
		if (!dir.exists()) {
			logger.warn("{} does not exist", path);
			return Collections.emptyList();
		}
		return Arrays.stream(dir.listFiles(f -> f.getName().endsWith(extensionPattern)))
				.map(f -> new FileDescription(f.getAbsolutePath())).collect(Collectors.toList());
	}

	@Override
	public List<FileDescription> listFiles(String path) throws ServerFault {
		File dir = new File(path);
		if (!dir.exists()) {
			logger.warn("{} does not exist", path);
			return Collections.emptyList();
		}
		return Arrays.stream(dir.listFiles()).map(f -> new FileDescription(f.getAbsolutePath()))
				.collect(Collectors.toList());
	}

	@Override
	public void deleteFile(String path) throws ServerFault {
		Path asPath = Paths.get(path);
		try {
			Files.deleteIfExists(asPath);
		} catch (IOException e) {
			throw new ServerFault(e);
		}
	}

	@Override
	public void moveFile(String origin, String destination) {
		try {
			Files.move(Paths.get(origin), Paths.get(destination));
		} catch (IOException e) {
			throw new ServerFault(e);
		}
	}

}

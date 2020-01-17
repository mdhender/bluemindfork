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
package net.bluemind.node.client.impl.ahc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.asynchttpclient.BoundRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;

import net.bluemind.common.io.FileBackedOutputStream;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.node.api.FileDescription;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.ProcessHandler;
import net.bluemind.node.client.impl.DoesNotExist;
import net.bluemind.node.client.impl.FBOSInput;
import net.bluemind.node.client.impl.HostPortClient;
import net.bluemind.node.client.impl.PingFailed;
import net.bluemind.node.shared.ActiveExecQuery;
import net.bluemind.node.shared.ExecDescriptor;
import net.bluemind.node.shared.ExecRequest;

public final class AHCHttpNodeClient implements INodeClient {

	private static final Logger logger = LoggerFactory.getLogger(AHCHttpNodeClient.class);

	private final HostPortClient cli;
	private final Escaper escaper;

	public AHCHttpNodeClient(HostPortClient cli) {
		this.cli = cli;
		this.escaper = UrlEscapers.urlFragmentEscaper();
	}

	@Override
	public void ping() throws ServerFault {
		String p = cli.path().append("/ping").toString();
		BoundRequestBuilder req = cli.getClient().prepareOptions(p);
		try {
			run(req, new PingHandler());
		} catch (PingFailed pf) {
			throw new ServerFault("Ping failed");
		} catch (Exception t) {
			logger.error(t.getMessage(), t);
			throw new ServerFault("Unknown ping error");
		}
	}

	private final String esc(String s) {
		return escaper.escape(s);
	}

	@Override
	public byte[] read(String path) throws ServerFault {
		ReadHandler rh = new ReadHandler();
		String p = cli.path().append("/fs").append(esc(path)).toString();
		BoundRequestBuilder req = cli.getClient().prepareGet(p);
		FileBackedOutputStream fbos = null;
		try {
			fbos = run(req, rh);
			byte[] ret = fbos.asByteSource().read();
			return ret;
		} catch (DoesNotExist dne) {
			return new byte[0];
		} catch (Exception t) {
			throw new ServerFault(t);
		} finally {
			if (fbos != null) {
				try {
					fbos.reset();
				} catch (IOException e) {
				}
			}
		}
	}

	@Override
	public InputStream openStream(String path) throws ServerFault {
		ReadHandler rh = new ReadHandler();
		String p = cli.path().append("/fs").append(esc(path)).toString();
		BoundRequestBuilder req = cli.getClient().prepareGet(p);
		FileBackedOutputStream fbos = null;
		try {
			fbos = run(req, rh);
			return FBOSInput.from(fbos);
		} catch (DoesNotExist dne) {
			return new ByteArrayInputStream(new byte[0]);
		} catch (Exception t) {
			throw new ServerFault(t);
		}
	}

	@Override
	public void writeFile(String path, InputStream content) throws ServerFault {
		String p = cli.path().append("/fs").append(esc(path)).toString();
		BoundRequestBuilder req = cli.getClient().preparePut(p);
		try {
			run(req, new WriteHandler(path, content));
		} catch (Exception t) {
			throw new ServerFault(t);
		}
	}

	@Override
	public void deleteFile(String path) throws ServerFault {
		String p = cli.path().append("/fs").append(esc(path)).toString();
		BoundRequestBuilder req = cli.getClient().prepareDelete(p);
		try {
			run(req, new DeleteHandler(path));
		} catch (Exception t) {
			throw new ServerFault(t);
		}
	}

	@Override
	public TaskRef executeCommand(String cmd) throws ServerFault {
		return executeCommand(ExecRequest.anonymous(cmd));
	}

	@Override
	public TaskRef executeCommand(ExecRequest tsk) throws ServerFault {
		String p = cli.path().append("/cmd").toString();
		BoundRequestBuilder req = cli.getClient().preparePost(p);
		try {
			return run(req, new SubmitHandler(tsk));
		} catch (Exception t) {
			throw new ServerFault(t.getMessage());
		}
	}

	@Override
	public TaskRef executeCommandNoOut(String cmd) throws ServerFault {
		return executeCommand(ExecRequest.anonymousWithoutOutput(cmd));
	}

	@Override
	public TaskStatus getExecutionStatus(TaskRef task) throws ServerFault {
		String pid = task.id;
		String p = cli.path().append("/cmd/").append(pid).toString();
		BoundRequestBuilder req = cli.getClient().prepareGet(p);
		try {
			return run(req, new StatusHandler(pid));
		} catch (Exception t) {
			throw new ServerFault(t.getMessage());
		}
	}

	@Override
	public List<FileDescription> listFiles(String path, String extensionPattern) throws ServerFault {
		String p = cli.path().append("/match/").append(extensionPattern).append(esc(path)).toString();
		BoundRequestBuilder req = cli.getClient().prepareGet(p);
		try {
			return run(req, new ListFilesHandler());
		} catch (DoesNotExist dne) {
			return ImmutableList.of();
		}
	}

	@Override
	public List<FileDescription> listFiles(String path) throws ServerFault {
		String p = cli.path().append("/list").append(esc(path)).toString();
		BoundRequestBuilder req = cli.getClient().prepareGet(p);
		try {
			return run(req, new ListFilesHandler());
		} catch (DoesNotExist dne) {
			return ImmutableList.of();
		}
	}

	private <T> T run(BoundRequestBuilder req, DefaultAsyncHandler<T> handler) {
		try {
			return handler.prepare(req).execute(handler).get();
		} catch (ExecutionException e) {
			throw Throwables.propagate(e.getCause());
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
	}

	@Override
	public List<ExecDescriptor> getActiveExecutions(ActiveExecQuery query) {
		String p = cli.path().append("/cmd").toString();
		BoundRequestBuilder req = cli.getClient().prepareGet(p);
		try {
			return run(req, new ActiveExecutionsHandler(query));
		} catch (Exception t) {
			throw new ServerFault(t);
		}
	}

	@Override
	public void interrupt(ExecDescriptor runningTask) {
		String p = cli.path().append("/cmd/").append(runningTask.taskRefId).toString();
		BoundRequestBuilder req = cli.getClient().prepareDelete(p);
		try {
			run(req, new InterruptHandler(runningTask));
		} catch (Exception t) {
			throw new ServerFault(t);
		}
	}

	@Override
	public void asyncExecute(ExecRequest req, ProcessHandler ph) {
		cli.getWebsocketLink().startWsAction(JsonHelper.toJson(req), ph);

	}

}

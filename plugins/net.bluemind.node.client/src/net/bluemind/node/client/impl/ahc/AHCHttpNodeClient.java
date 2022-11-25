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
import org.asynchttpclient.Request;
import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.uri.Uri;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.escape.Escaper;

import net.bluemind.common.io.FileBackedOutputStream;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.node.api.FileDescription;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.ProcessHandler;
import net.bluemind.node.client.DoesNotExist;
import net.bluemind.node.client.NodePathEscaper;
import net.bluemind.node.client.impl.FBOSInput;
import net.bluemind.node.client.impl.HostPortClient;
import net.bluemind.node.client.impl.NodeRuntimeException;
import net.bluemind.node.client.impl.PingFailed;
import net.bluemind.node.shared.ActiveExecQuery;
import net.bluemind.node.shared.ExecDescriptor;
import net.bluemind.node.shared.ExecRequest;

public final class AHCHttpNodeClient implements INodeClient {

	private static final Logger logger = LoggerFactory.getLogger(AHCHttpNodeClient.class);

	private final HostPortClient cli;
	private final Escaper escaper;

	private final Uri baseUri;

	public AHCHttpNodeClient(HostPortClient cli) {
		this.cli = cli;
		this.baseUri = Uri.create(cli.path().toString());
		this.escaper = new NodePathEscaper();
	}

	private Uri withPath(String p) {
		return new Uri(baseUri.getScheme(), null, baseUri.getHost(), baseUri.getPort(), p, null, null);
	}

	@Override
	public void ping() {
		Request built = new RequestBuilder("OPTIONS", true, false).setUri(withPath("/ping")).build();
		BoundRequestBuilder req = cli.getClient().prepareRequest(built);
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
	public byte[] read(String path) {
		ReadHandler rh = new ReadHandler();
		Request built = new RequestBuilder("GET", true, false).setUri(withPath("/fs" + esc(path))).build();
		BoundRequestBuilder req = cli.getClient().prepareRequest(built);

		FileBackedOutputStream fbos = null;
		try {
			fbos = run(req, rh);
			return fbos.asByteSource().read();
		} catch (DoesNotExist dne) {
			return new byte[0];
		} catch (Exception t) {
			throw new ServerFault(t);
		} finally {
			if (fbos != null) {
				try {
					fbos.reset();
				} catch (IOException e) {
					// bingo
				}
			}
		}
	}

	@Override
	public InputStream openStream(String path) {
		ReadHandler rh = new ReadHandler();
		Request built = new RequestBuilder("GET", true, false).setUri(withPath("/fs" + esc(path))).build();
		BoundRequestBuilder req = cli.getClient().prepareRequest(built);
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
	public boolean exists(String path) {
		HeadHandler rh = new HeadHandler();
		Request built = new RequestBuilder("HEAD", true, false).setUri(withPath("/fs" + esc(path))).build();
		BoundRequestBuilder req = cli.getClient().prepareRequest(built);
		Boolean exist = false;
		try {
			exist = run(req, rh);
			return exist.booleanValue();
		} catch (Exception t) {
			throw new ServerFault(t);
		}
	}

	@Override
	public void writeFile(String path, InputStream content) {
		Request built = new RequestBuilder("PUT", true, false).setUri(withPath("/fs" + esc(path))).build();
		BoundRequestBuilder req = cli.getClient().prepareRequest(built);
		try {
			run(req, new WriteHandler(path, content));
		} catch (Exception t) {
			throw new ServerFault(t);
		}
	}

	@Override
	public void deleteFile(String path) {
		Request built = new RequestBuilder("DELETE", true).setUri(withPath("/fs" + esc(path))).build();
		BoundRequestBuilder req = cli.getClient().prepareRequest(built);
		try {
			run(req, new DeleteHandler(path));
		} catch (Exception t) {
			throw new ServerFault(t);
		}
	}

	@Override
	public TaskRef executeCommand(String cmd) {
		return executeCommand(ExecRequest.anonymous(cmd));
	}

	@Override
	public TaskRef executeCommand(ExecRequest tsk) {
		Request built = new RequestBuilder("POST", true, false).setUri(withPath("/cmd")).build();
		BoundRequestBuilder req = cli.getClient().prepareRequest(built);
		try {
			return run(req, new SubmitHandler(tsk));
		} catch (Exception t) {
			throw new ServerFault(t.getMessage());
		}
	}

	@Override
	public TaskRef executeCommandNoOut(String cmd) {
		return executeCommand(ExecRequest.anonymousWithoutOutput(cmd));
	}

	@Override
	public TaskStatus getExecutionStatus(TaskRef task) {
		String pid = task.id;
		Request built = new RequestBuilder("GET", true, false).setUri(withPath("/cmd/" + pid)).build();
		BoundRequestBuilder req = cli.getClient().prepareRequest(built);
		try {
			return run(req, new StatusHandler(pid));
		} catch (Exception t) {
			throw new ServerFault(t.getMessage());
		}
	}

	@Override
	public List<FileDescription> listFiles(String path, String extensionPattern) {
		Request built = new RequestBuilder("GET", true, false)
				.setUri(withPath("/match/" + extensionPattern + esc(path))).build();
		BoundRequestBuilder req = cli.getClient().prepareRequest(built);
		try {
			return run(req, new ListFilesHandler());
		} catch (DoesNotExist dne) {
			return ImmutableList.of();
		}
	}

	@Override
	public List<FileDescription> listFiles(String path) {
		Request built = new RequestBuilder("GET", true, false).setUri(withPath("/list" + esc(path))).build();
		BoundRequestBuilder req = cli.getClient().prepareRequest(built);
		try {
			return run(req, new ListFilesHandler());
		} catch (DoesNotExist dne) {
			return ImmutableList.of();
		}
	}

	@Override
	public void moveFile(String origin, String destination) throws ServerFault {
		Request built = new RequestBuilder("POST", true, false).setUri(withPath("/move")).build();
		BoundRequestBuilder req = cli.getClient().prepareRequest(built);
		try {
			run(req, new MoveHandler(origin, destination));
		} catch (Exception t) {
			throw new ServerFault(t.getMessage());
		}
	}

	@Override
	public void mkdirs(String dst, String permissions, String owner, String group) throws ServerFault {
		Request built = new RequestBuilder("POST", true, false).setUri(withPath("/mkdirs")).build();
		BoundRequestBuilder req = cli.getClient().prepareRequest(built);
		try {
			run(req, new MkdirsHandler(dst, permissions, owner, group));
		} catch (Exception t) {
			throw new ServerFault(t.getMessage());
		}
	}

	private <T> T run(BoundRequestBuilder req, DefaultAsyncHandler<T> handler) {
		try {
			return handler.prepare(req).execute(handler).get();
		} catch (ExecutionException e) {
			throw NodeRuntimeException.wrap(e.getCause());
		} catch (Exception e) {
			throw NodeRuntimeException.wrap(e);
		}
	}

	@Override
	public List<ExecDescriptor> getActiveExecutions(ActiveExecQuery query) {
		Request built = new RequestBuilder("GET", true, false).setUri(withPath("/cmd")).build();
		BoundRequestBuilder req = cli.getClient().prepareRequest(built);
		try {
			return run(req, new ActiveExecutionsHandler(query));
		} catch (Exception t) {
			throw new ServerFault(t);
		}
	}

	@Override
	public void interrupt(ExecDescriptor runningTask) {
		Request built = new RequestBuilder("DELETE", true, false).setUri(withPath("/cmd/" + runningTask.taskRefId))
				.build();
		BoundRequestBuilder req = cli.getClient().prepareRequest(built);
		try {
			run(req, new InterruptHandler(runningTask));
		} catch (Exception t) {
			throw new ServerFault(t);
		}
	}

	@Override
	public void asyncExecute(ExecRequest req, ProcessHandler ph) {
		cli.getWebsocketLink().startWsAction(req, ph);

	}
}

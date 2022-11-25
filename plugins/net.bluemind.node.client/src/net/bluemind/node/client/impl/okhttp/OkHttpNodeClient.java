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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.node.client.impl.okhttp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.io.ByteStreams;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.common.io.FileBackedOutputStream;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.node.api.FileDescription;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.ProcessHandler;
import net.bluemind.node.client.NodePathEscaper;
import net.bluemind.node.client.impl.FBOSInput;
import net.bluemind.node.client.impl.ahc.JsonHelper;
import net.bluemind.node.client.impl.ahc.MkdirsHandler.MkdirsJsonHelper;
import net.bluemind.node.client.impl.ahc.MoveHandler.MoveJsonHelper;
import net.bluemind.node.shared.ActiveExecQuery;
import net.bluemind.node.shared.ExecDescriptor;
import net.bluemind.node.shared.ExecRequest;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.BufferedSink;
import okio.Okio;

public class OkHttpNodeClient implements INodeClient {

	private static final Logger logger = LoggerFactory.getLogger(OkHttpNodeClient.class);
	private final OkHttpClient client;
	private final int port;
	private final String baseUrl;
	private final NodePathEscaper escaper;

	private WebSocket websocket;
	private final Map<Long, ProcessHandler> execHandlers;
	private final CountDownLatch startReceivedLatch;
	private boolean closed;
	private static final AtomicLong wsIdGen = new AtomicLong();
	private static final byte[] NOT_EXISTING = new byte[0];

	private static final ScheduledExecutorService sched = Executors.newScheduledThreadPool(1);

	public OkHttpNodeClient(OkHttpClient client, int port, String baseUrl, String wsUrl) {
		this.client = client;
		this.port = port;
		this.baseUrl = baseUrl;
		this.escaper = new NodePathEscaper();
		this.execHandlers = new ConcurrentHashMap<>();
		this.startReceivedLatch = new CountDownLatch(1);
		connectWebsocket(this, wsUrl);
	}

	private void connectWebsocket(OkHttpNodeClient self, String wsUrl) {
		Request upgrade = new Request.Builder().url(wsUrl).get().build();
		websocket = client.newWebSocket(upgrade, new WebSocketListener() {

			@Override
			public void onClosed(WebSocket webSocket, int code, String reason) {
				self.websocket = null;
				logger.info("ws {} closed: {} - {}", websocket, code, reason);
				if (!closed) {
					sched.schedule(() -> connectWebsocket(self, wsUrl), 1, TimeUnit.SECONDS);
				}
			}

			@Override
			public void onFailure(WebSocket webSocket, Throwable t, Response response) {
				self.websocket = null;
				logger.error("ws {} failed: {}", webSocket, response, t);
				if (!closed) {
					sched.schedule(() -> connectWebsocket(self, wsUrl), 1, TimeUnit.SECONDS);
				}
			}

			@Override
			public void onMessage(WebSocket webSocket, String text) {
				self.onMessage(text);
			}

			@Override
			public void onOpen(WebSocket webSocket, Response response) {
				logger.info("ws {} opened.", webSocket);
			}
		});
	}

	private void onMessage(String message) {
		logger.debug("onMessage: {}", message);
		JsonObject msg = new JsonObject(message);
		long rid = msg.getLong("ws-rid", 0L);
		ProcessHandler ph = execHandlers.get(rid);
		if (ph != null) {
			handleWebSocketFrame(rid, ph, msg);
		} else {
			String kind = msg.getString("kind");
			switch (kind) {
			case "node-start":
				List<Long> removedHandlers = new ArrayList<>();
				execHandlers.forEach((runId, handler) -> {
					handler.log("Node has restarted.", false);
					handler.completed(1);
					removedHandlers.add(runId);
				});
				for (Long runId : removedHandlers) {
					execHandlers.remove(runId);
				}
				logger.info("Node has restarted on {}, dropping {} task handlers.", baseUrl, removedHandlers.size());
				this.startReceivedLatch.countDown();
				break;
			case "notification":
				// does not exist yet
				break;
			default:
				logger.warn("Unknown frame kind {}", kind);
			}
		}
	}

	private void handleWebSocketFrame(long rid, ProcessHandler ph, JsonObject payload) {
		if (logger.isDebugEnabled()) {
			logger.debug("WS - S: {}", payload.encodePrettily());
		}
		String kind = payload.getString("kind");
		switch (kind) {
		case "start":
			ph.starting(payload.getLong("task", 0L).toString());
			break;
		case "log":
			ph.log(payload.getString("log"), payload.getBoolean("continued", Boolean.FALSE));
			break;
		case "completion":
			ph.completed(payload.getInteger("exit", 0));
			execHandlers.remove(rid);
			break;
		default:
			logger.warn("Unknown frame kind {}", kind);
			break;
		}

	}

	public void startWsAction(ExecRequest wsReq, ProcessHandler ph) {
		try {
			startReceivedLatch.await();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		if (websocket == null) {
			logger.error("Error command as websocket is missing");
			ph.completed(1);
		} else {
			long rid = wsIdGen.incrementAndGet();
			execHandlers.put(rid, ph);
			websocket.send(JsonHelper.toJsonString(wsReq, rid));
		}

	}

	private String withPath(String s) {
		return baseUrl + s;
	}

	public boolean isSsl() {
		return port == 8022;
	}

	public void close() {
		this.closed = true;
		if (websocket != null) {
			websocket.close(1000, "kekbye");
		}
	}

	@Override
	public void ping() throws ServerFault {
		Request ping = new Request.Builder().url(withPath("/ping")).method("OPTIONS", null).build();
		try (Response pingResp = client.newCall(ping).execute()) {
			if (pingResp.code() != 200) {
				// node server sends 201 when switching to secure mode, NodeHook expects this
				// exception
				throw new ServerFault("ping failed");
			}
		} catch (IOException e) {
			throw new ServerFault(e);
		}
	}

	@Override
	public byte[] read(String path) throws ServerFault {
		Request readInMem = new Request.Builder().url(withPath("/fs") + escaper.escape(path)).get().build();
		try (Response readResp = client.newCall(readInMem).execute()) {
			if (readResp.code() == 404) {
				return NOT_EXISTING;
			} else {
				return readResp.body().bytes();
			}
		} catch (IOException e) {
			throw new ServerFault(e);
		}
	}

	@Override
	public boolean exists(String path) {
		Request readInMem = new Request.Builder().url(withPath("/fs") + escaper.escape(path)).head().build();
		try (Response readResp = client.newCall(readInMem).execute()) {
			return readResp.isSuccessful();
		} catch (IOException e) {
			throw new ServerFault(e);
		}
	}

	@Override
	public InputStream openStream(String path) throws ServerFault {
		Request readInMem = new Request.Builder().url(withPath("/fs") + escaper.escape(path)).get().build();
		try (Response readResp = client.newCall(readInMem).execute()) {
			if (readResp.code() == 404) {
				return new ByteArrayInputStream(NOT_EXISTING);
			} else {
				try (InputStream netIn = readResp.body().byteStream();
						FileBackedOutputStream fbos = new FileBackedOutputStream(64536, "node-openStream")) {
					ByteStreams.copy(netIn, fbos);
					return FBOSInput.from(fbos);
				}
			}
		} catch (IOException e) {
			throw new ServerFault(e);
		}
	}

	private static final MediaType MEDIA_TYPE_BINARY = MediaType.parse("application/octet-stream");
	private static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json");

	@Override
	public void writeFile(String path, InputStream content) throws ServerFault {
		RequestBody streamBody = new RequestBody() {

			@Override
			public void writeTo(BufferedSink sink) throws IOException {
				sink.writeAll(Okio.source(content));
			}

			@Override
			public MediaType contentType() {
				return MEDIA_TYPE_BINARY;
			}
		};
		Request postStream = new Request.Builder().url(withPath("/fs") + escaper.escape(path)).put(streamBody).build();
		try (Response writeResp = client.newCall(postStream).execute()) {
			if (!writeResp.isSuccessful()) {
				logger.error("Write to {} failed: {}", path, writeResp.code());
			}
		} catch (IOException e) {
			throw new ServerFault(e);
		}
	}

	@Override
	public TaskRef executeCommand(String cmd) throws ServerFault {
		return executeCommand(ExecRequest.anonymous(cmd));
	}

	@Override
	public TaskRef executeCommand(ExecRequest tsk) throws ServerFault {
		Request exec = new Request.Builder().url(withPath("/cmd"))
				.post(RequestBody.create(JsonHelper.toJsonString(tsk, null), MEDIA_TYPE_JSON)).build();
		try (Response exResp = client.newCall(exec).execute()) {
			if (exResp.code() == 201) {
				String pid = exResp.header("Pid");
				return TaskRef.create(pid);
			}
			throw new ServerFault("Submit error for " + tsk.command + ": " + exResp.code());
		} catch (IOException e) {
			throw new ServerFault(e);
		}
	}

	@Override
	public void asyncExecute(ExecRequest req, ProcessHandler ph) {
		startWsAction(req, ph);
	}

	@Override
	public List<ExecDescriptor> getActiveExecutions(ActiveExecQuery query) {
		okhttp3.HttpUrl.Builder urlBuilder = HttpUrl.parse(withPath("/cmd")).newBuilder();
		if (query.group != null) {
			urlBuilder.addQueryParameter("group", query.group);
			if (query.name != null) {
				urlBuilder.addQueryParameter("name", query.name);
			}
		}
		Builder builder = new Request.Builder().url(urlBuilder.build()).get();

		try (Response lr = client.newCall(builder.build()).execute()) {
			byte[] bytes = lr.body().bytes();
			JsonObject jso = new JsonObject(new String(bytes));
			if (logger.isInfoEnabled()) {
				logger.info("Executions {}", jso.encodePrettily());
			}
			JsonArray descs = jso.getJsonArray("descriptors");
			int len = descs.size();
			List<ExecDescriptor> ret = new ArrayList<>(len);
			for (int i = 0; i < len; i++) {
				JsonObject descJs = descs.getJsonObject(i);
				ExecDescriptor desc = new ExecDescriptor();
				desc.command = descJs.getString("command");
				desc.group = descJs.getString("group");
				desc.name = descJs.getString("name");
				desc.taskRefId = descJs.getString("pid");
				ret.add(desc);
			}
			return ret;
		} catch (IOException e) {
			throw new ServerFault(e);
		}
	}

	@Override
	public void interrupt(ExecDescriptor runningTask) {
		Request delReq = new Request.Builder().url(withPath("/cmd/" + runningTask.taskRefId)).delete().build();
		try (Response delResp = client.newCall(delReq).execute()) {
			if (!delResp.isSuccessful()) {
				logger.warn("Interrupt of {} failed (code {})", runningTask.taskRefId, delResp.code());
			}
		} catch (IOException e) {
			throw new ServerFault(e);
		}

	}

	@Override
	public TaskRef executeCommandNoOut(String cmd) throws ServerFault {
		return executeCommand(ExecRequest.anonymousWithoutOutput(cmd));
	}

	private static final JsonArray EMPTY_ARRAY = JsonArray.of();

	@Override
	public TaskStatus getExecutionStatus(TaskRef task) throws ServerFault {
		Request list = new Request.Builder().url(withPath("/cmd/" + task.id)).get().build();
		try (Response lr = client.newCall(list).execute()) {
			byte[] bytes = lr.body().bytes();
			JsonObject jso = new JsonObject(new String(bytes));
			boolean complete = jso.getBoolean("complete");
			boolean successfull = jso.getBoolean("successful");
			int exitCode = jso.getInteger("exitCode", 1);
			JsonArray output = jso.getJsonArray("output", EMPTY_ARRAY);
			String lastLogEntry = Joiner.on('\n').join(output);
			TaskStatus.State state = fromBooleans(complete, successfull);
			return TaskStatus.create(10, state.ended ? 10 : 1, lastLogEntry, state, "" + exitCode);
		} catch (IOException e) {
			throw new ServerFault(e);
		}
	}

	private TaskStatus.State fromBooleans(boolean complete, boolean successfull) {
		for (TaskStatus.State st : TaskStatus.State.values()) {
			if (st.ended == complete && st.succeed == successfull) {
				return st;
			}
		}
		return TaskStatus.State.InError;
	}

	@Override
	public List<FileDescription> listFiles(String path, String extensionPattern) throws ServerFault {
		Request list = new Request.Builder().url(withPath("/match/" + extensionPattern + escaper.escape(path))).get()
				.build();
		try (Response lr = client.newCall(list).execute()) {
			return parseResp(lr);
		} catch (IOException e) {
			throw new ServerFault(e);
		}
	}

	@Override
	public List<FileDescription> listFiles(String path) throws ServerFault {
		Request list = new Request.Builder().url(withPath("/list" + escaper.escape(path))).get().build();
		try (Response lr = client.newCall(list).execute()) {
			return parseResp(lr);
		} catch (IOException e) {
			throw new ServerFault(e);
		}
	}

	List<FileDescription> parseResp(Response lr) throws IOException {
		if (lr.code() == 404) {
			return Collections.emptyList();
		}
		byte[] body = lr.body().bytes();
		JsonObject jso = new JsonObject(new String(body));
		JsonArray descs = jso.getJsonArray("descriptions");
		int len = descs.size();
		List<FileDescription> ret = new ArrayList<>(len);
		for (int i = 0; i < len; i++) {
			JsonObject fdo = descs.getJsonObject(i);
			FileDescription desc = new FileDescription(fdo.getString("path"));
			boolean isDir = fdo.getBoolean("dir");
			desc.setDirectory(isDir);
			if (!isDir) {
				desc.setSize(fdo.getLong("size"));
			}
			ret.add(desc);
		}
		return ret;
	}

	@Override
	public void deleteFile(String path) throws ServerFault {
		Request delReq = new Request.Builder().url(withPath("/fs") + escaper.escape(path)).delete().build();
		try (Response delResp = client.newCall(delReq).execute()) {
			if (!delResp.isSuccessful()) {
				logger.warn("Delete of {} failed (code {})", path, delResp.code());
			}
		} catch (IOException e) {
			throw new ServerFault(e);
		}

	}

	@Override
	public void moveFile(String origin, String destination) throws ServerFault {
		Request delReq = new Request.Builder().url(withPath("/move"))
				.post(RequestBody.create(MoveJsonHelper.toJsonString(origin, destination), MEDIA_TYPE_JSON)).build();
		try (Response delResp = client.newCall(delReq).execute()) {
			if (!delResp.isSuccessful()) {
				logger.warn("mv {} {} failed (code {})", origin, destination, delResp.code());
			}
		} catch (IOException e) {
			throw new ServerFault(e);
		}
	}

	@Override
	public void mkdirs(String dst, String permissions, String owner, String group) throws ServerFault {
		Request delReq = new Request.Builder()
				.url(withPath("/mkdirs")).post(RequestBody
						.create(MkdirsJsonHelper.toJsonString(dst, permissions, owner, group), MEDIA_TYPE_JSON))
				.build();
		try (Response delResp = client.newCall(delReq).execute()) {
			if (!delResp.isSuccessful()) {
				logger.warn("mkdirs {} failed (code {})", dst, delResp.code());
			}
		} catch (IOException e) {
			throw new ServerFault(e);
		}
	}
}

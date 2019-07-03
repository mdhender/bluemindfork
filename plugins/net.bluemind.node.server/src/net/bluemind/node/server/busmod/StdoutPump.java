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

import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.parsetools.RecordParser;

import net.bluemind.node.server.busmod.SysCommand.WsEndpoint;

public final class StdoutPump implements Runnable {

	private static final byte[] LF = "\n".getBytes();
	private static final Logger logger = LoggerFactory.getLogger(StdoutPump.class);

	private final InputStream in;
	private final RunningCommand rc;
	private final RecordParser rp;
	private final Process proc;
	private final boolean record;
	private WsEndpoint wsEndpoint;

	public StdoutPump(Process proc, RunningCommand rc, boolean recordOutput, WsEndpoint wsEP) {
		this.proc = proc;
		this.in = proc.getInputStream();
		this.rc = rc;
		this.record = recordOutput;
		this.rp = RecordParser.newDelimited(LF, new Handler<Buffer>() {

			@Override
			public void handle(Buffer event) {
				newLine(event);
			}
		});
		this.wsEndpoint = wsEP;
	}

	private void newLine(Buffer b) {
		String line = b.toString();
		logger.debug("[{}]: {}", rc.getPid(), line);
		rc.out(line);
		if (wsEndpoint != null) {
			JsonObject log = new JsonObject().putString("log", line);
			wsEndpoint.write("log", log);
		}
	}

	private final byte[] chunk(byte[] buf, int len) {
		if (len == buf.length) {
			return buf;
		}
		byte[] result = new byte[len];
		System.arraycopy(buf, 0, result, 0, len);
		return result;
	}

	public void run() {
		long time = System.currentTimeMillis();
		long count = 0;
		try {
			Integer exit = null;
			byte[] buf = new byte[32768];
			while (true) {
				int read = in.read(buf);
				if (read == -1) {
					break;
				}
				if (record && read > 0) {
					Buffer chunk = new Buffer(chunk(buf, read));
					rp.handle(chunk);
				}
				logger.debug("[{}] pumped {}bytes.", rc.getPid(), read);
				count++;
			}
			logger.debug("Exited stream pump after {}loops.", count);
			try {
				exit = proc.waitFor();
				if (wsEndpoint != null) {
					wsEndpoint.write("completion", new JsonObject().putNumber("exit", exit));
					wsEndpoint.complete(rc.getPid());
				}
				rc.setExitValue(exit, time);
				logger.info("[{}] exit: {} (loops: {})", rc.getPid(), exit, count);
				proc.destroy();
			} catch (InterruptedException itse) {
				logger.error(itse.getMessage(), itse);
				// thrown by exitValue() when not finished
			}
		} catch (Exception e) {
			rp.handle(new Buffer(e.getMessage()));
			logger.error("[{}] {}", rc.getPid(), e.getMessage());
			rc.setExitValue(1, time);
		}
	}
}

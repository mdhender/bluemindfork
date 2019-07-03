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

import java.util.ArrayDeque;
import java.util.ArrayList;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public final class RunningCommand {

	private final long pid;
	public final String group;
	public final String name;
	public final String cmd;
	private ArrayDeque<String> output;
	private Integer exitValue;
	private long lastCheck;
	private Process process;

	public RunningCommand(String group, String name, String cmd, long pid) {
		this.pid = pid;
		this.group = group;
		this.name = name;
		this.cmd = cmd;
		output = new ArrayDeque<>();
	}

	public void out(String s) {
		output.add(s);
	}

	public JsonObject asJson() {
		JsonObject jso = new JsonObject();
		jso.putNumber("pid", pid);
		return jso;
	}

	public long getPid() {
		return pid;
	}

	public Integer getExitValue() {
		return exitValue;
	}

	public void setExitValue(Integer exitValue, long l) {
		this.exitValue = exitValue;
		this.lastCheck = l;
	}

	public long getLastCheck() {
		return lastCheck;
	}

	public JsonArray drainOutput() {
		int len = output.size();
		ArrayList<Object> al = new ArrayList<>(len);
		for (int i = 0; i < len; i++) {
			al.add(output.poll());
		}
		JsonArray ja = new JsonArray(al);
		return ja;
	}

	public void setLastCheck(long lastCheck) {
		this.lastCheck = lastCheck;
	}

	public void setProcess(Process proc) {
		this.process = proc;
	}

	public Process getProcess() {
		return process;
	}

}

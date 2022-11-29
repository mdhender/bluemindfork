/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.node.shared;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import com.google.common.base.MoreObjects;

public class ExecRequest {

	public enum Options {

		/**
		 * Process output will be dropped by node server
		 */
		DISCARD_OUTPUT,

		/**
		 * Refuse the execution if a process from the same group is already running
		 */
		FAIL_IF_GROUP_EXISTS,

		/**
		 * Refuse the execution if a process with the same group/name exists
		 */
		FAIL_IF_EXISTS,

		/**
		 * Replace the process if one with the same group/name exists
		 */
		REPLACE_IF_EXISTS;

	}

	public final String command;
	public final String group;
	public final String name;
	public final Set<Options> options;

	private ExecRequest(String group, String name, String command, EnumSet<Options> opt) {
		this.group = group;
		this.name = name;
		this.command = command;
		this.options = opt;
	}

	public static ExecRequest anonymousWithoutOutput(String cmd) {
		return new ExecRequest(null, null, cmd, EnumSet.of(Options.DISCARD_OUTPUT));
	}

	public static ExecRequest anonymous(String cmd) {
		return new ExecRequest(null, null, cmd, EnumSet.noneOf(Options.class));
	}

	public static ExecRequest named(String group, String name, String cmd, Options... options) {
		return new ExecRequest(group, name, cmd,
				options.length == 0 ? EnumSet.noneOf(Options.class) : EnumSet.copyOf(Arrays.asList(options)));
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(ExecRequest.class).add("g", group).add("n", name).add("cmd", command)
				.toString();
	}
}

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
import java.util.List;
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

	public final List<String> argv;
	public final String group;
	public final String name;
	public final Set<Options> options;

	private ExecRequest(String group, String name, List<String> argv, EnumSet<Options> opt) {
		this.group = group;
		this.name = name;
		this.argv = argv;
		this.options = opt;
	}

	public static ExecRequest anonymousWithoutOutput(List<String> argv) {
		return new ExecRequest(null, null, argv, EnumSet.of(Options.DISCARD_OUTPUT));
	}

	public static ExecRequest anonymous(List<String> argv) {
		return new ExecRequest(null, null, argv, EnumSet.noneOf(Options.class));
	}

	public static ExecRequest named(String group, String name, List<String> argv, Options... options) {
		return new ExecRequest(group, name, argv,
				options.length == 0 ? EnumSet.noneOf(Options.class) : EnumSet.copyOf(Arrays.asList(options)));
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(ExecRequest.class).add("g", group).add("n", name).add("cmd", argv).toString();
	}
}

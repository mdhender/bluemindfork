/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
 *
 * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License)
 * or the CeCILL as published by CeCILL.info (version 2 of the License).
 *
 * There are special exceptions to the terms and conditions of the
 * licenses as they are applied to this program. See LICENSE.txt in
 * the directory of this program distribution.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.cli.cmd.api;

import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class CliException extends RuntimeException {

	public CliException() {
		super();
	}

	public CliException(String message) {
		super(message);
		LoggerFactory.getLogger(
				StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass().getTypeName())
				.error(message);
	}

	public CliException(String message, Throwable t) {
		super(message, t);
		LoggerFactory.getLogger(
				StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass().getTypeName())
				.error(message + ":{}", t.getMessage());
	}

	public CliException(Throwable e) {
		super(e);
		LoggerFactory.getLogger(
				StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass().getTypeName())
				.error(e.getMessage());
	}
}

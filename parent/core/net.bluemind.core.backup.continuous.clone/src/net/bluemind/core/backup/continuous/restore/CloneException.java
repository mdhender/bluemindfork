/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.core.backup.continuous.restore;

@SuppressWarnings("serial")
public class CloneException extends RuntimeException {

	public CloneException(Throwable t) {
		super(t);
	}

	public CloneException(String string) {
		super(string);
	}

	public CloneException(String string, Throwable t) {
		super(string, t);
	}

	public static RuntimeException propagate(Exception e) {
		if (e instanceof RuntimeException) {
			throw (RuntimeException) e;
		} else {
			throw new CloneException(e);
		}
	}

}

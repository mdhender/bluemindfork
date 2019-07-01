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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.backend.mail.api.utils;

import net.bluemind.backend.mail.api.MessageBody.Part;

public class PartsWalker<T> {

	@FunctionalInterface
	public static interface PartVisitor<T> {
		void accept(T context, Part cur);
	}

	private final T context;

	public PartsWalker(T context) {
		this.context = context;
	}

	public void visit(PartVisitor<T> visitor, Part root) {
		visitor.accept(context, root);
		for (Part child : root.children) {
			visit(visitor, child);
		}

	}

}

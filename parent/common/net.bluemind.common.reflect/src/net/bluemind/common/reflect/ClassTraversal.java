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
package net.bluemind.common.reflect;

import java.lang.reflect.Method;

public class ClassTraversal<T> {

	private final Class<T> clazz;
	private final ClassVisitor visitor;

	public ClassTraversal(Class<T> clazz, ClassVisitor visitor) {
		this.clazz = clazz;
		this.visitor = visitor;
	}

	public void traverse() {
		visitor.visit(clazz);
		for (Method method : clazz.getMethods()) {
			visitor.visit(method);
		}
	}

	public int getMethodCount() {
		return clazz.getMethods().length;
	}

}

/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.system.service.internal;

import java.util.List;

import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.system.schemaupgrader.PostInst;

public class PostInstTasks {

	private PostInstTasks() {

	}

	public static List<PostInst> postInstJavaUpdaters() {
		RunnableExtensionLoader<PostInst> epLoader = new RunnableExtensionLoader<>();
		return epLoader.loadExtensionsWithPriority("net.bluemind.system.schemaupgrader", "postinst", "java", "code");
	}

}

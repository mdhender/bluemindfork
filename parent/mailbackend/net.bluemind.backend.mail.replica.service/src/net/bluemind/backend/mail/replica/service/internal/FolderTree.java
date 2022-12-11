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
package net.bluemind.backend.mail.replica.service.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.core.container.model.ItemValue;

public class FolderTree {

	private static class Node {
		public final String uid;
		public final ItemValue<MailboxFolder> folder;
		public final List<Node> children;

		public Node(ItemValue<MailboxFolder> f) {
			this.uid = f.uid;
			this.folder = f;
			this.children = new ArrayList<>();
		}

	}

	private Map<String, Node> nodes = new HashMap<>();

	public static FolderTree of(List<ItemValue<MailboxFolder>> folders) {
		FolderTree ft = new FolderTree();
		folders.stream().map(Node::new).forEach(n -> ft.nodes.put(n.uid, n));
		ft.nodes.values().forEach(n -> {
			if (n.folder.value.parentUid != null) {
				Node parentNode = ft.nodes.get(n.folder.value.parentUid);
				parentNode.children.add(n);
			}
		});
		return ft;
	}

	public List<ItemValue<MailboxFolder>> children(ItemValue<MailboxFolder> parent) {
		Node n = nodes.get(parent.uid);
		List<ItemValue<MailboxFolder>> ret = new ArrayList<>();
		children(ret, n);
		return ret;
	}

	private List<ItemValue<MailboxFolder>> children(List<ItemValue<MailboxFolder>> build, Node n) {
		n.children.forEach(child -> {
			build.add(child.folder);
			children(build, child);
		});
		return build;
	}

}

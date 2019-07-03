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
package net.bluemind.core.rest.vertx;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import net.bluemind.core.rest.base.RestRootHandler;
import net.bluemind.core.rest.base.RestRootHandler.TreePathLeaf;
import net.bluemind.core.rest.base.RestRootHandler.TreePathNode;

public class TreePathNodeTest {

	@Test
	public void testInsertAndSearch() {
		RestRootHandler.TreePathNode node = new TreePathNode();
		TreePathLeaf leaf1 = new TreePathLeaf(null);
		TreePathLeaf leaf2 = new TreePathLeaf(null);
		TreePathLeaf leaf3 = new TreePathLeaf(null);
		node.insert("/test/test2/test3/test4", leaf1);
		node.insert("/test2/{param1}/test2/{param2}", leaf2);
		node.insert("/test2/toto/test2/{param2}", leaf3);

		TreePathLeaf leaf = node.leaf("/test/test2/test3/test4");
		assertEquals(leaf1, leaf);

		leaf = node.leaf("/test2/toto/test2/titi");
		assertEquals(leaf3, leaf);
		leaf = node.leaf("/test2/toto/test2/tutu");
		assertEquals(leaf3, leaf);

		leaf = node.leaf("/test2/toti/test2/tutu");
		assertEquals(leaf2, leaf);

		leaf = node.leaf("/test2/ /test2/tutu");
		assertEquals(leaf2, leaf);
	}
}

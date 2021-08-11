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
package net.bluemind.core.container.api.internal;

import net.bluemind.core.container.api.ContainerHierarchyNode;
import net.bluemind.core.container.api.IContainersFlatHierarchy;
import net.bluemind.core.container.model.Item;

public interface IInternalContainersFlatHierarchy extends IContainersFlatHierarchy {

	void create(String uid, ContainerHierarchyNode node);

	void createWithId(long id, String uid, ContainerHierarchyNode node);

	void createItem(Item it, ContainerHierarchyNode node);

	void update(String uid, ContainerHierarchyNode node);

	void delete(String uid);

	void reset();

}

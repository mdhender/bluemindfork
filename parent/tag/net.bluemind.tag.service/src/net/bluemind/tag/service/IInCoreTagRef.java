/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.tag.service;

import java.util.List;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Item;
import net.bluemind.tag.api.TagRef;

public interface IInCoreTagRef {

	public void create(Item item, List<TagRef> value) throws ServerFault;

	public void update(Item item, List<TagRef> value) throws ServerFault;

	public void delete(Item item) throws ServerFault;

	public List<TagRef> get(Item item) throws ServerFault;

	public void deleteAll() throws ServerFault;

	public List<List<TagRef>> getMultiple(List<Item> items) throws ServerFault;

}
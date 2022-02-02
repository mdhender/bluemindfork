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

import net.bluemind.core.container.api.ContainerSubscriptionModel;
import net.bluemind.core.container.api.IOwnerSubscriptions;
import net.bluemind.core.container.api.IRestoreCrudSupport;

public interface IInternalOwnerSubscriptions extends IOwnerSubscriptions, IRestoreCrudSupport<ContainerSubscriptionModel> {

	void create(String uid, ContainerSubscriptionModel node);

	void createWithId(long id, String uid, ContainerSubscriptionModel node);

	void update(String uid, ContainerSubscriptionModel node);

	void delete(String uid);

	void reset();

}

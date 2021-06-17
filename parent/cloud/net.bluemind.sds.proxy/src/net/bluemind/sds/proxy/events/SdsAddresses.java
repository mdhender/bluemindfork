/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.sds.proxy.events;

public class SdsAddresses {

	private SdsAddresses() {
	}

	public static final String VALIDATION = "core.api.mailbox.validation";

	public static final String SIZE_VALIDATION = "core.api.msg_size.validation";

	public static final String MAP = "mapping.ctrl.map";

	public static final String UNMAP = "mapping.ctrl.unmap";

	public static final String QUERY = "mapping.ctrl.query";

}

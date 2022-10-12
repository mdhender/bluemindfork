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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.delivery.lmtp.common;

public interface IDeliveryHook {

	/**
	 * Enhance the mailbox record with missing data. The mailbox record can be
	 * altered.
	 *
	 *
	 * @param ctx
	 * @param targetFolder the target folder
	 * @param toDeliver    the mailbox record to enhance
	 * @param msg          the current message body
	 * @return an enhance mailbox record. Might be the same as the given one. Return
	 *         null if you want to leave the mailbox record untouched.
	 */
	DeliveryContent preDelivery(IDeliveryContext ctx, DeliveryContent content);

}

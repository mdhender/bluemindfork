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
package net.bluemind.backend.mail.api.flags;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import net.bluemind.core.api.BMApi;

/**
 * Update one flag on multiple {@link MailboxItem}
 */
@BMApi(version = "3")
public class FlagUpdate {

	/**
	 * {@link MailboxItem} item identifiers 
	 */
	public List<Long> itemsId;
	
	/**
	 * {@link MailboxItemFlag} to update 
	 */
	public MailboxItemFlag mailboxItemFlag;
	
	@Override
	public String toString() {
		List<String> items = itemsId.stream().map(Object::toString).collect(Collectors.toList());
		return "FlagUpdate : { flag: " + mailboxItemFlag + ", items: " + String.join(", ", items) + " }";
	}
	
	/**
	 * 
	 * @param itemsId {@link MailboxItem} item identifiers 
	 * @param flag {@link MailboxItemFlag} to update
	 * @return
	 */
	public static FlagUpdate of(List<Long> itemsId, MailboxItemFlag flag) {
		FlagUpdate flagUpdate = new FlagUpdate();
		flagUpdate.itemsId = itemsId;
		flagUpdate.mailboxItemFlag = flag;
		return flagUpdate;
	}
	
	/**
	 * 
	 * @param itemId {@link MailboxItem} item identifier 
	 * @param flag {@link MailboxItemFlag} to update
	 * @return
	 */
	public static FlagUpdate of(Long itemId, MailboxItemFlag flag) {
		return of(Arrays.asList(itemId), flag);
	}

}

/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.backend.cyrus.partitions;

import java.util.UUID;

import com.google.common.hash.Hashing;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.mailbox.api.Mailbox;

public class CyrusUniqueIds {

	private CyrusUniqueIds() {

	}

	/**
	 * Compute the UNIQUEID internal cyrus identifier we want to enforce on default
	 * folders
	 * 
	 * @param domainUid
	 * @param mbox
	 * @param folderName
	 * @return
	 */
	public static UUID forMailbox(String domainUid, ItemValue<Mailbox> mbox, String folderName) {
		String toDerive;
		if (!mbox.value.type.sharedNs) {
			toDerive = "user." + mbox.uid + "@" + domainUid + "#" + folderName;
		} else {
			toDerive = mbox.uid + "@" + domainUid + (folderName.isEmpty() ? "" : "#" + folderName);
		}
		byte[] hashBytes = Hashing.murmur3_128().hashBytes(toDerive.getBytes()).asBytes();
		ByteBuf twoLongs = Unpooled.wrappedBuffer(hashBytes);
		return new UUID(twoLongs.readLong(), twoLongs.readLong());
	}

}

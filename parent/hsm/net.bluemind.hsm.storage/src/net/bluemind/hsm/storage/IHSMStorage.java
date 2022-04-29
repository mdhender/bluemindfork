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
package net.bluemind.hsm.storage;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

import net.bluemind.node.api.INodeClient;

public interface IHSMStorage extends Closeable {

	/**
	 * Store a mime stream for the given mailbox and returns the HSM id that can be
	 * used to retrieved the stored stream.
	 * 
	 * @param mailContent
	 * @return an HSM id
	 * @throws IOException
	 */
	String store(String domainUid, String mailboxUid, InputStream mailContent) throws IOException;

	/**
	 * Retrieve a store mime stream for a mailbox with the given HSM id. The stream
	 * in the store is kept as-is for later re-use.
	 * 
	 * @param hsmId
	 * @param maxMessageSize
	 * @return
	 * @throws IOException
	 */
	InputStream peek(String domainUid, String mailboxUid, String hsmId, Integer maxMessageSize) throws IOException;

	/**
	 * Delete a stored mime stream with the given HSM.
	 * 
	 * @param box
	 * @param hsmId
	 * @throws IOException
	 */
	void delete(String domainUid, String mailboxUid, String hsmId) throws IOException;

	/**
	 * @param domainUid
	 * @param sourceMailboxUid
	 * @param destMailboxUid
	 * @param hsmId
	 * @throws IOException
	 */
	void copy(String domainUid, String sourceMailboxUid, String destMailboxUid, String hsmId) throws IOException;

	void open(INodeClient nc);

	public int getPriority();

}

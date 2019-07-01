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

package net.bluemind.imap;

/**
 * Sync parameters used for QRESYNC requests
 *
 */
public final class SyncData {

	private final long uidvalidity;
	private final long modseq;
	private final long lastSeenUid;
	private final long firstSeeenUid;

	/**
	 * Creates a SyncData object suitable for the initial sync of a folder
	 * 
	 * @param uidvalidity
	 */
	public SyncData(long uidvalidity) {
		this(uidvalidity, 1L, 0, 0);
	}

	/**
	 * @param uidvalidity
	 *            the folder uidvalidity, from
	 *            {@link StoreClient#getUidValidity(String)}
	 * @param modseq
	 *            use 1L for initial sync
	 * @param lastSeenUid
	 *            last known uid. This is used to differentiate updates and
	 *            creates. use 0 for initial sync
	 * @param firstSeenUid
	 *            first known uid. This is used to differentiate filter deleted
	 *            items. use 0 for initial sync
	 * 
	 */
	public SyncData(long uidvalidity, long modseq, long lastSeenUid, long firstSeenUid) {
		this.uidvalidity = uidvalidity;
		this.modseq = modseq;
		this.lastSeenUid = lastSeenUid;
		this.firstSeeenUid = firstSeenUid;
	}

	public long getUidvalidity() {
		return uidvalidity;
	}

	public long getModseq() {
		return modseq;
	}

	public long getLastseenUid() {
		return lastSeenUid;
	}

	public long getFirstSeenUid() {
		return firstSeeenUid;
	}

}

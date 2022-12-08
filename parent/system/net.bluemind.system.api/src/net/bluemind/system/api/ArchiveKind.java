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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.system.api;

public enum ArchiveKind {
	// amazon s3
	S3("s3", true),

	// scality
	ScalityRing("scalityring", true),

	// cyrus archive
	Cyrus("cyrus", true, true, true),

	// store on local filesystem
	Dummy("dummy"),

	// no store at all
	Noop("noop");

	private final String name;
	private final boolean isSdsArchive;
	private final boolean supportsHsm;
	private final boolean shardedByDatalocation;

	private ArchiveKind(String name, boolean isSdsArchive, boolean supportsHsm, boolean shardedByDatalocation) {
		this.name = name;
		this.isSdsArchive = isSdsArchive;
		this.supportsHsm = supportsHsm;
		this.shardedByDatalocation = shardedByDatalocation;
	}

	private ArchiveKind(String name, boolean isSdsArchive) {
		this(name, isSdsArchive, false, false);
	}

	private ArchiveKind(String name) {
		this(name, false, false, false);
	}

	@Override
	public String toString() {
		return this.name;
	}

	public boolean isSdsArchive() {
		return this.isSdsArchive;
	}

	public boolean supportsHsm() {
		return this.supportsHsm;
	}

	/**
	 * When true, the backing store has a separated dataset on each datalocation. S3
	 * and Scality hold the data for every location whereas the Cyrus store uses
	 * bm-node to split its data depending on datalocations.
	 * 
	 * @return
	 */
	public boolean isShardedByDatalocation() {
		return this.shardedByDatalocation;
	}

	public static ArchiveKind fromName(String name) {
		if (name != null) {
			String lowerName = name.toLowerCase();
			for (ArchiveKind kind : ArchiveKind.values()) {
				if (lowerName.equals(kind.name)) {
					return kind;
				}
			}
		}
		return null;
	}

}
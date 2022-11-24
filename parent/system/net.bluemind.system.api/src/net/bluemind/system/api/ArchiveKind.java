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
	Cyrus("cyrus", true, true),

	// store on local filesystem
	Dummy("dummy"),

	// no store at all
	Noop("noop"),

	// for some sds-proxy junits
	Test("test");

	private final String name;
	private final boolean isSdsArchive;
	private final boolean supportsHsm;

	private ArchiveKind(String name, boolean isSdsArchive, boolean supportsHsm) {
		this.name = name;
		this.isSdsArchive = isSdsArchive;
		this.supportsHsm = supportsHsm;
	}

	private ArchiveKind(String name, boolean isSdsArchive) {
		this(name, isSdsArchive, false);
	}

	private ArchiveKind(String name) {
		this(name, false, false);
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
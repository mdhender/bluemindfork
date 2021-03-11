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
package net.bluemind.backend.cyrus.index;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class CyrusHeader {
	private static final Logger logger = LoggerFactory.getLogger(CyrusHeader.class);
	private static final byte[] HL1MAGIC = { (byte) 0xa1, (byte) 0x02, (byte) 0x8b, (byte) 0x0d };
	private static final String HL1 = "Cyrus mailbox header\n";
	private static final String HL2 = "\"The best thing about this system was that it had lots of goals.\"\n";
	private static final String HL3 = "\t--Jim Morris on Andrew\n";

	private enum ReadState {
		QuotaRoot, UniqueId, Flags, AclUser, AclData
	}

	public static class CyrusAcl {
		public final String username;
		public final String acls;

		public CyrusAcl(String username, String acls) {
			this.username = username;
			this.acls = acls;
		}

		public String toString() {
			return String.format("<CyrusAcl %s: %s>", username, acls);
		}

		public byte[] getBytes() {
			ByteBuf buf = Unpooled.buffer(username.length() + acls.length() + 2);
			buf.writeBytes(username.getBytes()).writeByte('\t').writeBytes(acls.getBytes()).writeByte('\t');
			return buf.array();
		}
	}

	// 489daff7.internal!user.laurent
	private String quotaRoot;
	// 8b1b53b4-51cb-439b-8088-118735fa2248
	private String uniqueId;

	private String flags = "";
	// admin0 lrswipkxtecdan
	// cli-created-ff74e83c-1fad-49a9-8fca-89e767d2bdd7@489daff7.internal
	// lrswipkxtecdan
	private List<CyrusAcl> acls = new ArrayList<>();

	public CyrusHeader() {
		// ok
	}

	public String quotaRoot() {
		return quotaRoot;
	}

	public String flags() {
		return flags;
	}

	public String uniqueId() {
		return uniqueId;
	}

	public List<CyrusAcl> acls() {
		return acls;
	}

	public CyrusHeader quotaRoot(String quotaRoot) {
		// Sets the mailbox name of the quota holder
		this.quotaRoot = quotaRoot;
		return this;
	}

	public CyrusHeader uniqueId(String uuid) {
		if (uuid == null || uuid.isEmpty()) {
			this.uniqueId = UUID.randomUUID().toString();
		} else {
			this.uniqueId = uuid;
		}
		return this;
	}

	public CyrusHeader flags(String flags) {
		this.flags = flags != null ? flags : "";
		return this;
	}

	public CyrusHeader acls(CyrusAcl... acls) {
		for (CyrusAcl acl : acls) {
			this.acls.add(acl);
		}
		return this;
	}

	public String toString() {
		return String.format("<CyrusHeader quotaRoot: %s uniqueId: %s flags: %s acls: %s>", quotaRoot, uniqueId,
				flags.trim(), acls);
	}

	public static CyrusHeader from(ByteBuf buf) {
		CyrusHeader hdr = new CyrusHeader();
		byte[] magic = new byte[4];
		byte[] hl1 = new byte[HL1.length()];
		byte[] hl2 = new byte[HL2.length()];
		byte[] hl3 = new byte[HL3.length()];
		buf.readBytes(magic);
		buf.readBytes(hl1);
		buf.readBytes(hl2);
		buf.readBytes(hl3);
		if (!Arrays.equals(magic, HL1MAGIC)) {
			logger.error("HL1 magic does not match: {} {}", magic, HL1MAGIC);
		}
		if (!Arrays.equals(hl1, HL1.getBytes())) {
			logger.error("HL1 does not match");
		}
		if (!Arrays.equals(hl2, HL2.getBytes())) {
			logger.error("HL2 does not match");
		}
		if (!Arrays.equals(hl3, HL3.getBytes())) {
			logger.error("HL3 does not match");
		}
		ReadState state = ReadState.QuotaRoot;
		StringBuilder sb = new StringBuilder();
		String acldata = "";
		String acluser = "";

		while (buf.readableBytes() > 0) {
			char c = (char) buf.readByte();
			switch (state) {
			case QuotaRoot:
				if (c == '\t' || c == '\n') { // handle broken headers
					state = ReadState.UniqueId;
					hdr.quotaRoot = sb.toString();
					sb = new StringBuilder();
				} else {
					sb.append(c);
				}
				break;
			case UniqueId:
				if (c == '\n') {
					state = ReadState.Flags;
					hdr.uniqueId = sb.toString();
					sb = new StringBuilder();
				} else {
					sb.append(c);
				}
				break;
			case Flags:
				if (c == '\n') {
					state = ReadState.AclUser;
					hdr.flags = sb.toString();
					sb = new StringBuilder();
				} else {
					sb.append(c);
				}
				break;
			case AclData:
				if (c == '\t' || c == '\n') {
					acldata = sb.toString();
					if (!acluser.isEmpty() && !acldata.isEmpty()) {
						hdr.acls.add(new CyrusAcl(acluser, acldata));
					}
					sb = new StringBuilder();
					state = ReadState.AclUser;
				} else {
					sb.append(c);
				}
				break;
			case AclUser:
				if (c == '\t') {
					acluser = sb.toString();
					sb = new StringBuilder();
					state = ReadState.AclData;
				} else {
					sb.append(c);
				}
				break;
			default:
				break;
			}
		}
		return hdr;
	}

	public CyrusHeader to(ByteBuf buf) {
		buf.writeBytes(HL1MAGIC);
		buf.writeBytes(HL1.getBytes());
		buf.writeBytes(HL2.getBytes());
		buf.writeBytes(HL3.getBytes());
		buf.writeBytes(quotaRoot.getBytes()).writeByte('\t');
		buf.writeBytes(uniqueId.getBytes()).writeByte('\n');
		buf.writeBytes(flags.getBytes()).writeByte('\n');
		for (CyrusAcl acl : acls) {
			buf.writeBytes(acl.getBytes());
		}
		buf.writeByte('\n');
		return this;
	}
}

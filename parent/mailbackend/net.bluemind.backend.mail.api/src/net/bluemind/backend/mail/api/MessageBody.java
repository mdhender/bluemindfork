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
package net.bluemind.backend.mail.api;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class MessageBody {

	@BMApi(version = "3")
	public static class Header {
		public String name;
		public List<String> values;

		public static Header create(String name, String... values) {
			Header h = new Header();
			h.name = name;
			h.values = Arrays.asList(values);
			return h;
		}

		public static Header create(String name, Iterable<String> values) {
			Header h = new Header();
			h.name = name;
			h.values = new LinkedList<>();
			values.forEach(h.values::add);
			return h;
		}

		public String firstValue() {
			return values.get(0);
		}

	}

	@BMApi(version = "3")
	public static class Part {

		@Override
		public String toString() {
			return "Part [mime=" + mime + ", enc=" + encoding + ", cs=" + charset + ", address=" + address
					+ ", fileName=" + fileName //
					+ ", cid=" + contentId //
					+ ", disp=" + dispositionType //
					+ ", headers=" + headers + ", children=" + children + ", size=" + size + "]";
		}

		public String mime;

		public String address;

		@JsonInclude(Include.NON_NULL)
		public String encoding;

		@JsonInclude(Include.NON_NULL)
		public String charset;

		@JsonInclude(Include.NON_NULL)
		public String fileName;

		@JsonInclude(Include.NON_EMPTY)
		public List<Header> headers = new LinkedList<>();

		@JsonInclude(Include.NON_NULL)
		public String contentId;

		/**
		 * Parts with a multipart/... mime type have children.
		 */
		@JsonInclude(Include.NON_EMPTY)
		public List<Part> children = new LinkedList<>();

		/**
		 * Only for leaf parts
		 */
		public int size;

		@JsonInclude(Include.NON_NULL)
		public DispositionType dispositionType;

		public byte[] content;

		public static Part create(String file, String mime, String addr) {
			return create(file, mime, addr, 0);
		}

		public static Part create(String file, String mime, String addr, int size) {
			Part p = new Part();
			p.mime = mime;
			p.fileName = file;
			p.address = addr;
			p.size = size;
			return p;
		}

		public boolean hasRealAttachments() {
			return hasRealAttachments(this, null);
		}

		public List<Part> attachments() {
			return attachments(this, null, new LinkedList<>());
		}

		public List<Part> nonInlineAttachments() {
			return nonInlineAttachments(this, null, new LinkedList<>());
		}

		public List<Part> inlineAttachments() {
			return inlineAttachments(this, null, new LinkedList<>());
		}

		private static boolean hasRealAttachments(Part structure, Part parent) {
			boolean ret = false;
			if (parent != null && DispositionType.ATTACHMENT == structure.dispositionType
					&& !"multipart/related".equals(parent.mime)) {
				return true;
			}
			for (Part p : structure.children) {
				ret = hasRealAttachments(p, structure);
				if (ret) {
					break;
				}
			}
			return ret;
		}

		private static List<Part> attachments(Part structure, Part parent, List<Part> attach) {
			if (parent != null) {
				attach.add(structure);
			}
			for (Part p : structure.children) {
				attachments(p, structure, attach);
			}
			return attach;
		}

		private static List<Part> nonInlineAttachments(Part structure, Part parent, List<Part> attach) {
			if (parent != null && DispositionType.ATTACHMENT == structure.dispositionType) {
				attach.add(structure);
			}
			for (Part p : structure.children) {
				nonInlineAttachments(p, structure, attach);
			}
			return attach;
		}

		private static List<Part> inlineAttachments(Part structure, Part parent, List<Part> attach) {
			if (parent != null
					&& (DispositionType.INLINE == structure.dispositionType || structure.contentId != null)) {
				attach.add(structure);
			}
			for (Part p : structure.children) {
				inlineAttachments(p, structure, attach);
			}
			return attach;
		}

	}

	@BMApi(version = "3")
	public static enum RecipientKind {
		Originator, Sender, Primary, CarbonCopy, BlindCarbonCopy;
	}

	@BMApi(version = "3")
	public static class Recipient {
		public RecipientKind kind;
		public String address;

		@JsonInclude(Include.NON_NULL)
		public String dn;

		public static Recipient create(RecipientKind kind, String dn, String address) {
			Recipient rcpt = new Recipient();
			rcpt.kind = kind;
			rcpt.dn = dn;
			rcpt.address = address;
			return rcpt;
		}

		public String toString() {
			return (dn != null ? dn + " <" : "<") + address + ">";
		}
	}

	public String guid;
	public String subject;

	/**
	 * True with real attachments, false if all attachments are inline
	 */
	public boolean smartAttach;

	public Date date;
	public int size;
	public List<Header> headers = Collections.emptyList();
	public List<Recipient> recipients = Collections.emptyList();
	public String messageId;
	public List<String> references = Collections.emptyList();
	public Part structure;
	public String preview;
	public int bodyVersion;

	public static MessageBody of(String subject, Part structure) {
		MessageBody mb = new MessageBody();
		mb.subject = subject;
		mb.structure = structure;
		return mb;
	}

	@Override
	public String toString() {
		return "B{id: " + guid + ", struc: " + structure + "}";
	}

}

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
package net.bluemind.eas.backend.bm.mail;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.james.mime4j.dom.field.FieldName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.mail.api.IMailboxItems;
import net.bluemind.backend.mail.api.MailboxItem;
import net.bluemind.backend.mail.api.MessageBody.Header;
import net.bluemind.backend.mail.api.MessageBody.Part;
import net.bluemind.core.api.Stream;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.backend.MSEmail;
import net.bluemind.eas.backend.MailFolder;
import net.bluemind.eas.backend.bm.impl.CoreConnect;
import net.bluemind.eas.backend.bm.mail.loader.BodyAccumulator;
import net.bluemind.eas.dto.base.AirSyncBaseRequest;
import net.bluemind.eas.dto.base.AirSyncBaseResponse;
import net.bluemind.eas.dto.base.AirSyncBaseResponse.Attachment;
import net.bluemind.eas.dto.base.AirSyncBaseResponse.Attachment.Method;
import net.bluemind.eas.dto.base.BodyOptions;
import net.bluemind.eas.dto.base.BodyType;

/**
 * Creates a {@link MSEmail} from an imap uid.
 * 
 * 
 */
public class BodyMailLoader extends CoreConnect {

	private final BackendSession bs;
	private final MailFolder folder;

	private static final Logger logger = LoggerFactory.getLogger(BodyMailLoader.class);

	/**
	 * @param bf           the body factory used to process the body parts
	 * @param bs
	 * @param collectionId used to encode unique identifiers for attachments
	 * @param store        must be in selected state
	 */
	public BodyMailLoader(BackendSession bs, MailFolder folder) {
		this.bs = bs;
		this.folder = folder;
	}

	public InputStream fetchMimeInputStream(int id) {

		IMailboxItems service = getMailboxItemsService(bs, folder.uid);
		ItemValue<MailboxItem> item = service.getCompleteById(id);
		if (item == null) {
			return null;
		}

		try {
			BodyOptions options = new BodyOptions();
			AirSyncBaseRequest.BodyPreference bp = new AirSyncBaseRequest.BodyPreference();
			bp.truncationSize = Integer.MAX_VALUE;
			bp.type = BodyType.MIME;
			options.bodyPrefs = new ArrayList<AirSyncBaseRequest.BodyPreference>(1);
			options.bodyPrefs.add(bp);
			Stream content = service.fetchComplete(item.value.imapUid);
			BodyAccumulator bodyAccumulator = new BodyAccumulator(options);
			return bodyAccumulator.toInputStream(content);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return null;
		}

	}

	public AirSyncBaseResponse fetch(int id, BodyOptions options) {
		IMailboxItems service = getMailboxItemsService(bs, folder.uid);
		ItemValue<MailboxItem> item = service.getCompleteById(id);
		if (item == null) {
			return null;
		}

		BodyAccumulator bodyAccumulator = new BodyAccumulator(options);

		if (bodyAccumulator.getBodyType() == BodyType.MIME) {
			Stream content = service.fetchComplete(item.value.imapUid);
			bodyAccumulator.consumeMime(content);
		} else {
			Part bodyPart = bodyPart(item.value.body.structure);
			Stream content = service.fetch(item.value.imapUid, bodyPart.address, bodyPart.encoding, bodyPart.mime,
					bodyPart.charset, null);
			bodyAccumulator.consumeBodyPart(bodyPart, content);
		}

		return toAirSyncBaseResponse(bodyAccumulator, item);

	}

	private Part bodyPart(Part root) {
		if (root.mime.startsWith("multipart/")) {
			switch (root.mime) {
			case "multipart/mixed":
				return bodyPart(root.children.get(0));
			case "multipart/alternative":
				int partIdx = root.children.size() - 1;
				Part ret = bodyPart(root.children.get(partIdx));
				if (ret.mime.equals("text/calendar")) {
					ret = bodyPart(root.children.get(partIdx - 1));
				}
				return ret;
			default:
			case "multipart/related":
				return bodyPart(root.children.get(0));
			}
		}
		return root;
	}

	private AirSyncBaseResponse toAirSyncBaseResponse(BodyAccumulator bodyAccumulator, ItemValue<MailboxItem> item) {
		AirSyncBaseResponse msm = new AirSyncBaseResponse();
		msm.body = bodyAccumulator.body();
		msm.nativeBodyType = bodyAccumulator.nativeBodyType();
		msm.attachments = addAttachments(item.internalId, item.value.body.structure.attachments());
		return msm;
	}

	private Set<Attachment> addAttachments(long id, List<Part> attachments) {
		Set<Attachment> ret = new HashSet<>();

		for (Part p : attachments) {
			if (!"application/ics".equals(p.mime)) {
				Attachment at = newAttach(id, p);
				ret.add(at);
			}
		}
		return ret;
	}

	private Attachment newAttach(long id, Part p) {
		Attachment at = new Attachment();
		at.displayName = p.fileName;
		at.method = Method.Normal;

		if (p.contentId != null) {
			String contentId = p.contentId;

			if (contentId.startsWith("<") && contentId.endsWith(">")) {
				contentId = contentId.substring(1, contentId.length() - 1);
			}

			at.contentId = contentId;

			Optional<Header> cdf = p.headers.stream()
					.filter(h -> FieldName.CONTENT_DISPOSITION.equalsIgnoreCase(h.name)).findFirst();

			if (cdf.isPresent()) {
				at.isInline = cdf.get().firstValue().contains("inline");
			} else {
				// BM-9912
				at.isInline = true;
			}
			at.method = Method.OLE;
			// Android wants a display name for inline content
			String dn = at.displayName;
			if (dn == null || dn.isEmpty()) {
				dn = contentId;
			}

			String mimeType = p.mime;
			if ("image/jpeg".equals(mimeType) && !(dn.endsWith(".jpeg") || dn.endsWith(".jpg"))) {
				at.displayName = dn + ".jpeg";
			} else if ("image/png".equals(mimeType) && !dn.endsWith(".png")) {
				at.displayName = dn + ".png";
			} else if ("image/gif".equals(mimeType) && !dn.endsWith(".gif")) {
				at.displayName = dn + ".gif";
			} else if ("image/bmp".equals(mimeType) && !dn.endsWith(".bmp")) {
				at.displayName = dn + ".bmp";
			}

		}

		at.fileReference = AttachmentHelper.getAttachmentId(folder.collectionId, id, p.address, p.mime, p.encoding);
		at.estimateDataSize = p.size;

		return at;
	}

}

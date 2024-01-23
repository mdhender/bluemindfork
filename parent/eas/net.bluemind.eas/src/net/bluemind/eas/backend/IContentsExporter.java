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
package net.bluemind.eas.backend;

import java.util.Date;
import java.util.List;
import java.util.Map;

import net.bluemind.eas.dto.base.AppData;
import net.bluemind.eas.dto.base.BodyOptions;
import net.bluemind.eas.dto.email.AttachmentResponse;
import net.bluemind.eas.dto.find.FindRequest;
import net.bluemind.eas.dto.find.FindResponse.Response;
import net.bluemind.eas.dto.resolverecipients.ResolveRecipientsRequest;
import net.bluemind.eas.dto.resolverecipients.ResolveRecipientsResponse;
import net.bluemind.eas.dto.resolverecipients.ResolveRecipientsResponse.Response.Recipient;
import net.bluemind.eas.dto.sync.CollectionId;
import net.bluemind.eas.dto.sync.CollectionSyncRequest.Options;
import net.bluemind.eas.dto.sync.SyncState;
import net.bluemind.eas.dto.type.ItemDataType;
import net.bluemind.eas.exception.ActiveSyncException;
import net.bluemind.eas.exception.CollectionNotFoundException;
import net.bluemind.eas.exception.ObjectNotFoundException;

/**
 * The exporter API fetches data from the backend store and returns it to the
 * mobile device
 * 
 * 
 */
public interface IContentsExporter {

	Changes getChanged(BackendSession bs, SyncState state, Options options, CollectionId collectionId)
			throws ActiveSyncException;

	AppData loadStructure(BackendSession bs, BodyOptions bodyOptions, ItemChangeReference ir)
			throws ActiveSyncException;

	Map<Long, AppData> loadStructures(BackendSession bs, BodyOptions bodyOptions, ItemDataType type,
			CollectionId collectionId, List<Long> ids) throws ActiveSyncException;

	AttachmentResponse getAttachmentMetadata(BackendSession bs, String attachmentName) throws ObjectNotFoundException;

	MSAttachementData getAttachment(BackendSession bs, String attachmentName) throws ActiveSyncException;

	List<ResolveRecipientsResponse.Response.Recipient> resolveRecipients(BackendSession bs, List<String> to,
			ResolveRecipientsRequest.Options.Picture picture);

	Recipient.Availability fetchAvailability(BackendSession bs, String emailAddress, Date startTime, Date endTime);

	Response find(BackendSession bs, FindRequest query) throws CollectionNotFoundException;

}

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
import net.bluemind.eas.dto.resolverecipients.ResolveRecipientsRequest;
import net.bluemind.eas.dto.resolverecipients.ResolveRecipientsResponse;
import net.bluemind.eas.dto.resolverecipients.ResolveRecipientsResponse.Response.Recipient;
import net.bluemind.eas.dto.sync.FilterType;
import net.bluemind.eas.dto.sync.SyncState;
import net.bluemind.eas.dto.type.ItemDataType;
import net.bluemind.eas.exception.ActiveSyncException;
import net.bluemind.eas.exception.ObjectNotFoundException;

/**
 * The exporter API fetches data from the backend store and returns it to the
 * mobile device
 * 
 * 
 */
public interface IContentsExporter {

	Changes getChanged(BackendSession bs, SyncState state, FilterType filterType, Integer collectionId)
			throws ActiveSyncException;

	AppData loadStructure(BackendSession bs, BodyOptions bodyOptions, ItemChangeReference ir)
			throws ActiveSyncException;

	Map<String, AppData> loadStructures(BackendSession bs, BodyOptions bodyOptions, ItemDataType type, int collectionId,
			List<String> uids) throws ActiveSyncException;

	AttachmentResponse getAttachmentMetadata(BackendSession bs, String attachmentName) throws ObjectNotFoundException;

	MSAttachementData getEmailAttachement(BackendSession bs, String attachmentName) throws ObjectNotFoundException;

	List<ResolveRecipientsResponse.Response.Recipient> resolveRecipients(BackendSession bs, List<String> to,
			ResolveRecipientsRequest.Options.Picture picture);

	Recipient.Availability fetchAvailability(BackendSession bs, String emailAddress, Date startTime, Date endTime);

}

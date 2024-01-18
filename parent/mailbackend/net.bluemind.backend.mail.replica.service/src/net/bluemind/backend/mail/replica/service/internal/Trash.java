/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.backend.mail.replica.service.internal;

import java.util.List;

import net.bluemind.backend.mail.api.IMailboxFoldersByContainer;
import net.bluemind.backend.mail.api.IMailboxItems;
import net.bluemind.backend.mail.api.ImportMailboxItemSet;
import net.bluemind.backend.mail.api.ImportMailboxItemSet.MailboxItemId;
import net.bluemind.backend.mail.api.ImportMailboxItemsStatus;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.api.flags.FlagUpdate;
import net.bluemind.backend.mail.api.flags.MailboxItemFlag;
import net.bluemind.backend.mail.replica.api.IDbByContainerReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IDbReplicatedMailboxes;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;

public class Trash {

	private final BmContext context;
	private final String subtreeContainer;
	private final IDbMailboxRecords sourceFolderRecordService;

	public Trash(BmContext context, String subtreeContainer, IDbMailboxRecords sourceFolderRecordService) {
		this.context = context;
		this.subtreeContainer = subtreeContainer;
		this.sourceFolderRecordService = sourceFolderRecordService;
	}

	/**
	 * Delete multiple mails which are not already in the trash folder. Deletion
	 * consists in storing a copy of the mails to the trash folder and applying the
	 * DELETED flag (soft delete) The mails in the source folder will be
	 * hard-deleted
	 * 
	 * @param sourceFolderId id of the source folder
	 * @param ids            item ids of the mail items on the source folder
	 */
	public void deleteItems(Long sourceFolderId, List<Long> ids) {
		if (ids.isEmpty()) {
			return;
		}
		IDbByContainerReplicatedMailboxes replicaService = context.provider()
				.instance(IDbByContainerReplicatedMailboxes.class, subtreeContainer);

		ItemValue<MailboxFolder> trashFolder = getTrashFolder(replicaService);
		if (trashFolder != null) {
			ImportMailboxItemsStatus importItems = softDeleteAndMoveItemsToTrash(replicaService, sourceFolderId, ids,
					trashFolder);
			hardDeleteSourceItems(importItems);
		}
	}

	private void hardDeleteSourceItems(ImportMailboxItemsStatus importItems) {
		importItems.doneIds.stream().map(moved -> moved.source).forEach(sourceFolderRecordService::deleteById);
	}

	private ImportMailboxItemsStatus softDeleteAndMoveItemsToTrash(IDbReplicatedMailboxes replicaService,
			Long sourceFolderId, List<Long> ids, ItemValue<MailboxFolder> trashFolder) {
		IMailboxItems mboxService = context.provider().instance(IMailboxItems.class,
				replicaService.getCompleteById(sourceFolderId).uid);
		mboxService.addFlag(FlagUpdate.of(ids, MailboxItemFlag.System.Deleted.value()));
		IMailboxFoldersByContainer folderService = context.provider().instance(IMailboxFoldersByContainer.class,
				subtreeContainer);
		ImportMailboxItemSet items = ImportMailboxItemSet.of(sourceFolderId,
				ids.stream().map(id -> MailboxItemId.of(id)).toList(), false);
		return folderService.importItems(trashFolder.internalId, items);
	}

	private ItemValue<MailboxFolder> getTrashFolder(IDbReplicatedMailboxes replicaService) {
		return replicaService.trash();
	}

}

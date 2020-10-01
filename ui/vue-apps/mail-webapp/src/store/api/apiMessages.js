import { inject } from "@bluemind/inject";
import map from "lodash.map";
import flatmap from "lodash.flatmap";

import MessageAdaptor from "../messages/helpers/MessageAdaptor";
import { ItemFlag } from "@bluemind/core.container.api";

export default {
    async deleteFlag(messages, mailboxItemFlag) {
        const byFolder = groupByFolder(messages);
        const requests = map(byFolder, ({ itemsId }, folder) => api(folder).deleteFlag({ itemsId, mailboxItemFlag }));
        return Promise.all(requests);
    },
    addFlag(messages, mailboxItemFlag) {
        const byFolder = groupByFolder(messages);
        const requests = map(byFolder, ({ itemsId }, folder) => api(folder).addFlag({ itemsId, mailboxItemFlag }));
        return Promise.all(requests);
    },

    async multipleById(messages) {
        const byFolder = groupByFolder(messages);
        const requests = map(byFolder, async ({ itemsId, folderRef }, folderUid) => {
            const items = await api(folderUid).multipleById(itemsId);
            return items.map(item => MessageAdaptor.fromMailboxItem(item, folderRef));
        });
        return flatmap(await Promise.all(requests));
    },

    multipleDeleteById(messages) {
        const byFolder = groupByFolder(messages);
        const requests = map(byFolder, ({ itemsId }, folder) => api(folder).multipleDeleteById(itemsId));
        return Promise.all(requests);
    },
    async sortedIds(filter, folder) {
        const service = inject("MailboxItemsPersistence", folder.remoteRef.uid);
        switch (filter) {
            case "unread": {
                return await service.unreadItems();
            }
            case "flagged": {
                const filters = { must: [ItemFlag.Important], mustNot: [ItemFlag.Deleted] };
                return await service.filteredChangesetById(0, filters).then(changeset => {
                    return changeset.created.map(itemVersion => itemVersion.id);
                });
            }
            default:
                return await service.sortedIds();
        }
    }
};

function api(folderUid) {
    return inject("MailboxItemsPersistence", folderUid);
}

function groupByFolder(messages) {
    return messages.reduce((byFolder, message) => {
        if (!byFolder[message.folderRef.uid]) {
            byFolder[message.folderRef.uid] = { itemsId: [], folderRef: message.folderRef };
        }
        byFolder[message.folderRef.uid].itemsId.push(message.remoteRef.internalId);
        return byFolder;
    }, {});
}

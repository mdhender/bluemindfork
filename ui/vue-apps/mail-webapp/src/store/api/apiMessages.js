import { inject } from "@bluemind/inject";
import map from "lodash.map";
import flatmap from "lodash.flatmap";
import chunk from "lodash.chunk";

import MessageAdaptor from "../messages/helpers/MessageAdaptor";
import { ItemFlag } from "@bluemind/core.container.api";
import { FolderAdaptor } from "../folders/helpers/FolderAdaptor";

const MAX_CHUNK_SIZE = 500;

export default {
    multipleDeleteById(messages) {
        const byFolder = groupByFolder(messages);
        const requests = map(byFolder, ({ itemsId }, folder) => api(folder).multipleDeleteById(itemsId));
        return Promise.all(requests);
    },
    deleteFlag(messages, mailboxItemFlag) {
        const byFolder = groupByFolder(messages);
        const requests = map(byFolder, ({ itemsId }, folder) => api(folder).deleteFlag({ itemsId, mailboxItemFlag }));
        return Promise.all(requests);
    },
    addFlag(messages, mailboxItemFlag) {
        const byFolder = groupByFolder(messages);
        const requests = map(byFolder, ({ itemsId }, folder) => api(folder).addFlag({ itemsId, mailboxItemFlag }));
        return Promise.all(requests);
    },
    async multipleGetById(messages) {
        const byFolder = groupByFolder(messages);
        const requests = map(byFolder, async ({ itemsId, folderRef }, folderUid) => {
            const items = flatmap(
                await Promise.all(
                    chunk(itemsId, MAX_CHUNK_SIZE).map(chunkedIds => api(folderUid).multipleGetById(chunkedIds))
                )
            );
            return items
                .filter(item => !item.flags.includes(ItemFlag.Deleted))
                .map(item => MessageAdaptor.fromMailboxItem(item, folderRef));
        });
        return flatmap(await Promise.all(requests));
    },
    async getForUpdate(message) {
        const remoteMessage = await api(message.folderRef.uid).getForUpdate(message.remoteRef.internalId);
        return MessageAdaptor.fromMailboxItem(remoteMessage, message.folderRef);
    },
    move(messages, destination) {
        const destinationUid = destination.remoteRef.uid;
        const byFolder = groupByFolder(messages);
        const requests = map(byFolder, ({ itemsId }, sourceUid) => importApi(sourceUid, destinationUid).move(itemsId));
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
    },
    async search({ pattern, folder }, filter, currentFolder) {
        const payload = { query: searchQuery(pattern, filter, folder && folder.uid), sort: searchSort() };
        const { results } = await folderApi(currentFolder.mailboxRef.uid).searchItems(payload);
        if (!results) {
            return [];
        }
        const resultKeys = new Set();
        const filteredResults = [];
        results.forEach(({ itemId, containerUid }) => {
            const resultKey = `${itemId}@${containerUid}`;
            if (!resultKeys.has(resultKey)) {
                const folderRef = FolderAdaptor.toRef(extractFolderUid(containerUid));
                filteredResults.push({ id: itemId, folderRef });
                resultKeys.add(resultKey);
            }
        });
        return filteredResults;
    }
};

function api(folderUid) {
    return inject("MailboxItemsPersistence", folderUid);
}

function folderApi(mailboxUid) {
    return inject("MailboxFoldersPersistence", mailboxUid);
}

function importApi(sourceUid, destinationUid) {
    return inject("ItemsTransferPersistence", sourceUid, destinationUid);
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

function searchQuery(query, filter, folderUid) {
    return { query, recordQuery: filterQuery(filter), maxResults: MAX_CHUNK_SIZE, scope: searchScope(folderUid) };
}

function filterQuery(filter) {
    switch (filter) {
        case "unread":
        case "flagged":
            return "is:" + filter;
        default:
            return "";
    }
}

function searchScope(folderUid) {
    return { folderScope: { folderUid }, isDeepTraversal: false };
}

function searchSort() {
    return { criteria: [{ field: "date", order: "Desc" }] };
}

function extractFolderUid(containerUid) {
    return containerUid.replace("mbox_records_", "");
}

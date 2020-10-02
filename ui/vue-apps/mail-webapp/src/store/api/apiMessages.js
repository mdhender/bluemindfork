import { inject } from "@bluemind/inject";
import map from "lodash.map";
import flatmap from "lodash.flatmap";

import MessageAdaptor from "../messages/helpers/MessageAdaptor";

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

import { ItemFlag } from "@bluemind/core.container.api";
import { inject } from "@bluemind/inject";
import { ConversationListFilter, SortOrder } from "../conversationList";

export default {
    multipleGet(conversations, mailboxRef) {
        return conversationApi(mailboxRef.uid).multipleGet(conversations.map(({ remoteRef: { uid } }) => uid));
    },
    addFlag(conversations, flag, mailbox) {
        return manageFlag(conversations, flag, mailbox, "addFlag");
    },
    deleteFlag(conversations, flag, mailbox) {
        return manageFlag(conversations, flag, mailbox, "deleteFlag");
    },
    multipleDeleteById(conversations, mailbox) {
        return groupByFolderAndExecute(conversations, mailbox, ({ mailbox, folderUid, conversations }) =>
            conversationActionsApi(mailbox.remoteRef.uid, folderUid).multipleDeleteById(
                conversations.map(({ remoteRef: { uid } }) => uid)
            )
        );
    },
    move(conversations, folder, mailbox) {
        return groupByFolderAndExecute(conversations, mailbox, ({ mailbox, folderUid, conversations }) =>
            conversationActionsApi(mailbox.remoteRef.uid, folderUid).move(
                folder.remoteRef.uid,
                conversations.map(({ remoteRef: { uid } }) => uid)
            )
        );
    },
    sortedIds(filter, sort, folder) {
        return conversationApi(folder.mailboxRef.uid).byFolder(folder.remoteRef.uid, toSortDescriptor(filter, sort));
    }
};

function manageFlag(conversations, flag, mailbox, apiFunctionName) {
    return groupByFolderAndExecute(conversations, mailbox, ({ mailbox, folderUid, conversations }) =>
        conversationActionsApi(mailbox.remoteRef.uid, folderUid)[apiFunctionName]({
            conversationUids: conversations.map(({ remoteRef: { uid } }) => uid),
            mailboxItemFlag: flag
        })
    );
}

function groupByFolderAndExecute(conversations, mailbox, fn) {
    const promises = [];
    const byFolder = groupByFolder(conversations);
    for (const folderUid in byFolder) {
        promises.push(fn({ mailbox, folderUid, conversations: byFolder[folderUid] }));
    }
    return Promise.all(promises);
}

function groupByFolder(items) {
    return items.reduce((byFolder, item) => {
        if (!byFolder[item.folderRef.uid]) {
            byFolder[item.folderRef.uid] = [];
        }
        byFolder[item.folderRef.uid].push(item);
        return byFolder;
    }, {});
}

function conversationApi(mailboxUid) {
    return inject("MailConversationPersistence", mailboxUid);
}

function conversationActionsApi(mailboxUid, folderUid) {
    return inject("MailConversationActionsPersistence", mailboxUid, folderUid);
}

function toSortDescriptor(filter, sort) {
    const sortDescriptor = {
        fields: [
            {
                column: sort.field,
                dir: sort.order === SortOrder.ASC ? "Asc" : "Desc"
            }
        ],
        filter: { must: [], mustNot: [ItemFlag.Deleted] }
    };
    switch (filter) {
        case ConversationListFilter.UNREAD:
            sortDescriptor.filter.mustNot.push(ItemFlag.Seen);
            break;
        case ConversationListFilter.FLAGGED:
            sortDescriptor.filter.must.push(ItemFlag.Important);
            break;
    }
    return sortDescriptor;
}

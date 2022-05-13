import { inject } from "@bluemind/inject";

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
    move(conversations, folder, mailbox) {
        return groupByFolderAndExecute(conversations, mailbox, ({ mailbox, folderUid, conversations }) =>
            conversationActionsApi(mailbox.remoteRef.uid, folderUid).move(
                folder.remoteRef.uid,
                conversations.map(({ remoteRef: { uid } }) => uid)
            )
        );
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

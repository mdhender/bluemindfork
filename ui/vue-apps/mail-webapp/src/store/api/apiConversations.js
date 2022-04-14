import { inject } from "@bluemind/inject";

export default {
    multipleGet(conversations, mailboxRef) {
        return api(mailboxRef.uid).multipleGet(conversations.map(({ remoteRef: { uid } }) => uid));
    }
};

function api(folderUid) {
    return inject("MailConversationPersistence", folderUid);
}

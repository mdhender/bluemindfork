import { inject } from "@bluemind/inject";
import { Flag } from "@bluemind/email";

function apiClient({ folderUid }) {
    return inject("MailboxItemsPersistence", folderUid);
}

function markAsRead({ uid }, { id }) {
    return apiClient({ uid }).addFlag({ messageKeys: [id], mailboxItemFlag: Flag.SEEN });
}

function markAsUnread() {
    throw new Error("not implemented");
}

async function fetchMessages({ folderUid }) {
    const changeset = await apiClient({ folderUid }).filteredChangesetById(0, {});
    const ids = Object.entries(changeset)
        .filter(([category]) => ["created", "updated"].includes(category))
        .flatMap(([, changesetData]) => changesetData)
        .map(({ id }) => id);
    return loadMessages({ folderUid }, { ids });
}

function loadMessages({ folderUid }, { ids }) {
    return apiClient({ folderUid }).multipleById(ids);
}

export default {
    fetchMessages,
    loadMessages,
    markAsRead,
    markAsUnread
};

import ServiceLocator from "@bluemind/inject";
import { ItemFlag } from "@bluemind/core.container.api";

export function list({ commit }, { sorted, folderUid, filter }) {
    const service = ServiceLocator.getProvider("MailboxItemsPersistence").get(folderUid);

    switch (filter) {
        case "unread": {
            return unreadItems(commit, service, folderUid, sorted);
        }
        case "flagged": {
            const filters = { must: [ItemFlag.Important], mustNot: [ItemFlag.Deleted] };
            return filteredIds(service, filters, sorted, commit, folderUid);
        }
        case "all":
        default:
            return service.sortedIds(sorted).then(ids => {
                commit("setItemKeysByIdsFolderUid", { ids, folderUid });
            });
    }
}

function filteredIds(service, filters, sorted, commit, folderUid) {
    return service.filteredChangesetById(0, filters).then(changeset => {
        let ids = changeset.created.map(itemVersion => itemVersion.id);
        if (sorted && sorted.dir.toLowerCase() === "asc") {
            ids = ids.reverse();
        }
        commit("setItemKeysByIdsFolderUid", { ids, folderUid });
    });
}

function unreadItems(commit, service, folderUid, sorted) {
    return service.unreadItems().then(ids => {
        if (sorted && sorted.dir.toLowerCase() === "asc") {
            ids = ids.reverse();
        }
        commit("setItemKeysByIdsFolderUid", { ids, folderUid });
    });
}

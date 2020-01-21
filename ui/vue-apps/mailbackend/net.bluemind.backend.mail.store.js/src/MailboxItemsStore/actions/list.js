import ServiceLocator from "@bluemind/inject";
import { ItemFlag } from "@bluemind/core.container.api";

export function list({ commit }, { sorted, folderUid, filter }) {
    const service = ServiceLocator.getProvider("MailboxItemsPersistence").get(folderUid);
    switch (filter) {
        case "unread": {
            const filters = { must: [], mustNot: [ItemFlag.Deleted, ItemFlag.Seen] };
            return service.filteredChangesetById(0, filters).then(changeset => {
                const ids = changeset.created.map(itemVersion => itemVersion.id);
                if (sorted && sorted.dir.toLowerCase() === "asc") {
                    return ids.reverse();
                }
                commit("setItemKeys", { ids, folderUid });
            });
        }
        case "all":
        default:
            return service.sortedIds(sorted).then(ids => {
                commit("setItemKeys", { ids, folderUid });
            });
    }
}

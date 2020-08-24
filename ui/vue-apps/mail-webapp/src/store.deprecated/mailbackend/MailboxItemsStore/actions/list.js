import ServiceLocator from "@bluemind/inject";
import { ItemFlag } from "@bluemind/core.container.api";
import { FETCH_MESSAGES } from "../../../../store/messages/actions";

//FIXME: Aucune raison de faire les appels d'API ici
export function list({ commit, dispatch }, { sorted, folderUid, filter }) {
    const service = ServiceLocator.getProvider("MailboxItemsPersistence").get(folderUid);

    switch (filter) {
        case "unread": {
            return service.unreadItems().then(ids => {
                if (sorted && sorted.dir.toLowerCase() === "asc") {
                    ids = ids.reverse();
                }
                commit("setItemKeysByIdsFolderUid", { ids, folderUid });
            });
        }
        case "flagged": {
            const filters = { must: [ItemFlag.Important], mustNot: [ItemFlag.Deleted] };
            return service.filteredChangesetById(0, filters).then(changeset => {
                let ids = changeset.created.map(itemVersion => itemVersion.id);
                if (sorted && sorted.dir.toLowerCase() === "asc") {
                    ids = ids.reverse();
                }
                commit("setItemKeysByIdsFolderUid", { ids, folderUid });
            });
        }
        case "all":
        default:
            //FIXME: On ne veut garder que ce dispatch
            dispatch("mail/" + FETCH_MESSAGES, { folderUid }, { root: true });
            return service.sortedIds(sorted).then(ids => {
                commit("setItemKeysByIdsFolderUid", { ids, folderUid });
            });
    }
}

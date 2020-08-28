import { inject } from "@bluemind/inject";
import { ItemFlag } from "@bluemind/core.container.api";
import MessageAdaptor from "./messages/MessageAdaptor";
import mutationTypes from "./mutationTypes";
import actionTypes from "./actionTypes";

const state = {
    messageKeys: []
};

const mutations = {
    [mutationTypes.CLEAR_MESSAGE_LIST]: state => {
        state.messageKeys = [];
    },
    [mutationTypes.REMOVE_MESSAGES]: (state, messageKeys) => {
        state.messageKeys = state.messageKeys.filter(key => !messageKeys.includes(key));
    },
    [mutationTypes.SET_MESSAGE_LIST]: (state, messages) => {
        state.messageKeys = messages.map(m => m.key);
    }
};

const actions = {
    async [actionTypes.FETCH_FOLDER_MESSAGE_KEYS]({ commit }, { folder, filter }) {
        const service = inject("MailboxItemsPersistence", folder.uid);
        let ids;
        switch (filter) {
            case "unread": {
                ids = await service.unreadItems();
                break;
            }
            case "flagged": {
                const filters = { must: [ItemFlag.Important], mustNot: [ItemFlag.Deleted] };
                ids = await service.filteredChangesetById(0, filters).then(changeset => {
                    return changeset.created.map(itemVersion => itemVersion.id);
                });
                break;
            }
            default:
                ids = await service.sortedIds();
                break;
        }
        const messages = ids.map(id => MessageAdaptor.create(id, folder));
        commit(mutationTypes.SET_MESSAGE_LIST, messages);
    }
};

export default {
    actions,
    mutations,
    state,
    getters: {}
};

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
    async [actionTypes.FETCH_FOLDER_MESSAGE_KEYS]({ commit }, { folder, filter, conversationsEnabled }) {
        const service = inject("MailboxItemsPersistence", folder.remoteRef.uid);
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

        ids = conversationsEnabled ? await conversationFilter(folder, ids) : ids;

        const messages = ids.map(id => MessageAdaptor.create(id, folder));

        commit(mutationTypes.SET_MESSAGE_LIST, messages);
    }
};

async function conversationFilter(folder, ids) {
    let conversations = await inject("MailConversationPersistence").byFolder(folder.uid);
    return (
        conversations
            // extract first message from each conversation matching one of ids
            .map(({ value: { messageIds } }) => {
                const message = messageIds.sort((a, b) => a.date - b.date).find(({ itemId }) => ids.includes(itemId));
                return message ? { itemId: message.itemId, date: message.date } : undefined;
            })
            .filter(Boolean)
            // order by creation date, newer message to older one
            .sort((a, b) => b.date - a.date)
            .map(m => m.itemId)
    );
}

export default {
    actions,
    mutations,
    state,
    getters: {}
};

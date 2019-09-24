import * as actions from "./actions.js";
import * as getters from "./getters.js";
import * as mutations from "./mutations.js";
import DraftStatus from "./DraftStatus.js";

const state = {
    attachments: [],
    current: null,
    items: {},
    sortedIds: [],
    unreadCount: 0,
    partsToDisplay: [],
    search: {
        pattern: null,
        loading: false,
        error: false
    },
    shouldRemoveItem: null,
    draft: {
        id: null,
        status: DraftStatus.NEW,
        saveDate: null
    }
};

export default {
    namespaced: true,
    state,
    actions,
    mutations,
    getters
};

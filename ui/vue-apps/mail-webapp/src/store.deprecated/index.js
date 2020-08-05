import * as actions from "./actions";
import events from "./events/events";
import * as getters from "./getters";
import * as mutations from "./mutations";
import * as mutactions from "./mutactions";
import * as state from "./state";
import messages from "../store.deprecated/mailbackend/MailboxItemsStore";
import folders from "../store.deprecated/mailbackend/MailboxFoldersStore";
import MessageStore from "./MessageStore/";
import search from "./modules/search";

export default {
    namespaced: true,
    state: Object.assign({}, state, events.state),
    actions: Object.assign({}, actions, mutactions, events.actions),
    mutations: Object.assign({}, mutations, events.mutations),
    getters,
    modules: {
        search,
        messages,
        folders,
        currentMessage: MessageStore,
        draft: MessageStore
    }
};

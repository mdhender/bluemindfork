import * as actions from "./actions";
import * as getters from "./getters";
import * as mutations from "./mutations";
import * as mutactions from "./mutactions";
import * as state from "./state";
import messages from "../store.deprecated/mailbackend/MailboxItemsStore";
import MessageStore from "./MessageStore/";
import search from "./modules/search";

export default {
    namespaced: true,
    state: Object.assign({}, state),
    actions: Object.assign({}, actions, mutactions),
    mutations: Object.assign({}, mutations),
    getters,
    modules: {
        search,
        messages,
        currentMessage: MessageStore
    }
};

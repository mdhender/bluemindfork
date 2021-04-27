import * as actions from "./actions";
import messages from "../store.deprecated/mailbackend/MailboxItemsStore";

export default {
    namespaced: true,
    state: {},
    actions: actions,
    mutations: {},
    getters: {},
    modules: {
        messages
    }
};

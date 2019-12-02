import * as actions from "./actions";
import * as getters from "./getters";
import * as mutations from "./mutations";
import * as state from "./state";
import { MailboxItemsStore as messages, MailboxFoldersStore as folders } from "@bluemind/backend.mail.store";
export default {
    namespaced: true,
    state: Object.assign({}, state),
    actions,
    mutations,
    getters,
    modules: {
        messages,
        folders
    }
};

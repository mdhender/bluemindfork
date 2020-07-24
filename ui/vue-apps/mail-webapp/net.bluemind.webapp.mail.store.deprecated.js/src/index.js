import * as actions from "./actions";
import events from "./events/events";
import * as getters from "./getters";
import * as mutations from "./mutations";
import * as mutactions from "./mutactions";
import * as state from "./state";
import { MailboxItemsStore as messages, MailboxFoldersStore as folders } from "@bluemind/backend.mail.store";
import { ContainersStore as mailboxes } from "@bluemind/core.container.store";
import MessageStore from "./MessageStore/";
import search from "./modules/search";
export * from "./SearchHelper";
export * from "./FolderHelper";

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
        mailboxes,
        currentMessage: MessageStore,
        draft: MessageStore
    }
};

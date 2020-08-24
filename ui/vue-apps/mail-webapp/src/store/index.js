import * as app from "./app";
import * as consultPanel from "./consultPanel";
import folders from "./folders";
import messages from "./messages";
import * as folderList from "./folderList";
import * as mailboxes from "./mailboxes";

export default {
    namespaced: true,
    state: { ...app.state, ...consultPanel.state, ...folderList.state, ...mailboxes.state },
    actions: { ...consultPanel.actions, ...mailboxes.actions },
    mutations: {
        ...app.mutations,
        ...consultPanel.mutations,
        ...folderList.mutations,
        ...mailboxes.mutations
    },
    getters: { ...app.getters, ...mailboxes.getters },
    modules: {
        folders,
        messages
    }
};

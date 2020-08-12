import * as app from "./app";
import * as consultPanel from "./consultPanel";
import * as folders from "./folders";
import * as folderList from "./folderList";
import * as mailboxes from "./mailboxes";

export default {
    namespaced: true,
    state: { ...app.state, ...consultPanel.state, ...folders.state, ...folderList.state, ...mailboxes.state },
    actions: { ...consultPanel.actions, ...folders.actions, ...mailboxes.actions },
    mutations: {
        ...app.mutations,
        ...consultPanel.mutations,
        ...folders.mutations,
        ...folderList.mutations,
        ...mailboxes.mutations
    },
    getters: { ...app.getters, ...folders.getters, ...mailboxes.getters }
};

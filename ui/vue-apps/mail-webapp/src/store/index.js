import { state, actions, getters, mutations } from "./store";
import activeMessage from "./activeMessage";
import messageCompose from "./messageCompose";
import folders from "./folders";
import messages from "./messages";
import consultPanel from "./consultPanel";
import messageList from "./messageList";
import folderList from "./folderList";
import mailboxes from "./mailboxes";
import selection from "./selection";

export default {
    namespaced: true,
    actions,
    state,
    getters,
    mutations,
    modules: {
        activeMessage,
        folders,
        mailboxes,
        messages,
        selection,
        folderList,
        messageList,
        messageCompose,
        consultPanel
    }
};

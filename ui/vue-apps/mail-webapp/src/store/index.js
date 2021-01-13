import activeMessage from "./activeMessage";
import consultPanel from "./consultPanel";
import folderList from "./folderList";
import folders from "./folders";
import mailboxes from "./mailboxes";
import messages from "./messages";
import messageCompose from "./messageCompose";
import messageList from "./messageList";
import selection from "./selection";
import { state, getters, mutations } from "./store";

export default {
    namespaced: true,
    state,
    getters,
    mutations,
    modules: {
        activeMessage,
        consultPanel,
        folderList,
        folders,
        mailboxes,
        messageCompose,
        messageList,
        messages,
        selection
    }
};

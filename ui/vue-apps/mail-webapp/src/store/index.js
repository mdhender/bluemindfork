import activeMessage from "./activeMessage";
import { state, getters, mutations } from "./store";
import consultPanel from "./consultPanel";
import folderList from "./folderList";
import folders from "./folders";
import mailboxes from "./mailboxes";
import messages from "./messages";
import messageCompose from "./messageCompose";
import messageList from "./messageList";
import route from "./route";
import selection from "./selection";

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
        route,
        selection
    }
};

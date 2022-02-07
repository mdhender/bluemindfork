import activeMessage from "./activeMessage";
import consultPanel from "./consultPanel";
import conversations from "./conversations";
import folderList from "./folderList";
import folders from "./folders";
import mailboxes from "./mailboxes";
import messageCompose from "./messageCompose";
import conversationList from "./conversationList";
import partsData from "./partsData";
import route from "./route";
import selection from "./selection";
import preview from "./preview";
import { state, getters, mutations } from "./store";

export default {
    namespaced: true,
    state,
    getters,
    mutations,
    modules: {
        activeMessage,
        partsData,
        consultPanel,
        conversations,
        folderList,
        folders,
        mailboxes,
        messageCompose,
        conversationList,
        route,
        selection,
        preview
    }
};

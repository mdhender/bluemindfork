import activeMessage from "./activeMessage";
import addressAutocomplete from "./addressAutocomplete";
import consultPanel from "./consultPanel";
import conversationList from "./conversationList";
import conversations from "./conversations";
import folderList from "./folderList";
import folders from "./folders";
import mailboxes from "./mailboxes";
import messageCompose from "./messageCompose";
import partsData from "./partsData";
import preview from "./preview";
import route from "./route";
import selection from "./selection";
import { state, getters, mutations } from "./store";

export default {
    namespaced: true,
    state,
    getters,
    mutations,
    modules: {
        activeMessage,
        addressAutocomplete,
        consultPanel,
        conversationList,
        conversations,
        folderList,
        folders,
        mailboxes,
        messageCompose,
        partsData,
        preview,
        route,
        selection
    }
};

import { conversationUtils } from "@bluemind/mail";
import { FETCH_TEMPLATES_KEYS } from "~/actions";
import {
    SET_TEMPLATE_CHOOSER_VISIBLE,
    SET_TEMPLATE_CHOOSER_TARGET,
    SET_TEMPLATE_LIST_LOADING,
    SET_TEMPLATE_LIST_SEARCH_PATTERN,
    SET_TEMPLATES_LIST
} from "~/mutations";
import apiMessages from "./api/apiMessages";
import { SortField, SortOrder } from "./conversationList";
import { FolderAdaptor } from "./folders/helpers/FolderAdaptor";

const { createConversationStub } = conversationUtils;

export default {
    mutations: {
        [SET_TEMPLATE_CHOOSER_VISIBLE](state, isVisible) {
            state.visible = isVisible;
        },
        [SET_TEMPLATE_CHOOSER_TARGET](state, key) {
            state.target = key;
        },
        [SET_TEMPLATE_LIST_LOADING](state, isLoading) {
            state.loading = isLoading;
        },
        [SET_TEMPLATE_LIST_SEARCH_PATTERN](state, pattern) {
            state.pattern = pattern;
        },
        [SET_TEMPLATES_LIST](state, { conversations }) {
            state.keys = conversations.map(({ key }) => key);
        }
    },
    actions: {
        async [FETCH_TEMPLATES_KEYS]({ commit, state }, folder) {
            commit(SET_TEMPLATE_LIST_LOADING, true);
            let conversations;
            if (state.pattern) {
                conversations = await search(state.pattern, folder);
            } else {
                conversations = await list(folder);
            }
            commit(SET_TEMPLATES_LIST, { conversations });
            commit(SET_TEMPLATE_LIST_LOADING, false);
        }
    },
    state: {
        keys: [],
        loading: false,
        pattern: "",
        visible: false,
        target: 0
    }
};

async function search(pattern, folder) {
    const ref = FolderAdaptor.toRef(folder);
    const { results } = await apiMessages.search(
        { pattern, folder: ref },
        undefined,
        { field: SortField.SUBJECT, order: SortOrder.ASC },
        folder
    );
    return results.slice(0, 100).map(({ id, folderRef }) => createConversationStub(id, folderRef));
}

async function list(folder) {
    const sortedIds = (
        await apiMessages.sortedIds(undefined, { field: SortField.SUBJECT, order: SortOrder.ASC }, folder)
    ).slice(0, 100);
    return sortedIds.map(id => createConversationStub(id, FolderAdaptor.toRef(folder)));
}

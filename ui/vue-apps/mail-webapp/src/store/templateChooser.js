import { createConversationStubsFromSortedIds, createConversationStubsFromSearchResult } from "~/model/conversations";
import { FETCH_TEMPLATES_KEYS } from "~/actions";
import {
    SET_TEMPLATE_CHOOSER_VISIBLE,
    SET_TEMPLATE_LIST_LOADING,
    SET_TEMPLATE_LIST_SEARCH_PATTERN,
    SET_TEMPLATES_LIST
} from "~/mutations";
import apiMessages from "./api/apiMessages";
import { FolderAdaptor } from "./folders/helpers/FolderAdaptor";

export default {
    mutations: {
        [SET_TEMPLATE_CHOOSER_VISIBLE](state, isVisible) {
            state.visible = isVisible;
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
            let conversations, messages;
            if (state.pattern) {
                ({ conversations, messages } = await search(state.pattern, folder));
            } else {
                ({ conversations, messages } = await list(folder));
            }
            commit(SET_TEMPLATES_LIST, { conversations, messages });
            commit(SET_TEMPLATE_LIST_LOADING, false);
        }
    },
    state: {
        keys: [],
        loading: false,
        pattern: "",
        visible: false
    }
};

async function search(pattern, folder) {
    const ref = FolderAdaptor.toRef(folder);
    let searchResult = (await apiMessages.search({ pattern, folder: ref }, undefined, folder)).slice(0, 100);
    return createConversationStubsFromSearchResult(searchResult);
}

async function list(folder) {
    let sortedIds = (await apiMessages.sortedIds(undefined, folder)).slice(0, 100);
    return createConversationStubsFromSortedIds(sortedIds, folder);
}

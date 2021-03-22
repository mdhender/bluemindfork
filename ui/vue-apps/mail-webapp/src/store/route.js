import { SET_ROUTE_FILTER, SET_ROUTE_FOLDER, SET_ROUTE_MAILBOX, SET_ROUTE_SEARCH } from "~mutations";
export default {
    state: {
        folder: undefined,
        filter: undefined,
        mailbox: undefined,
        search: undefined
    },
    mutations: {
        [SET_ROUTE_FILTER](state, filter) {
            state.filter = filter;
        },
        [SET_ROUTE_FOLDER](state, path) {
            state.folder = path;
        },
        [SET_ROUTE_MAILBOX](state, mailbox) {
            state.mailbox = mailbox;
        },
        [SET_ROUTE_SEARCH](state, search) {
            state.search = search;
        }
    }
};

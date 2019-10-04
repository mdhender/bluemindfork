import * as actions from "./actions";
import * as getters from "./getters";
import * as mutations from "./mutations";

const state = {
    items: [],
    folders: [],
    settings: {}
};

export default {
    namespaced: true,
    state,
    actions,
    mutations,
    getters
};

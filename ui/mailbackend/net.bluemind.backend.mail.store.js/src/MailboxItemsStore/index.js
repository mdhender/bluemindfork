import * as actions from "./actions.js";
import * as getters from "./getters.js";
import * as mutations from "./mutations.js";

const state = {
    items: [],
    parts: [],
    current: null,
    count: 0,
    partsToDisplay: []
};

export default {
    namespaced: true,
    state,
    actions,
    mutations,
    getters
};

import * as mutations from "./mutations.js";

const state = {
    errors: [],
    successes: []
};

export default {
    namespaced: true,
    state,
    mutations
};

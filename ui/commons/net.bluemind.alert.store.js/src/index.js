import * as mutations from "./mutations.js";
export { default as AlertTypes } from "./AlertTypes";
export { default as Alert } from "./Alert";

const state = {
    errors: [],
    successes: []
};

export default {
    namespaced: true,
    state,
    mutations
};

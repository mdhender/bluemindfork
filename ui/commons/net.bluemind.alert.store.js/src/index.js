import * as mutations from "./mutations.js";
export { default as AlertTypes } from "./AlertTypes";
export { default as Alert } from "./Alert";

const state = {
    alerts: []
};

export default {
    namespaced: true,
    state,
    mutations
};

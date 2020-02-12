import * as mutations from "./mutations";
export { default as Alert } from "./Alert";
export { default as AlertFactory } from "./AlertFactory";
export { default as AlertTypes } from "./AlertTypes";

const state = {
    alerts: []
};

export default {
    namespaced: true,
    state,
    mutations
};

import * as mutations from "./mutations";
export { default as Alert } from "./Alert";
export { default as AlertFactory } from "./AlertFactory";
export { default as AlertTypes } from "./AlertTypes";
export { default as withAlert } from "./actionWithAlert";

const state = {
    applicationAlerts: []
};

export default {
    state,
    mutations
};

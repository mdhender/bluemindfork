import * as mutations from "./mutations";
import AlertTypes from "./AlertTypes";
import { CLEAR, ERROR, LOADING, REMOVE, SUCCESS } from "./types";
export { default as Alert } from "./Alert";
export { default as AlertFactory } from "./AlertFactory";
export { default as AlertTypes } from "./AlertTypes";
export { withAlert } from "./withAlert";
export * from "./types";

export default {
    namespaced: true,
    state: [],
    mutations: {
        [LOADING]: (state, { uid, name, payload, renderer }) => {
            remove(state, uid);
            state.push(create({ uid, name, payload, renderer, type: AlertTypes.LOADING }));
        },
        [SUCCESS]: (state, { uid, name, payload, result, renderer }) => {
            remove(state, uid);
            state.push(create({ uid, name, payload, result, renderer, type: AlertTypes.SUCCESS }));
        },
        [ERROR]: (state, { uid, name, payload, error, renderer }) => {
            remove(state, uid);
            state.push(create({ uid, name, payload, error, renderer, type: AlertTypes.ERROR }));
        },
        [REMOVE]: (state, uid) => {
            remove(state, uid);
        },
        [CLEAR]: state => {
            state.splice(0);
        }
    },
    modules: {
        applicationAlerts: {
            state: [],
            mutations
        }
    }
};

function remove(state, uid) {
    const index = state.findIndex(alert => alert.uid === uid);
    if (index >= 0) {
        state.splice(index, 1);
    }
}

function create({ name, error, payload, result, uid, type, renderer }) {
    return { name, error, payload, result, uid, type, renderer };
}

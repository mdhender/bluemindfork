import AlertTypes from "./AlertTypes";
import { ADD, WARNING, ERROR, LOADING, REMOVE, SUCCESS } from "./types";

export { default as AlertTypes } from "./AlertTypes";
export { default as AlertMixin } from "./AlertMixin";
export { default as DefaultAlert } from "./DefaultAlert";
export { withAlert } from "./withAlert";
export * from "./types";

const timers = new Map();

export default {
    namespaced: true,
    state: [],
    mutations: {
        [ADD]: (state, alert) => {
            remove(state, alert.uid);
            state.push(alert);
        },
        [REMOVE]: (state, uid) => {
            remove(state, uid);
        }
    },
    actions: {
        [LOADING]: async ({ dispatch }, { alert, options: opt_options }) => {
            let options = { ...DEFAULT_LOADING_OPTIONS, ...(opt_options || {}) };
            await dispatch(ADD, { alert: { ...alert, type: AlertTypes.LOADING }, options });
        },
        [SUCCESS]: async ({ dispatch }, { alert, options: opt_options }) => {
            let options = { ...DEFAULT_SUCCESS_OPTIONS, ...(opt_options || {}) };
            await dispatch(ADD, { alert: { ...alert, type: AlertTypes.SUCCESS }, options });
        },
        [WARNING]: async ({ dispatch }, { alert, options: opt_options }) => {
            let options = { ...DEFAULT_WARNING_OPTIONS, ...(opt_options || {}) };
            await dispatch(ADD, { alert: { ...alert, type: AlertTypes.WARNING }, options });
        },
        [ERROR]: async ({ dispatch }, { alert, options: opt_options }) => {
            let options = { ...DEFAULT_ERROR_OPTIONS, ...(opt_options || {}) };
            await dispatch(ADD, { alert: { ...alert, type: AlertTypes.ERROR }, options });
        },
        [ADD]: async ({ commit }, { alert: payload, options }) => {
            const alert = create(payload, options);
            const { delay, countDown } = options;
            clearDelay(alert);
            const fn = () => {
                commit(ADD, create(alert, options));
                if (countDown) {
                    setDelay(alert, () => commit(REMOVE, alert.uid), countDown);
                }
            };
            if (delay) {
                setDelay(alert, fn, delay);
            } else {
                fn();
            }
        },
        [REMOVE]: async ({ commit }, payload) => {
            let alerts = Array.isArray(payload) ? payload : [payload];
            alerts.forEach(({ uid }) => {
                clearDelay({ uid });
                commit(REMOVE, uid);
            });
        }
    }
};

const DEFAULT_LOADING_OPTIONS = { delay: 1000, dismissible: false };
const DEFAULT_SUCCESS_OPTIONS = { countDown: 5000, dismissible: true };
const DEFAULT_WARNING_OPTIONS = { dismissible: true };
const DEFAULT_ERROR_OPTIONS = { dismissible: true };

function remove(state, uid) {
    const index = state.findIndex(alert => alert.uid === uid);
    if (index >= 0) {
        state.splice(index, 1);
    }
}

function create({ name, error, payload, result, uid, type }, { dismissible, renderer }) {
    return { name, error, payload, result, uid, type, renderer, dismissible };
}

function clearDelay({ uid }) {
    clearTimeout(timers.get(uid));
}

function setDelay({ uid }, fn, timout) {
    timers.set(uid, setTimeout(fn, timout));
}

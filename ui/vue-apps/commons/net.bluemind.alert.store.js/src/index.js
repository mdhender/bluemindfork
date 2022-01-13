import AlertTypes from "./AlertTypes";
import { ADD, CLEAR, ERROR, INFO, LOADING, REMOVE, SUCCESS, WARNING } from "./types";

export { default as AlertTypes } from "./AlertTypes";
export { default as AlertMixin } from "./AlertMixin";
export { default as DefaultAlert } from "./DefaultAlert";
export { default as ReadMoreAlert } from "./ReadMoreAlert";
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
        [INFO]: async ({ dispatch }, { alert, options: opt_options }) => {
            let options = { ...DEFAULT_INFO_OPTIONS, ...(opt_options || {}) };
            await dispatch(ADD, { alert: { ...alert, type: AlertTypes.INFO }, options });
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
                commit(ADD, alert);
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
        },
        [CLEAR]: async ({ state, dispatch }, filter) => {
            let alerts = filter === undefined ? state : state.filter(({ area }) => area === filter);
            dispatch(REMOVE, alerts);
        }
    }
};

const DEFAULT_OPTIONS = { dismissible: true, area: "", renderer: "DefaultAlert" };
const DEFAULT_LOADING_OPTIONS = { ...DEFAULT_OPTIONS, icon: false, delay: 1000, dismissible: false };
const DEFAULT_INFO_OPTIONS = { ...DEFAULT_OPTIONS, icon: "info-circle-plain" };
const DEFAULT_SUCCESS_OPTIONS = { ...DEFAULT_OPTIONS, icon: "check-circle", countDown: 5000 };
const DEFAULT_WARNING_OPTIONS = { ...DEFAULT_OPTIONS, icon: "exclamation-circle" };
const DEFAULT_ERROR_OPTIONS = { ...DEFAULT_OPTIONS, icon: "exclamation-circle" };

function remove(state, uid) {
    const index = state.findIndex(alert => alert.uid === uid);
    if (index >= 0) {
        state.splice(index, 1);
    }
}

function create({ name, error, payload, result, uid, type }, { area, dismissible, icon, link, renderer }) {
    return { name, error, payload, result, uid, type, renderer, icon, dismissible, area, link };
}

function clearDelay({ uid }) {
    clearTimeout(timers.get(uid));
}

function setDelay({ uid }, fn, timout) {
    timers.set(uid, setTimeout(fn, timout));
}

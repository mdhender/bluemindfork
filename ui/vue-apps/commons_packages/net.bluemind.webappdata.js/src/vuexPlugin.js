import snakeCase from "lodash.snakecase";
import { WebAppDataClient } from "@bluemind/webappdata.api";
import { adapt, toRemote } from "./adapter";
import Vue from "vue";

const userSession = window.bmcSessionInfos; // FIXME, but cant use inject("UserSession") ?
const api = new WebAppDataClient(userSession.sid, "webappdata:" + userSession.userId);

export default store => {
    const cache = new Map(); // appData
    const subscribedMutations = new Map(); // match `sync` properties given by consumers

    const privateMutations = new Map(); // used only by plugin, one per synced appData
    const moduleCache = new Map();

    store.subscribe((mutation, state) => {
        const subscription = subscribedMutations.get(mutation.type);
        if (subscription) {
            const key = subscription.key;
            const appData = { key, value: getValue(state, subscription) };
            const remote = toRemote(appData);
            cache.get(key) ? api.update(key, remote) : api.create(key, remote);
            cache.set(key, appData);
        }
    });

    store.subscribeModule({
        before: (path, module) => {
            path = Array.isArray(path) ? path : [path];
            before(path, module);
        },
        after: async (path, module) => {
            path = Array.isArray(path) ? path : [path];
            sync(path, module);
        }
    });

    async function sync(path, module) {
        const appData = parseSynced(module.state.synced);
        Object.entries(appData).forEach(async ([stateProperty]) => {
            const key = getKey(path, stateProperty);
            // useful to test reactivity
            // const delay = ms => new Promise(res => setTimeout(res, ms));
            // await delay(10000);
            const remote = await api.getByKey(key);
            if (remote) {
                const appData = adapt(remote);
                cache.set(key, appData);
                const type = privateMutations.get(key);
                const namespacedType = namespaceMutationType(type, path, moduleCache);
                store.commit(namespacedType, appData.value);
            }
        });
        if (module.modules) {
            Object.entries(module.modules).forEach(async ([moduleName, module]) => {
                await sync([...path, moduleName], module, store);
            });
        }
    }

    function before(path, module) {
        moduleCache.set(path[path.length - 1], module);
        const appData = parseSynced(module.state.synced);
        Object.entries(appData).forEach(async ([stateProperty, mutations]) => {
            const key = getKey(path, stateProperty);
            mutations.forEach(mutation => {
                addSubscribedMutation(module, mutation, stateProperty, key, path);
            });
            addPrivateMutation(module, stateProperty, key);
        });
        if (module.modules) {
            Object.entries(module.modules).forEach(async ([moduleName, module]) => {
                before([...path, moduleName], module);
            });
        }
    }

    function addPrivateMutation(module, stateProperty, key) {
        const type = privateMutationType(stateProperty);
        addMutation(module, type, stateProperty);
        privateMutations.set(key, type);
    }

    function addSubscribedMutation(module, mutation, stateProperty, key, path) {
        const type = namespaceMutationType(mutation, path, moduleCache);
        subscribedMutations.set(type, { key, path, stateProperty });
        const defaultType = defaultMutationType(stateProperty);
        if (mutation === defaultType && (!module.mutations || !module.mutations[defaultType])) {
            addMutation(module, defaultType, stateProperty);
        }
    }
};

const KEY_SEPARATOR = ":";
// INTERNAL METHOD (exported only for testing purpose)
export function getKey(path, stateProperty) {
    return [...path, stateProperty].map(snakeCase).join(KEY_SEPARATOR);
}

// INTERNAL METHOD (exported only for testing purpose)
export function privateMutationType(stateProperty) {
    return "$_AppData_VuexPlugin_" + defaultMutationType(stateProperty);
}

// INTERNAL METHOD (exported only for testing purpose)
export function defaultMutationType(stateProperty) {
    const normalizedStateProperty = snakeCase(stateProperty).toUpperCase();
    return "SET_" + normalizedStateProperty;
}

function namespaceMutationType(mutationType, path, moduleCache) {
    let prefix = "";
    path.forEach(moduleName => {
        if (moduleCache.get(moduleName).namespaced) {
            prefix += moduleName + "/";
        }
    });
    return prefix + mutationType;
}

function parseSynced(synced) {
    const result = {};
    if (Array.isArray(synced)) {
        synced.forEach(stateProperty => {
            result[stateProperty] = [defaultMutationType(stateProperty)];
        });
    } else if (typeof synced === "object") {
        Object.entries(synced).forEach(([stateProperty, mutations]) => {
            const mutationArray = Array.isArray(mutations) ? mutations : [mutations];
            result[stateProperty] = mutationArray;
        });
    }
    return result;
}

function addMutation(module, type, property) {
    const mutation = (state, value) => {
        if (Array.isArray(value)) {
            state[property].splice(0, state[property].length, ...value);
        } else if (typeof value === "object") {
            //FIXME: supports only flat object
            Object.entries(value).forEach(([key, value]) => {
                Vue.set(state[property], key, value);
            });
        } else {
            state[property] = value;
        }
    };
    module.mutations = module.mutations ? module.mutations : {};
    module.mutations[type] = mutation;
}

function getValue(state, { path, stateProperty }) {
    let tmpState = state;
    path.forEach(module => {
        tmpState = tmpState[module];
    });
    return tmpState[stateProperty];
}

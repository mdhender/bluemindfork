import snakeCase from "lodash.snakecase";
import { WebAppDataClient } from "@bluemind/webappdata.api";
import { adapt, toRemote } from "./adapter";

const userSession = window.bmcSessionInfos; // FIXME, but cant use inject("UserSession") ?
const api = new WebAppDataClient(userSession.sid, "webappdata:" + userSession.userId);

export default store => {
    const cache = new Map(); // appData
    const subscribedMutations = new Map(); // match `sync` properties given by consumers

    const privateMutations = new Map(); // used only by plugin, one per synced appData
    const moduleCache = new Map();

    store.subscribe(async (mutation, state) => {
        const subscription = subscribedMutations.get(mutation.type);
        if (subscription) {
            const key = subscription.key;
            const value = getValue(state, subscription);
            const remote = toRemote({ key, value });
            cache.get(key) ? api.update(key, remote) : api.create(key, remote);
        }
    });

    store.subscribeModule({
        before: (path, module) => {
            path = Array.isArray ? path : [path];
            $_AppData_VuexPlugin_before(path, module);
        },
        after: async (path, module) => {
            path = Array.isArray ? path : [path];
            $_AppData_VuexPlugin_sync(path, module);
        }
    });

    async function $_AppData_VuexPlugin_sync(path, module) {
        const appData = parseSynced(module.state.synced);
        Object.entries(appData).forEach(async ([stateProperty]) => {
            const key = getKey(path, stateProperty);
            const remote = await api.getByKey(key);
            if (remote) {
                const appData = adapt(remote);
                cache.set(key, appData);
                const privateMutation = privateMutations.get(key).type;
                const namespacedType = namespaceMutationType(privateMutation, path, moduleCache);
                store.commit(namespacedType, appData.value);
            }
        });
        if (module.modules) {
            Object.entries(module.modules).forEach(async ([moduleName, module]) => {
                await $_AppData_VuexPlugin_sync([...path, moduleName], module, store);
            });
        }
    }

    function $_AppData_VuexPlugin_before(path, module) {
        moduleCache.set(path[path.length - 1], module);
        const appData = parseSynced(module.state.synced);
        Object.entries(appData).forEach(async ([stateProperty, mutations]) => {
            const key = getKey(path, stateProperty);
            mutations.forEach(mutation => {
                $_AppData_VuexPlugin_addSubscribedMutation(module, mutation, stateProperty, key, path);
            });
            $_AppData_VuexPlugin_addPrivateMutation(module, stateProperty, key);
        });
        if (module.modules) {
            Object.entries(module.modules).forEach(async ([moduleName, module]) => {
                $_AppData_VuexPlugin_before([...path, moduleName], module);
            });
        }
    }

    function $_AppData_VuexPlugin_addPrivateMutation(module, stateProperty, key) {
        const type = privateMutationType(stateProperty);
        const privateMutation = (state, appDataValue) => {
            state[stateProperty] = appDataValue;
        };
        privateMutations.set(key, { type, mutation: privateMutation });
        addMutation(module, type, privateMutation);
    }

    function $_AppData_VuexPlugin_addSubscribedMutation(module, mutation, stateProperty, key, path) {
        const type = namespaceMutationType(mutation, path, moduleCache);
        console.log("gonna subscribe mutation ", type, " for appData ", stateProperty);
        subscribedMutations.set(type, { key, path, stateProperty });
        const defaultType = defaultMutationType(stateProperty);
        if (mutation === defaultType && !module.mutations[defaultType]) {
            const defaultMutation = (state, value) => {
                state[stateProperty] = value;
            };
            addMutation(module, defaultType, defaultMutation);
        }
    }
};

const KEY_SEPARATOR = ":";
function getKey(path, stateProperty) {
    return [...path, stateProperty].map(snakeCase).join(KEY_SEPARATOR);
}

function privateMutationType(stateProperty) {
    return "$_AppData_VuexPlugin_" + defaultMutationType(stateProperty);
}

function defaultMutationType(stateProperty) {
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

function addMutation(module, type, mutation) {
    module.mutations = module.mutations ? module.mutations : {};
    module.mutations[type] = mutation;
}

function getValue(state, { path, stateProperty }) {
    let tmp = state;
    path.forEach(module => {
        tmp = tmp[module];
    });
    return tmp[stateProperty];
}

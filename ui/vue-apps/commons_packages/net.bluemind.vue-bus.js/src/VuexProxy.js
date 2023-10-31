const PREFIX = "$_vuebus_";
const METHOD_PATTERN = /[\W\s_]/g;

export default {
    install(VueBus, store) {
        this.start(new VueBus.Client(), store);
    },

    start(bus, store) {
        _bus = bus;
        _store = store;
        _bus.$on("*", dispatch);
    },

    stop() {
        _bus.$off("*", dispatch);
        _store = _bus = undefined;
    }
};

let _bus, _store;

function dispatch(event, payload) {
    const name = event.replace(METHOD_PATTERN, "").toLowerCase();

    const mutationMatch = anyMatch(_store._mutations, name);
    if (mutationMatch) {
        _store.commit(mutationMatch, payload);
    }

    const actionMatch = anyMatch(_store._actions, name);
    if (actionMatch) {
        _store.dispatch(actionMatch, payload);
    }
}

function anyMatch(obj, name) {
    for (const fullName in obj) {
        const endPoint = fullName.split("/").pop().toLowerCase();

        if (endPoint.startsWith(PREFIX) && name === endPoint.replace(PREFIX, "").replace(METHOD_PATTERN, "")) {
            return fullName;
        }
    }
    return false;
}

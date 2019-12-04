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
    const sendToStore = (rules, storeCallback) => {
        for (const fullName in rules) {
            const endPoint = fullName.split("/").pop().toLowerCase();
            if (endPoint.startsWith(PREFIX)) {
                if (name === endPoint.replace(PREFIX, "").replace(METHOD_PATTERN, "")) {
                    storeCallback(fullName, payload);
                }
            }
        }
    };

    sendToStore(_store._mutations, (fullName, payload) => {
        _store.commit(fullName, payload);
    });

    sendToStore(_store._actions, (fullName, payload) => {
        _store.dispatch(fullName, payload);
    });
}

export function extend(router, store) {
    router.beforeEach((to, from, next) => {
        executeLeaveGuards(to, from, store)
            .then(() => executeParamsLeaveGuards(to, from, store))
            .then(() => executeEnterGuards(to, from, store))
            .then(() => executeEachGuards(to, from, store))
            .then(() => executeParamsEnterGuards(to, from, store))
            .then(() => next());
    });
}

function executeLeaveGuards() {
    return Promise.resolve();
}

function executeEnterGuards() {
    return Promise.resolve();
}

function executeEachGuards() {
    return Promise.resolve();
}

function executeParamsLeaveGuards(to, from, store) {
    return executeParamsGuards(from, to, store, false);
}

function executeParamsEnterGuards(to, from, store) {
    return executeParamsGuards(to, from, store, true);
}

function executeParamsGuards(to, from, store, isBefore) {
    let promise = Promise.resolve();
    if (to.meta && to.meta.$actions) {
        for (const parameter in to.meta.$actions) {
            const action = normalize(to.meta.$actions[parameter]);
            if (action.isBefore == isBefore && (from.params[parameter] != to.params[parameter] || action.force))
                promise = promise.then(() =>
                    action.call(store, to.params[parameter], from.params[parameter], to, from)
                );
        }
    }
}

function normalize(action) {
    const defaults = { call: undefined, force: false, isBefore: true };
    if (typeof action == "function") {
        action = { call: action };
    }
    return Object.assign(defaults, action);
}

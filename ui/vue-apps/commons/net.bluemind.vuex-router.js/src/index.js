export function extend(router, store) {
    router.beforeEach(executeActions.bind(null, store));
}

async function executeActions(store, to, from, next) {
    await executeLeaveActions(to, from, store);
    await executeEnterActions(to, from, store);
    await executeEachActions(to, from, store);
    await executeParamsActions(to, from, store);
    next();
}

async function executeEnterActions(to, from, store) {
    for (const route of to.matched) {
        if (!!route.meta.onEnter && !includes(from, route.path)) {
            await route.meta.onEnter(store, to, from);
        }
    }
}

async function executeEachActions(to, from, store) {
    for (const route of to.matched) {
        if (route.meta.onUpdate) {
            await route.meta.onUpdate(store, to, from);
        }
    }
}

async function executeLeaveActions(to, from, store) {
    for (const route of from.matched) {
        if (!!route.meta.onLeave && !includes(to, route.path)) {
            await route.meta.onLeave(store, to, from);
        }
    }
}

async function executeParamsActions(to, from, store) {
    for (const route of to.matched) {
        await executeRouteParamsActions(route, to, from, store);
    }
}

async function executeRouteParamsActions(route, to, from, store) {
    if (route.meta.watch) {
        for (const parameter in route.meta.watch) {
            const action = route.meta.watch[parameter];
            await executeParamAction(action, parameter, to, from, store);
        }
    }
}

async function executeParamAction(action, parameter, to, from, store) {
    if (from.params[parameter] !== to.params[parameter])
        await action(store, to.params[parameter], from.params[parameter], to, from);
}

function includes(route, path) {
    return route.matched.findIndex(match => match.path === path) >= 0;
}

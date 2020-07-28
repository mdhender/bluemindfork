export function extend(router, store) {
    router.beforeEach(executeActions.bind(null, store));
}

async function executeActions(store, to, from, next) {
    for (const route of from.matched) {
        await executeLeaveActions(route, to, from, store);
    }
    for (const route of to.matched) {
        await executeEnterActions(route, to, from, store);
        await executeEachActions(route, to, from, store);
        await executeParamsActions(route, to, from, store);
    }
    next();
}

async function executeEnterActions(route, to, from, store) {
    if (!!route.meta.onEnter && !includes(from, route.path)) {
        await route.meta.onEnter(store, to, from);
    }
}

async function executeEachActions(route, to, from, store) {
    if (route.meta.onUpdate) {
        await route.meta.onUpdate(store, to, from);
    }
}

async function executeLeaveActions(route, to, from, store) {
    if (!!route.meta.onLeave && !includes(to, route.path)) {
        await route.meta.onLeave(store, to, from);
    }
}

async function executeParamsActions(route, to, from, store) {
    if (route.meta.watch) {
        for (const parameter in route.meta.watch) {
            const action = route.meta.watch[parameter];
            await executeParamAction(action, parameter, to, from, store);
        }
    }
}

async function executeParamAction(action, parameter, to, from, store) {
    if (!(parameter in from.params) || from.params[parameter] !== to.params[parameter])
        await action(store, to.params[parameter], from.params[parameter], to, from);
}

function includes(route, path) {
    return route.matched.findIndex(match => match.path === path) >= 0;
}

export function extend(router, store) {
    router.beforeEach((to, from, next) => {
        let promise = { then: fn => fn() };
        if (to.meta && to.meta.$actions) {
            for (const parameter in to.meta.$actions) {
                if (from.params[parameter] != to.params[parameter]) {
                    promise = promise.then(() =>
                        to.meta.$actions[parameter](store, to.params[parameter], from.params[parameter], to, from)
                    );
                }
            }
        }
        promise.then(() => next());
    });
}

export default {
    extends(VueRouter) {
        const push = VueRouter.prototype.push;
        VueRouter.prototype.push = function (location, onResolve, onReject) {
            if (onResolve || onReject) return push.call(this, location, onResolve, onReject);
            return push.call(this, location).catch(err => {
                if (!isSilentNavigationFailure(err, VueRouter)) {
                    throw err;
                }
            });
        };
    }
};

function isSilentNavigationFailure(error, vueRouter) {
    const { isNavigationFailure, NavigationFailureType } = vueRouter;
    return (
        isNavigationFailure(error, NavigationFailureType.cancelled) ||
        isNavigationFailure(error, NavigationFailureType.duplicated)
    );
}

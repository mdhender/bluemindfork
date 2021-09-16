export default {
    extends(VueRouter) {
        const push = VueRouter.prototype.push;
        VueRouter.prototype.push = function (location, onResolve, onReject) {
            if (onResolve || onReject) return push.call(this, location, onResolve, onReject);
            return push.call(this, location).catch(err => {
                if (err.name !== "NavigationDuplicated") {
                    throw err;
                }
            });
        };
    }
};

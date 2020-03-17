export default {
    extends(VueRouter) {
        if (!VueRouter.prototype.navigate) {
            VueRouter.prototype.navigate = function(to, from) {
                this.push(this.relative(to, from));
            };
        }
        if (!VueRouter.prototype.relative) {
            VueRouter.prototype.relative = function(to, from) {
                let { name, params, hash, query } = from || this.currentRoute;
                const location = normalize(to, { name, params, hash, query });
                return Object.assign({}, { name, params, hash, query }, location);
            };
        }
    }
};

function normalize(raw, current) {
    let location = typeof raw === "string" ? { name: raw } : raw;
    location.params = Object.assign({}, current.params, location.params);
    if (location.name && !location.query) {
        location.query = {};
    }
    if (location.name && !location.hash) {
        location.hash = "";
    }
    return location;
}

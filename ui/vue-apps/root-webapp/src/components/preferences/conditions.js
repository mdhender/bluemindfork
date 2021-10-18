import { inject } from "@bluemind/inject";
import isEqual from "lodash.isequal";

export const RoleCondition = function () {
    return RoleCondition.some.apply(null, arguments);
};
RoleCondition.every = function () {
    return checkRole(Array.from(arguments), Array.prototype.every);
};
RoleCondition.some = function () {
    return checkRole(Array.from(arguments), Array.prototype.some);
};
RoleCondition.none = function () {
    return !RoleCondition.or.apply(null, arguments);
};

function checkRole(required, assert) {
    const roles = inject("UserSession").roles;
    return assert.call(required, role => new RegExp(`\\b${role}\\b`, "i").test(roles));
}

export const StoreFieldCondition = function () {
    return StoreFieldCondition.current.apply(null, arguments);
};

StoreFieldCondition.current = function (id, value) {
    return vm => isEqual(vm.$store.state.preferences.fields[id]?.current?.value, value);
};

StoreFieldCondition.saved = function (id, value) {
    return vm => isEqual(vm.$store.state.preferences.fields[id]?.saved?.value, value);
};

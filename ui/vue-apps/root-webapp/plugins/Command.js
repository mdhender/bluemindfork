import camelize from "lodash.camelcase";
import isPlainObject from "lodash.isplainobject";
import { mapExtensions } from "@bluemind/extensions";
import { inject } from "@bluemind/inject";

export default {
    install(Vue) {
        const beforeHooks = new Map();
        const afterHooks = new Map();
        const roles = inject("UserSession").roles.split(",");
        mapExtensions("webapp", ["command"])?.command?.forEach(({ fn, name, role, after }) => {
            if (!role || roles.includes(role)) {
                let hooks = after ? afterHooks : beforeHooks;
                name = camelize(name);
                const value = hooks.get(name) || [];
                value.push(fn);
                hooks.set(name, value);
            }
        });

        Vue.prototype.$execute = async function (name, payload = {}) {
            name = camelize(name);
            if (this.$options.commands[name]) {
                try {
                    await executeHooks.call(this, beforeHooks.get(name), payload);
                    this.$options.commands[name].call(this, payload);
                    await executeHooks.call(this, afterHooks.get(name), payload);
                } catch (error) {
                    if (error.name === "StopExecution") {
                        return;
                    }
                }
            }
        };
    }
};

async function executeHooks(hooks, payload) {
    const isPlain = isPlainObject(payload);
    for (const fn of hooks || []) {
        const values = await fn.call(this, payload);
        if (isPlain) {
            Object.assign(payload, values);
        } else if (values !== undefined) {
            payload = values;
        }
    }
}

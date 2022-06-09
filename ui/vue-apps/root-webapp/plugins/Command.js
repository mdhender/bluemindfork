import camelize from "lodash.camelcase";
import isPlainObject from "lodash.isplainobject";
import { mapExtensions } from "@bluemind/extensions";
import { inject } from "@bluemind/inject";

export default {
    install(Vue) {
        const roles = inject("UserSession").roles.split(",");

        Vue.prototype.$execute = async function (command, payload = {}) {
            command = camelize(command);
            if (this.$options.commands[command]) {
                const beforeHooks = [];
                const afterHooks = [];
                mapExtensions("webapp", ["command"])?.command?.forEach(({ fn, name, role, after }) => {
                    name = camelize(name);
                    if (command === name && (!role || roles.includes(role))) {
                        let hooks = after ? afterHooks : beforeHooks;
                        hooks.push(fn);
                    }
                });
                try {
                    await executeHooks.call(this, beforeHooks, payload);
                    this.$options.commands[command].call(this, payload);
                    await executeHooks.call(this, afterHooks, payload);
                } catch {
                    return;
                }
            }
        };
    }
};

async function executeHooks(hooks, payload) {
    const isPlain = isPlainObject(payload);
    for (const fn of hooks || []) {
        try {
            const values = await fn.call(this, payload);
            if (isPlain) {
                Object.assign(payload, values);
            } else if (values !== undefined) {
                payload = values;
            }
        } catch (error) {
            if (error.name === "StopExecution") {
                throw error;
            }
        }
    }
}

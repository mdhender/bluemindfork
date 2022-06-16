import camelize from "lodash.camelcase";
import isPlainObject from "lodash.isplainobject";
import { mapExtensions } from "@bluemind/extensions";
import { inject } from "@bluemind/inject";

export default {
    install(Vue) {
        const roles = inject("UserSession").roles.split(",");
        Vue.mixin({
            beforeCreate() {
                this._commands = {};
                for (let name in this.$parent?._commands) {
                    this._commands[name] = this.$parent._commands[name];
                }
                for (let name in this.$options.commands) {
                    this._commands[name] = this.$options.commands[name].bind(this);
                }
            }
        });
        Vue.prototype.$execute = async function (command, payload = {}, options = {}) {
            command = camelize(command);
            if (this._commands[command]) {
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
                    await executeHooks.call(this, beforeHooks, payload, options);
                    this._commands[command].call(this, payload), options;
                    await executeHooks.call(this, afterHooks, payload, options);
                } catch {
                    return;
                }
            }
        };
    }
};

async function executeHooks(hooks, payload, options) {
    const isPlain = isPlainObject(payload);
    for (const fn of hooks || []) {
        try {
            const values = await fn.call(this, payload, options);
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

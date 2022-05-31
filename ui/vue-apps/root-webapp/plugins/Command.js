import camelize from "lodash.camelcase";
import isPlainObject from "lodash.isplainobject";
import { mapExtensions } from "@bluemind/extensions";
import { inject } from "@bluemind/inject";

export default {
    install(Vue) {
        const extensions = new Map();
        const roles = inject("UserSession").roles.split(",");
        mapExtensions("webapp", ["command"])?.command?.forEach(({ fn, name, role }) => {
            if (!role || roles.includes(role)) {
                name = camelize(name);
                const value = extensions.get(name) || [];
                value.push(fn);
                extensions.set(name, value);
            }
        });

        Vue.prototype.$execute = async function (name, payload = {}) {
            name = camelize(name);
            if (this.$options.commands[name]) {
                const isPlain = isPlainObject(payload);
                for (const fn of extensions.get(name) || []) {
                    try {
                        const values = await fn.call(this, payload);
                        if (isPlain) {
                            Object.assign(payload, values);
                        } else if (values !== undefined) {
                            payload = values;
                        }
                    } catch (error) {
                        if (error.name === "StopExecution") {
                            return;
                        }
                    }
                }
                this.$options.commands[name].call(this, payload);
            }
        };
    }
};

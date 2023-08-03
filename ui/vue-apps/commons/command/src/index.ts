import camelize from "lodash.camelcase";
import isPlainObject from "lodash.isplainobject";
import { mapExtensions } from "@bluemind/extensions";
import { inject as bmInject } from "@bluemind/inject";
import { inject, provide } from "vue";
import { CommandFn, CommandRegistry, Options, Payload } from "../types";
import { VueConstructor } from "vue/types/umd";

/** Global mixin, autoregister commands mixins and add $execute to run commands */
export default {
    install(Vue: VueConstructor) {
        Vue.mixin({
            inject: { commandRegistry: { default: defaultRegistry } },
            created() {
                const self = this as Vue;
                for (const name in self.$options.commands) {
                    if (!self.commandRegistry?.[name]) {
                        self.commandRegistry[name] = self.$options.commands[name];
                    }
                }
            }
        });
        Vue.prototype.$execute = function (command: string, payload: Payload, options: Options) {
            return execute.call(this, this.commandRegistry, command, payload, options);
        };
    }
};

/** Composable, register a command and return a callback to run it */
export function useCommand(name: string, commandFn?: CommandFn) {
    name = camelize(name);
    const commandRegistry: CommandRegistry = inject("commandRegistry", defaultRegistry);
    if (commandFn && !commandRegistry[name]) {
        commandRegistry[name] = commandFn;
    }
    return function (this: void, payload?: Payload, options?: Options) {
        return execute.call(this, commandRegistry, name, payload, options);
    };
}

/** Composable, allow execution of already registered commands */
export function useExecuteCommand() {
    const commandRegistry: CommandRegistry = inject("commandRegistry", defaultRegistry);
    return function (this: void, name: string, payload: Payload, options: Options) {
        return execute.call(this, commandRegistry, name, payload, options);
    };
}

async function execute(
    this: void,
    commandRegistry: Record<string, CommandFn>,
    command: string,
    payload: Payload = {},
    options: Options = {}
) {
    const roles = bmInject("UserSession").roles.split(",");

    if (!commandRegistry) {
        throw new Error(
            "Command Registry does not exist yet, the execute command was called too early. Please refactor your code to run after the 'created' lifecycle"
        );
    }
    command = camelize(command);
    if (commandRegistry[command]) {
        const beforeHooks: CommandFn[] = [];
        const afterHooks: CommandFn[] = [];
        mapExtensions("webapp", ["command"])?.command?.forEach(
            ({ fn, name, role, after }: { fn: CommandFn; name: string; role: string; after: boolean }) => {
                name = camelize(name);
                if (command === name && (!role || roles.includes(role))) {
                    const hooks = after ? afterHooks : beforeHooks;
                    hooks.push(fn);
                }
            }
        );
        try {
            await executeHooks.call(this, [...beforeHooks, commandRegistry[command], ...afterHooks], payload, options);
        } catch {
            return;
        }
    }
}

async function executeHooks(this: void, hooks: CommandFn[] = [], payload: Payload, options: Options) {
    const isPlain = isPlainObject(payload);
    for (const fn of hooks) {
        try {
            const values = await fn.call(this, payload, options);
            if (isPlain) {
                Object.assign(payload, values);
            } else if (values !== undefined) {
                payload = values;
            }
        } catch (error: unknown) {
            if (error instanceof Error && error.name === "StopExecution") {
                throw error;
            }
        }
    }
}

export function useCommandRegistryProvider() {
    const registry = {};
    provide("commandRegistry", registry);
}

const defaultRegistry = {};

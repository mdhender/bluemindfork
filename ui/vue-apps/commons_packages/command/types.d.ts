// eslint-disable-next-line @typescript-eslint/no-unused-vars
import Vue from "vue";

declare module "vue/types/vue" {
    interface Vue {
        commandRegistry: CommandRegistry;
    }
}

declare module "vue/types/options" {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    interface ComponentOptions<V extends Vue> {
        commands?: Record<string, CommandFn>;
    }
}

export type Payload = Record<string, unknown>;
export type Options = Record<string, unknown>;
export type CommandFn = (payload?: Payload, options?: Options) => Payload;
export type CommandRegistry = Record<string, CommandFn>;

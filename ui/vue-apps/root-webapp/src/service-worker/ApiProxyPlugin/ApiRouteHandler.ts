import { EndPointMetadata } from "@bluemind/api.commons";
import session from "../session";
import { ApiEndPointClass, ExecutionParameters, IApiProxy } from "./types";

export class ApiRouteHandler {
    client: ApiEndPointClass;
    next: ApiRouteHandler | null;
    priority: number;
    metadatas: EndPointMetadata.MethodMetadata;
    role: string | undefined;

    constructor(client: ApiEndPointClass, metadatas: EndPointMetadata.MethodMetadata, priority: number, role?: string) {
        this.client = client;
        this.metadatas = metadatas;
        this.priority = priority;
        this.role = role;
        this.next = null;
    }
    chain(handler: ApiRouteHandler | null): ApiRouteHandler {
        if (handler && handler.priority > this.priority) {
            return handler.chain(this);
        } else if (handler) {
            this.next = handler.chain(this.next);
        }
        return this;
    }
    async execute(
        parameters: ExecutionParameters,
        event: ExtendableEvent,
        ...overwrite: Array<unknown>
    ): Promise<unknown> {
        parameters = overwrite.length > 0 ? { ...parameters, method: overwrite } : parameters;
        const next = await this.#generateNext(parameters, event, ...overwrite);
        if (await this.#hasRole()) {
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            const client: any = new this.client(await session.sid, ...parameters.client);
            client.event = event;
            client.next = next;
            return client[this.metadatas.name](...parameters.method);
        } else {
            return next();
        }
    }

    async #generateNext(parameters: ExecutionParameters, event: ExtendableEvent, ...overwrite: Array<unknown>) {
        if (this.next) {
            return this.next.execute.bind(this.next, parameters, event);
        } else {
            const next: any = RootApiClientFactory.create(this.client, await session.sid, ...parameters.client);
            const args = overwrite.length > 0 ? overwrite : parameters.method;
            return next[this.metadatas.name].bind(next, ...args);
        }
    }

    async #hasRole() {
        const userRoles = await session.roles;
        return !this.role || userRoles.includes(this.role);
    }
}

const RootApiClientFactory = {
    create(client: ApiEndPointClass, sid: string, ...parameters: Array<string>): IApiProxy {
        while (Object.getPrototypeOf(client).prototype?.getMetadata) {
            client = Object.getPrototypeOf(client);
        }
        return new client(sid, ...parameters);
    }
};

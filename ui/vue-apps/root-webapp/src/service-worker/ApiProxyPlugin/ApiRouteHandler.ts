import { EndPointMetadata } from "@bluemind/api.commons";
import session from "../session";
import { ApiEndPointClass, ExecutionParameters, IApiProxy } from "./types";

export class ApiRouteHandler {
    client: ApiEndPointClass;
    next: ApiRouteHandler | null;
    priority: number;
    metadatas: EndPointMetadata.MethodMetadata;
    constructor(client: ApiEndPointClass, metadatas: EndPointMetadata.MethodMetadata, priority: number) {
        this.client = client;
        this.metadatas = metadatas;
        this.priority = priority;
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
    async execute(parameters: ExecutionParameters, ...overwrite: Array<unknown>): Promise<unknown> {
        parameters = overwrite.length > 0 ? { ...parameters, method: overwrite } : parameters;
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const client: any = new this.client(await session.sid, ...parameters.client);
        if (this.next) {
            client.next = this.next.execute.bind(this.next, parameters);
        } else {
            const next: any = RootApiClientFactory.create(this.client, await session.sid, ...parameters.client);
            const args = overwrite.length > 0 ? overwrite : parameters.method;
            client.next = next[this.metadatas.name].bind(next, ...args);
        }
        return await client[this.metadatas.name](...parameters.method);
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

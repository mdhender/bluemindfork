import session from "../session";
import { APIClient, ExecutionParameters, MethodMetadatas } from "./types";

export class ApiHandler {
    client: typeof APIClient;
    next: ApiHandler | null;
    priority: number;
    metadatas: MethodMetadatas;
    constructor(client: typeof APIClient, metadatas: MethodMetadatas, priority: number) {
        this.client = client;
        this.metadatas = metadatas;
        this.priority = priority;
        this.next = null;
    }
    chain(handler: ApiHandler | null): ApiHandler {
        if (handler && handler.priority > this.priority) {
            return handler.chain(this);
        } else if (handler) {
            this.next = handler.chain(this.next);
        }
        return this;
    }
    async execute(parameters: ExecutionParameters, ...overwrite: Array<any>): Promise<any> {
        parameters = overwrite.length > 0 ? { ...parameters, method: overwrite } : parameters;
        const client: any = new this.client(...parameters.client, await session.sid);
        if (this.next) {
            client.next = this.next.execute.bind(this.next, parameters);
        } else {
            client.next = async (...overwrite: Array<any>) => {
                const client: any = RootApiClientFactory.create(this.client, ...parameters.client, await session.sid);
                const args = overwrite.length > 0 ? overwrite : parameters.method;
                return await client[this.metadatas.name](...args);
            };
        }
        return await client[this.metadatas.name](...parameters.method);
    }
}

const RootApiClientFactory = {
    create(client: typeof APIClient, ...parameters: Array<any>): APIClient {
        while (Object.getPrototypeOf(client).prototype?.getMetadatas) {
            client = Object.getPrototypeOf(client);
        }
        return new client(...parameters);
    }
};

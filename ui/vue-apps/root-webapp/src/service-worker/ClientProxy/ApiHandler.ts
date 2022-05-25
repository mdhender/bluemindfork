import session from "../session";
import { APIClient, ExecutionParameters, MethodMetadatas } from "./types";
import { UnhandledRequestError } from "./UnhandedRequestError";

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
        parameters = overwrite && overwrite.length > 0 ? { ...parameters, method: overwrite } : parameters;
        const client: any = new this.client(...parameters.client, await session.sid);

        if (this.next) {
            client.next = this.next.execute.bind(this.next, parameters);
        } else {
            client.next = () => Promise.reject(new UnhandledRequestError());
        }
        return await client[this.metadatas.name](...parameters.method);
    }
}

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
    async execute(parameters: ExecutionParameters, request: Request, ...overwrite: Array<any>): Promise<any> {
        parameters = overwrite.length > 0 ? { ...parameters, method: overwrite } : parameters;
        const client: any = new this.client(await session.sid, ...parameters.client);
        if (this.next) {
            client.next = this.next.execute.bind(this.next, parameters, request);
        } else {
            // FIXME FEATWEBML-2079: Need the BM client to be upgrader with a version using fetch instead of axios
            client.next = () => fetch(request);
            // const client: any = RootApiClientFactory.create(this.client, await session.sid, ...parameters.client);
            // const args = overwrite.length > 0 ? overwrite : parameters.method;
            // return client[this.metadatas.name](...args);
        }
        return await client[this.metadatas.name](...parameters.method);
    }
}

// const RootApiClientFactory = {
//     create(client: typeof APIClient, ...parameters: Array<any>): APIClient {
//         while (Object.getPrototypeOf(client).prototype?.getMetadatas) {
//             client = Object.getPrototypeOf(client);
//         }
//         // FIXME: To Remove
//         client = Object.getPrototypeOf(client);

//         return new client(...parameters);
//     }
// };

import { ApiEndPoint } from "@bluemind/api.commons";

export type ExecutionParameters = {
    client: Array<string>;
    method: Array<unknown>;
};

//
export interface GenericApiClient {
    [key: string]: (...args: Array<unknown>) => Promise<unknown>;
}

export type ApiEndPointClass = new (sid: string, ...args: Array<string>) => IApiProxy;

// FIXME: This type need to be exported (plugin proxy class should implement this)
export interface IApiProxy extends ApiEndPoint {
    next?: (...args: Array<unknown>) => Promise<unknown>;
}

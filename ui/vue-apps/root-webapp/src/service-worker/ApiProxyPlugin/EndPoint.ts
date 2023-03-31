import { RegExpRoute, Route } from "workbox-routing";
import { RouteHandlerCallbackOptions } from "workbox-core";

import { ApiRouteHandler } from "./ApiRouteHandler";
import { ApiEndPointClass, ExecutionParameters } from "./types";
import { EndPointMetadata } from "@bluemind/api.commons";
export class EndPoint {
    static key(method: EndPointMetadata.MethodMetadata, endpoint: EndPointMetadata): string {
        return `${endpoint.packageName}.${endpoint.className}#${method.name}`;
    }

    endpoint: EndPointMetadata;
    metadatas: EndPointMetadata.MethodMetadata;
    handler: ApiRouteHandler | null;
    url: string;
    regExp: RegExp;

    constructor(metadatas: EndPointMetadata.MethodMetadata, endpoint: EndPointMetadata) {
        this.endpoint = endpoint;
        this.metadatas = metadatas;
        this.handler = null;
        this.url = `/api${[endpoint.path.value, metadatas.path.value].filter(Boolean).join("/")}`;
        const query = this.metadatas.inParams.some(({ paramType }) => paramType === "QueryParam") ? "(\\?.*)?" : "";
        this.regExp = new RegExp(`${this.url.replace(/{[^}]+}/g, "([^_/][^/?]*)")}${query}$`);
    }

    priority(): number {
        const isVar = /^{.*}$/;
        return this.url
            .split("/")
            .slice(0, 8)
            .reduce((priority, part, index) => (priority += (isVar.test(part) ? 0 : 1) << (8 - index)), 0);
    }
    route(): Route {
        return new RegExpRoute(this.regExp, this.handle.bind(this), this.metadatas.verb);
    }

    chain(client: ApiEndPointClass, priority: number, role?: string) {
        this.handler = new ApiRouteHandler(client, this.metadatas, priority, role).chain(this.handler);
    }
    async handle({ request, params, event }: RouteHandlerCallbackOptions): Promise<Response> {
        if (this.handler) {
            const pathParams: string[] = Array.isArray(params) ? params : [];
            try {
                const params = await this.parse(request.clone(), pathParams);
                const result = await this.handler.execute(params, event);
                return this.reply(result);
            } catch (e) {
                return this.replyError(e);
            }
        }
        return fetch(request);
    }

    async parse(request: Request, params: string[]): Promise<ExecutionParameters> {
        const query = new URL(request.url).searchParams;
        const result: ExecutionParameters = { client: [], method: [] };
        result.client = this.endpoint.path.parameters.map(() => params.shift() as string);
        for (const input of this.metadatas.inParams) {
            switch (input.paramType) {
                case "PathParam":
                    result.method.push(params.shift());
                    break;
                case "Body":
                    if (isStream(input.type)) {
                        result.method.push(await request.text());
                    } else {
                        result.method.push(await request.json());
                    }
                    break;
                case "QueryParam":
                    result.method.push(query.get(input.name));
                    break;
            }
        }
        return result;
    }

    reply(result: Response | Blob | unknown): Response {
        if (result instanceof Response) {
            return result;
        } else {
            const produce =
                this.metadatas.produce ||
                (isStream(this.metadatas.outParam)
                    ? (result as Blob).type || "application/octet-stream"
                    : "application/json");
            const value = isStream(this.metadatas.outParam) ? (result as Blob) : JSON.stringify(result);
            return new Response(value, {
                status: 200,
                headers: { "Content-Type": produce, "X-Bm-ServiceWorker": "true" }
            });
        }
    }

    replyError(reason: string | Error | unknown): Response {
        const error = { errorType: "ServiceWorkerProxyError", message: reason };
        return new Response(JSON.stringify(error), {
            status: 500,
            headers: { "Content-Type": "application/json", "X-Bm-ServiceWorker": "true" }
        });
    }
}

function isStream(type: EndPointMetadata.ParameterType) {
    return type.name === "Stream";
}

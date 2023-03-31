import { EndPointMetadata, ApiEndPoint } from "@bluemind/api.commons";
import { RegExpRoute, Route } from "workbox-routing";
import { EndPoint } from "./EndPoint";
import { ApiEndPointClass } from "./types";

type ApiRouteRegistry = {
    endpoints: Map<string, EndPoint>;
    register(ProxyClass: ApiEndPointClass, priority: number, role?: string): void;
    routes(): Array<Route>;
};

export const ApiRouteRegistry: ApiRouteRegistry = {
    endpoints: new Map(),
    register(proxyClass: ApiEndPointClass, priority: number, role?: string) {
        const proxy = new proxyClass("fake-sid");
        const methods = getProxifiedMethods(proxy);
        methods.forEach(method => {
            const key: string = EndPoint.key(method, proxy.getMetadata());
            if (!this.endpoints.has(key)) {
                this.endpoints.set(key, new EndPoint(method, proxy.getMetadata()));
            }
            this.endpoints.get(key)?.chain(proxyClass, priority, role);
        });
    },
    routes() {
        const sorted: Array<EndPoint> = [];
        this.endpoints.forEach(endpoint => {
            const index = sorted.findIndex(e => endpoint.priority() > e.priority());
            sorted.splice(index >= 0 ? index : sorted.length, 0, endpoint);
        });
        const routes: Array<RegExpRoute> = sorted.map(endpoint => endpoint.route());
        return routes;
    }
};

function getProxifiedMethods(proxy: ApiEndPoint): Array<EndPointMetadata.MethodMetadata> {
    const clientMetadatas: Array<EndPointMetadata.MethodMetadata> = proxy.getMetadata().methods;
    const methods: Set<EndPointMetadata.MethodMetadata> = new Set();
    do {
        const properties = Object.getOwnPropertyNames(proxy);
        if (properties.includes("getMetadata")) {
            break;
        }
        properties.forEach(name => {
            const methodMetadatas = clientMetadatas.find(metadatas => metadatas.name === name);
            if (methodMetadatas) {
                methods.add(methodMetadatas);
            }
        });
    } while ((proxy = Object.getPrototypeOf(proxy)));
    return [...methods];
}

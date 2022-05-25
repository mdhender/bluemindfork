import { RegExpRoute, Route } from "workbox-routing";
import { EndPoint } from "./EndPoint";
import { APIClient, MethodMetadatas } from "./types";

type ApiRouteRegistry = {
    endpoints: Map<String, EndPoint>;
    register(ProxyClass: typeof APIClient, priority: number): void;
    routes(): Array<Route>;
};

export const ApiRouteRegistry: ApiRouteRegistry = {
    endpoints: new Map(),
    register(ProxyClass, priority) {
        const proxy = new ProxyClass();
        const methods = getProxifiedMethods(proxy);
        methods.forEach(method => {
            const key: string = EndPoint.key(method, proxy.getMetadatas());
            if (!this.endpoints.has(key)) {
                this.endpoints.set(key, new EndPoint(method, proxy.getMetadatas()));
            }
            this.endpoints.get(key)!.chain(ProxyClass, priority);
        });
    },
    routes() {
        //TODO : sort
        const sorted: Array<EndPoint> = [];
        this.endpoints.forEach(endpoint => {
            const index = sorted.findIndex(e => endpoint.priority() > e.priority());
            sorted.splice(index >= 0 ? index : sorted.length, 0, endpoint);
        });
        const routes: Array<RegExpRoute> = sorted.map(endpoint => endpoint.route());
        return routes;
    }
};

function getProxifiedMethods(proxy: APIClient): Array<MethodMetadatas> {
    const clientMetadatas: Array<MethodMetadatas> = proxy.getMetadatas().methods;
    const methods: Set<MethodMetadatas> = new Set();
    do {
        Object.getOwnPropertyNames(proxy).forEach(name => {
            const methodMetadatas = clientMetadatas.find(metadatas => metadatas.name === name);
            if (methodMetadatas) {
                methods.add(methodMetadatas);
            }
        });
    } while ((proxy = Object.getPrototypeOf(proxy)) && proxy.getMetadatas);
    return [...methods];
}

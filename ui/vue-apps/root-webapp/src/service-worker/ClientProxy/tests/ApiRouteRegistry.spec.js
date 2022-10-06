import { ApiRouteRegistry } from "../ApiRouteRegistry";
import { EndPoint } from "../EndPoint";

jest.mock("../EndPoint");

class MockApiClient {}
MockApiClient.prototype.getMetadata = jest.fn(() => ({
    className: "IMock",
    packageName: "net.bluemind.mock.api",
    path: { value: "/service/", parameters: ["param"] },
    methods: [
        {
            path: { value: "{path}", parameters: ["path"] },
            verb: "GET",
            name: "first",
            outParam: { name: "String", parameters: [{ name: "String" }] },
            inParams: [{ type: { name: "Long" }, name: "query", paramType: "QueryParam" }]
        },
        {
            path: { value: "sub/{path}", parameters: ["path"] },
            verb: "GET",
            name: "third",
            outParam: { name: "String", parameters: [{ name: "String" }] },
            inParams: [{ type: { name: "Long" }, name: "query", paramType: "QueryParam" }]
        },
        {
            path: { value: "sub/_part", parameters: [] },
            verb: "GET",
            name: "second",
            outParam: { name: "String", parameters: [{ name: "String" }] },
            inParams: [{ type: { name: "Long" }, name: "query", paramType: "QueryParam" }]
        }
    ]
}));

describe("ApiRouteRegistry", () => {
    beforeEach(() => {
        ApiRouteRegistry.endpoints = new Map();
        MockApiClient.prototype.getMetadata.mockClear();
    });
    describe("register", () => {
        test("register add one endpoint per proxified method ", () => {
            const AProxy = class extends MockApiClient {
                first() {}
            };
            ApiRouteRegistry.register(AProxy, 0);
            expect(ApiRouteRegistry.endpoints.size).toBe(1);
            const AnotherProxy = class extends MockApiClient {
                first() {}
            };
            ApiRouteRegistry.register(AnotherProxy, 0);
            expect(ApiRouteRegistry.endpoints.size).toBe(1);
            const DifferentProxy = class extends MockApiClient {
                second() {}
            };
            EndPoint.key.mockReturnValueOnce("anotherKey");
            ApiRouteRegistry.register(DifferentProxy, 0);
            expect(ApiRouteRegistry.endpoints.size).toBe(2);
        });

        test("register only register client methods ", () => {
            const ApiProxy = class extends MockApiClient {
                first() {}
                another() {}
            };
            ApiRouteRegistry.register(ApiProxy, 0);
            expect(ApiRouteRegistry.endpoints.size).toBe(1);
        });

        test("register register one Handler per proxy  ", () => {
            const AProxy = class extends MockApiClient {
                first() {}
            };
            ApiRouteRegistry.register(AProxy, 0);
            const AnotherProxy = class extends MockApiClient {
                first() {}
            };
            const endpoint = ApiRouteRegistry.endpoints.values().next().value;
            ApiRouteRegistry.register(AnotherProxy, 0);
            expect(endpoint.chain).toHaveBeenNthCalledWith(1, AProxy, 0);
            expect(endpoint.chain).toHaveBeenNthCalledWith(2, AnotherProxy, 0);
        });

        test("register to accept different client with same class name ", () => {
            const AProxy = class extends MockApiClient {
                first() {}
            };
            ApiRouteRegistry.register(AProxy, 0);
            const AnotherProxy = class extends MockApiClient {
                first() {}
            };
            EndPoint.key.mockReturnValueOnce("anotherKey");

            ApiRouteRegistry.register(AnotherProxy, 0);
            expect(ApiRouteRegistry.endpoints.size).toBe(2);
        });
    });
    describe("routes", () => {
        test("routes to generate one route per EndPoint", () => {
            ApiRouteRegistry.endpoints.set("One", new EndPoint());
            expect(ApiRouteRegistry.routes().length).toBe(1);
            ApiRouteRegistry.endpoints.set("Two", new EndPoint());
            expect(ApiRouteRegistry.routes().length).toBe(2);
        });
        test("routes are ordered endpoint with path regexp 'specificity ", () => {
            let endpoint = new EndPoint();
            endpoint.priority.mockReturnValue(1);
            endpoint.route.mockReturnValue("A");
            ApiRouteRegistry.endpoints.set("A", endpoint);

            endpoint = new EndPoint();
            endpoint.priority.mockReturnValue(4);
            endpoint.route.mockReturnValue("B");
            ApiRouteRegistry.endpoints.set("B", endpoint);

            endpoint = new EndPoint();
            endpoint.priority.mockReturnValue(3);
            endpoint.route.mockReturnValue("C");
            ApiRouteRegistry.endpoints.set("C", endpoint);

            endpoint = new EndPoint();
            endpoint.priority.mockReturnValue(3);
            endpoint.route.mockReturnValue("D");
            ApiRouteRegistry.endpoints.set("D", endpoint);

            const routes = ApiRouteRegistry.routes();
            expect(routes[0]).toBe("B");
            expect(routes[1]).toBe("C");
            expect(routes[2]).toBe("D");
            expect(routes[3]).toBe("A");
        });
    });
});

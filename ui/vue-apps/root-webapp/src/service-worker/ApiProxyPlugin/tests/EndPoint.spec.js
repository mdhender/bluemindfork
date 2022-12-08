import { Response } from "node-fetch";

import { EndPoint } from "../EndPoint";
import { RegExpRoute } from "workbox-routing";
import { ApiRouteHandler } from "../ApiRouteHandler";
global.fetch = jest.fn();

global.Response = Response;
class Request {
    constructor(params) {
        for (let key in params) {
            this[key] = params[key];
        }
    }
    clone() {
        return this;
    }
}

jest.mock("../ApiRouteHandler");
jest.mock("workbox-routing");

let metadatas;

class MockApiClient {}
describe("EndPoint", () => {
    beforeEach(() => {
        ApiRouteHandler.mockClear();
        ApiRouteHandler.prototype.chain.mockReturnThis();
        metadatas = {
            className: "IMock",
            packageName: "net.bluemind.mock.api",
            path: { value: "/service", parameters: [] },
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
        };
    });
    describe("constructor", () => {
        test("to generate url from client metadata", () => {
            const endpoint = new EndPoint(metadatas.methods[0], metadatas);
            expect(endpoint.url).toBe("/api/service/{path}");
        });
        test("to generate regExp from client metadata", () => {
            const endpoint = new EndPoint(metadatas.methods[0], metadatas);
            expect(endpoint.regExp).toEqual(/\/api\/service\/([^_/][^/]*)/);
        });
        test("to generate regExp with multiple params", () => {
            const endpoint = new EndPoint(metadatas.methods[0], {
                ...metadatas,
                path: {
                    value: "/service/{param}"
                }
            });
            expect(endpoint.regExp).toEqual(/\/api\/service\/([^_/][^/]*)\/([^_/][^/]*)/);
        });
    });
    describe("priority", () => {
        test("to be higher when the path have more part", () => {
            const a = new EndPoint(metadatas.methods[0], metadatas);
            a.url = "/a";
            const b = new EndPoint(metadatas.methods[0], metadatas);
            b.url = "/b";
            const c = new EndPoint(metadatas.methods[0], metadatas);
            c.url = "/b/c";
            const d = new EndPoint(metadatas.methods[0], metadatas);
            d.url = "/abc/d";
            expect(a.priority()).toEqual(b.priority());
            expect(a.priority()).toBeLessThan(c.priority());
            expect(c.priority()).toEqual(d.priority());
        });
        test("to be higher when the path do not have parameter", () => {
            const a = new EndPoint(metadatas.methods[0], metadatas);
            a.url = "/{a}";
            const b = new EndPoint(metadatas.methods[0], metadatas);
            b.url = "/b";
            const c = new EndPoint(metadatas.methods[0], metadatas);
            c.url = "/b/{c}";
            const d = new EndPoint(metadatas.methods[0], metadatas);
            d.url = "/b/d";
            expect(a.priority()).toBeLessThan(b.priority());
            expect(a.priority()).toBeLessThan(c.priority());
            expect(c.priority()).toBeLessThan(d.priority());
        });
    });
    describe("route", () => {
        test("to create a regexp route from metadata", () => {
            const endpoint = new EndPoint(metadatas.methods[0], metadatas);
            const route = endpoint.route();
            expect(RegExpRoute).toHaveBeenCalledWith(endpoint.regExp, expect.anything(), metadatas.methods[0].verb);
            expect(route).toBeInstanceOf(RegExpRoute);
        });
    });
    describe("chain", () => {
        test("to wrap api client in a ApiRouteHandler", () => {
            const endpoint = new EndPoint(metadatas.methods[0], metadatas);
            endpoint.chain(MockApiClient, 0);
            expect(ApiRouteHandler).toHaveBeenCalledWith(MockApiClient, metadatas.methods[0], 0);
        });
        test("to chain Handlers ", () => {
            const endpoint = new EndPoint(metadatas.methods[0], metadatas);
            expect(endpoint.handler).toBeNull();
            endpoint.chain(MockApiClient, 0);
            expect(endpoint.handler).not.toBeNull();
            expect(endpoint.handler.chain).toHaveBeenCalledWith(null);
            const handler = endpoint.handler;
            endpoint.chain(MockApiClient, 0);
            expect(endpoint.handler.chain).toHaveBeenCalledWith(handler);
        });
    });
    describe("handle", () => {
        test("to call handler execute function with request parameters as parameters ", async () => {
            const endpoint = new EndPoint(metadatas.methods[0], metadatas);
            endpoint.chain(MockApiClient, 0);
            const request = new Request({ url: `https://domain.tld/${endpoint.url}` });
            endpoint.parse = jest.fn().mockReturnValue(Promise.resolve("Parameters"));
            await endpoint.handle({ request, params: [] });

            expect(endpoint.handler.execute).toBeCalledTimes(1);
            expect(endpoint.parse).toBeCalledWith(request, []);
            expect(endpoint.handler.execute).toBeCalledWith("Parameters");
        });
        test("to call reply with handler execute result ", async () => {
            const endpoint = new EndPoint(metadatas.methods[0], metadatas);
            endpoint.chain(MockApiClient, 0);
            const request = new Request({ url: `https://domain.tld/${endpoint.url}` });

            endpoint.handler.execute.mockResolvedValue("Result");
            endpoint.reply = jest.fn();
            await endpoint.handle({ request, params: [] });
            expect(endpoint.reply).toBeCalledWith("Result");
        });
        test("to call fetch with request if there is no handler ", async () => {
            const endpoint = new EndPoint(metadatas.methods[0], metadatas);
            const request = new Request({ url: `https://domain.tld/${endpoint.url}` });

            await endpoint.handle({ request, params: [] });
            expect(fetch).toBeCalledWith(request);
        });
        test("to call replyError if an error occurs ", async () => {
            const endpoint = new EndPoint(metadatas.methods[0], metadatas);
            const request = new Request({ url: `https://domain.tld/${endpoint.url}` });
            endpoint.chain(MockApiClient, 0);
            endpoint.handler.execute.mockRejectedValue("Network Error");
            endpoint.replyError = jest.fn();
            await endpoint.handle({ request }, []);
            expect(endpoint.replyError).toBeCalledWith("Network Error");
        });
    });
    describe("parse", () => {
        test("to parse client parameters from second parameter", async () => {
            const endpoint = new EndPoint(metadatas.methods[0], {
                ...metadatas,
                path: {
                    value: "/service/{0}/{1}",
                    parameters: ["0", "1"]
                }
            });
            const request = new Request({ url: `https://domain.tld/api/service/` });
            const result = await endpoint.parse(request, ["zero", "one", "two"]);
            expect(result.client).toEqual(["zero", "one"]);
        });
        test("to parse method path parameters from second parameter", async () => {
            metadatas.methods[0].inParams = [
                { name: "0", paramType: "PathParam" },
                { name: "1", paramType: "PathParam" }
            ];
            const endpoint = new EndPoint(metadatas.methods[0], metadatas);
            const request = new Request({ url: `https://domain.tld/api/service/` });
            const result = await endpoint.parse(request, ["zero", "one", "two"]);
            expect(result.method).toEqual(["zero", "one"]);
        });
        test("to parse method query parameters from url search params", async () => {
            metadatas.methods[0].inParams = [
                { name: "0", paramType: "QueryParam" },
                { name: "1", paramType: "QueryParam" }
            ];
            const endpoint = new EndPoint(metadatas.methods[0], metadatas);
            const request = new Request({ url: `https://domain.tld/api/service/?ignored=true&0=zero&1=one` });
            const result = await endpoint.parse(request, []);
            expect(result.method).toEqual(["zero", "one"]);
        });
        test("to parse method body stream parameters from request body as text", async () => {
            metadatas.methods[0].inParams = [{ name: "0", type: { name: "Stream" }, paramType: "Body" }];
            const endpoint = new EndPoint(metadatas.methods[0], metadatas);
            const request = new Request({ url: `https://domain.tld/`, text: jest.fn().mockResolvedValue("BodyBody") });
            const result = await endpoint.parse(request, []);
            expect(result.method).toEqual(["BodyBody"]);
        });

        test("to parse method body parameters from request body as json", async () => {
            metadatas.methods[0].inParams = [{ name: "0", type: { name: "Custom" }, paramType: "Body" }];
            const endpoint = new EndPoint(metadatas.methods[0], metadatas);
            const Body = {};
            const request = new Request({ url: `https://domain.tld/`, json: jest.fn().mockResolvedValue(Body) });
            const result = await endpoint.parse(request, []);
            expect(result.method).toEqual([Body]);
        });
        test.skip("[NOT IMPLEMENTED] to serve method stream parameters as a Stream implementation", async () => {
            //FIXME : Not yet implmented...
            metadatas.methods[0].inParams = [{ name: "0", type: { name: "Stream" }, paramType: "Body" }];
            const endpoint = new EndPoint(metadatas.methods[0], metadatas);
            const Body = {};
            const request = new Request({ url: `https://domain.tld/`, text: jest.fn().mockResolvedValue(Body) });
            const result = await endpoint.parse(request, []);
            expect(result.method[0]).toBeInstanceOf(ReadableStream);
        });
    });
    describe("reply", () => {
        test("to send a Response with result as json on custom types", async () => {
            metadatas.methods[0].outParam = [{ type: { name: "Custom" } }];
            const endpoint = new EndPoint(metadatas.methods[0], metadatas);
            const response = endpoint.reply({ value: "Result" });
            expect(response).toBeInstanceOf(Response);
            expect(response.status).toBe(200);
            expect(await response.json()).toEqual({ value: "Result" });
        });
        test("to send a Response with result as text on Stream types", async () => {
            metadatas.methods[0].outParam = { name: "Stream" };
            const endpoint = new EndPoint(metadatas.methods[0], metadatas);
            const response = endpoint.reply("Stream");
            expect(response).toBeInstanceOf(Response);
            expect(response.status).toBe(200);
            expect(await response.text()).toEqual("Stream");
        });
        test("to add a custom header", () => {
            metadatas.methods[0].outParam = { name: "Custom" };
            const endpoint = new EndPoint(metadatas.methods[0], metadatas);
            const response = endpoint.reply({ value: "Result" });
            expect(response.headers.get("X-Bm-ServiceWorker")).toBe("true");
        });
        test("to set response type depending on produce metadata with fallback", () => {
            metadatas.methods[0].outParam = { name: "JSON" };
            let endpoint = new EndPoint(metadatas.methods[0], metadatas);
            let response = endpoint.reply("");
            expect(response.headers.get("content-type")).toBe("application/json");
            metadatas.methods[0].outParam = { name: "Stream" };
            endpoint = new EndPoint(metadatas.methods[0], metadatas);
            response = endpoint.reply("");
            expect(response.headers.get("content-type")).toBe("application/octet-stream");
            metadatas.methods[0].produce = "text/calendar";
            endpoint = new EndPoint(metadatas.methods[0], metadatas);
            response = endpoint.reply("");
            expect(response.headers.get("content-type")).toBe("text/calendar");
        });
    });
    describe("replyError", () => {
        test("to send a Response with an error code", () => {
            const endpoint = new EndPoint(metadatas.methods[0], metadatas);
            const response = endpoint.replyError("Error");
            expect(response).toBeInstanceOf(Response);
            expect(response.status).toBe(500);
        });
        test("to send error message as json value", async () => {
            const endpoint = new EndPoint(metadatas.methods[0], metadatas);
            const response = endpoint.replyError("Error");
            expect(response.status).toBe(500);
            expect(await response.json()).toEqual({ errorType: "ServiceWorkerProxyError", message: "Error" });
        });
    });
});

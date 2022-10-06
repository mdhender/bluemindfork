import { ApiHandler } from "../ApiHandler";
import Session from "../../session";
jest.mock("../../session");

const implementations = [];
const constructor = jest.fn();
const method = jest.fn();
const getMetadata = jest.fn();

class MockApiClient {
    constructor() {
        constructor(...arguments);
        implementations.push(this);
    }
    method() {
        method(...arguments);
    }
    getMetadata() {
        getMetadata(...arguments);
    }
}
Session.sid = Promise.resolve("SID");

describe("ApiHandler", () => {
    beforeEach(() => {
        implementations.splice(0, implementations.length);
        constructor.mockClear();
        method.mockClear();
        getMetadata.mockClear();
    });
    describe("chain", () => {
        test("to return this if handler priority is higher than parameter priority", () => {
            const handler = new ApiHandler(MockApiClient, {}, 2);
            const anotherHandler = new ApiHandler(MockApiClient, {}, 1);
            const result = handler.chain(anotherHandler);
            expect(result).toBe(handler);
            expect(handler.next).toBe(anotherHandler);
        });
        test("to return parameter if parameter priority is higher than hander priority", () => {
            const handler = new ApiHandler(MockApiClient, {}, 2);
            const anotherHandler = new ApiHandler(MockApiClient, {}, 3);
            const result = handler.chain(anotherHandler);
            expect(result).toBe(anotherHandler);
            expect(handler.next).toBeNull();
            expect(anotherHandler.next).toBe(handler);
        });

        test("to accept a null value", () => {
            const handler = new ApiHandler(MockApiClient, {}, 2);
            const result = handler.chain(null);
            expect(result).toBe(handler);
            expect(handler.next).toBeNull();
        });
    });
    describe("execute", () => {
        test("to build client with parameters", async () => {
            const handler = new ApiHandler(MockApiClient, { name: "method" }, 0);
            await handler.execute({ client: ["one", "two"], method: [] });
            expect(constructor).toBeCalledWith("one", "two", await Session.sid);
        });
        test("to constructor client method", async () => {
            const handler = new ApiHandler(MockApiClient, { name: "method" }, 0);
            const parameters = { client: [], method: ["one", "two"] };
            await handler.execute(parameters);
            expect(method).toBeCalledWith(...parameters.method);
        });
        test("to constructor client with overwritten parameters", async () => {
            const handler = new ApiHandler(MockApiClient, { name: "method" }, 0);
            const parameters = { client: [], method: ["one", "two"] };
            await handler.execute(parameters, "three", "four");
            expect(method).toBeCalledWith("three", "four");
        });
        test("to add a property 'next' to client", async () => {
            const handler = new ApiHandler(MockApiClient, { name: "method" }, 0);
            const anotherHandler = { execute: jest.fn() };
            handler.next = anotherHandler;
            const parameters = { client: ["one", "two"], method: [] };
            await handler.execute(parameters);
            expect(implementations[0].next).toBeDefined();
            expect(anotherHandler.execute).not.toBeCalled();
            implementations[0].next();
            expect(anotherHandler.execute).toBeCalledWith(parameters);
        });
        test("to use overwritten parameter in next execution", async () => {
            const handler = new ApiHandler(MockApiClient, { name: "method" }, 0);
            const anotherHandler = { execute: jest.fn() };
            handler.next = anotherHandler;
            const parameters = { client: ["one", "two"], method: [] };
            await handler.execute(parameters, "two", "three");
            implementations[0].next();
            expect(anotherHandler.execute).toBeCalledWith({ client: ["one", "two"], method: ["two", "three"] });
        });
        test("to set next with a fallback on root client api", async () => {
            const ExtendedMockApiClient = class extends MockApiClient {
                method() {
                    return this.next();
                }
            };
            const handler = new ApiHandler(ExtendedMockApiClient, { name: "method" }, 0);
            const parameters = { client: ["one", "two"], method: [] };

            await handler.execute(parameters);
            expect(implementations.length).toBe(2);
            expect(implementations[1]).toBeInstanceOf(MockApiClient);
            expect(implementations[1]).not.toBeInstanceOf(ExtendedMockApiClient);
        });
    });
});

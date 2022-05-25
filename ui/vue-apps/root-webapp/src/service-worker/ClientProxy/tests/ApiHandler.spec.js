import { ApiHandler } from "../ApiHandler";
import Session from "../../session";
import { UnhandledRequestError } from "../UnhandedRequestError";
jest.mock("../../session");

const MockApiClient = jest.fn();
Session.sid = Promise.resolve("SID");

describe("ApiHandler", () => {
    beforeEach(() => {
        MockApiClient.mockClear();
        MockApiClient.mockImplementation(function () {
            return {};
        });
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
            const mockedClient = { method: jest.fn() };
            MockApiClient.mockImplementation(() => mockedClient);
            const handler = new ApiHandler(MockApiClient, { name: "method" }, 0);
            await handler.execute({ client: ["one", "two"], method: [] });
            expect(MockApiClient).toBeCalledWith("one", "two", await Session.sid);
        });
        test("to call client method", async () => {
            const mockedClient = { method: jest.fn() };
            const MockApiClient = () => mockedClient;
            const handler = new ApiHandler(MockApiClient, { name: "method" }, 0);
            const parameters = { client: [], method: ["one", "two"] };
            await handler.execute(parameters);
            expect(mockedClient.method).toBeCalledWith(...parameters.method);
        });
        test("to call client with overwritten parameters", async () => {
            const mockedClient = { method: jest.fn() };
            const MockApiClient = () => mockedClient;
            const handler = new ApiHandler(MockApiClient, { name: "method" }, 0);
            const parameters = { client: [], method: ["one", "two"] };
            await handler.execute(parameters, "three", "four");
            expect(mockedClient.method).toBeCalledWith("three", "four");
        });
        test("to add a property 'next' to client", async () => {
            const mockedClient = { method: () => null };
            const MockApiClient = () => mockedClient;
            const handler = new ApiHandler(MockApiClient, { name: "method" }, 0);
            const anotherHandler = { execute: jest.fn() };
            handler.next = anotherHandler;
            const parameters = { client: ["one", "two"], method: [] };
            await handler.execute(parameters);
            expect(mockedClient.next).toBeDefined();
            expect(anotherHandler.execute).not.toBeCalled();
            mockedClient.next();
            expect(anotherHandler.execute).toBeCalledWith(parameters);
        });
        test("to use overwritten parameter in next execution", async () => {
            const mockedClient = { method: () => null };
            const MockApiClient = () => mockedClient;
            const handler = new ApiHandler(MockApiClient, { name: "method" }, 0);
            const anotherHandler = { execute: jest.fn() };
            handler.next = anotherHandler;
            const parameters = { client: ["one", "two"], method: [] };
            await handler.execute(parameters, "two", "three");
            mockedClient.next();
            expect(anotherHandler.execute).toBeCalledWith({ client: ["one", "two"], method: ["two", "three"] });
        });
        test("to set next with a fallback", async () => {
            expect.assertions(1);

            const mockedClient = { method: () => null };
            const MockApiClient = () => mockedClient;
            const handler = new ApiHandler(MockApiClient, { name: "method" }, 0);
            const parameters = { client: ["one", "two"], method: [] };

            await handler.execute(parameters);
            try {
                await mockedClient.next();
            } catch (e) {
                expect(e).toBeInstanceOf(UnhandledRequestError);
            }
        });
    });
});

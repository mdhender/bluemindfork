import WebSocketClient from "../src/";
import SockJS from "sockjs-client";
import injector from "@bluemind/inject";

jest.mock("@bluemind/inject");
jest.mock("sockjs-client");

injector.getProvider.mockReturnValue({
    get: () => {
        return { sid: 1 };
    }
});

const websocket = {
    status: 200,
    readyState: 0,
    send(json) {
        const data = JSON.parse(json);
        data.statusCode = this.status;
        setTimeout(() => this.onmessage({ data: JSON.stringify(data) }), 0);
        if (!data.XTestDelay) {
            jest.runOnlyPendingTimers();
        }
    },
    open() {
        if (this.readyState !== 1) {
            this.readyState = 1;
            this.onopen();
        }
    },
    close() {
        if (this.readyState !== 0) {
            this.readyState = 0;
            this.onclose();
        }
    }
};

describe("WebSocketClient", () => {
    beforeEach(() => {
        SockJS.mockImplementation(() => websocket);
    });

    afterEach(() => {
        new WebSocketClient().reset();
    });

    test("Only the first WebSocketClient object create a new SockJs connection", () => {
        new WebSocketClient("server.local.host");
        websocket.open();
        expect(SockJS).toHaveBeenCalledWith("server.local.host");
        new WebSocketClient("");
        expect(SockJS).toHaveBeenCalledTimes(1);
    });

    test("Getting or posting data must trigger the listener only once", async () => {
        const socket = new WebSocketClient("server.local.host");
        websocket.open();
        const request = { method: "GET", requestId: "1" };
        const callback = jest.fn();
        await socket.send(request, callback);
        await socket.send(request);
        expect(callback).toHaveBeenCalledTimes(1);
    });

    test("Ping to be called immediately if ping method is called, and periodically otherwise", async () => {
        const socket = new WebSocketClient("server.local.host");
        websocket.open();
        const callback = jest.fn();
        jest.useFakeTimers();
        try {
            await socket.ping(callback).then(() => {
                jest.runOnlyPendingTimers();
                expect(callback).toHaveBeenCalledTimes(2);
            });
            expect.hasAssertions();
        } finally {
            jest.useRealTimers();
        }
    });

    test("Listener registered on a bus must be called each time a message is sent until unregistered", async () => {
        expect.hasAssertions();
        const socket = new WebSocketClient("server.local.host");
        websocket.open();
        const callback = jest.fn();
        await socket.register("/1", callback);
        websocket.onmessage({ data: JSON.stringify({ requestId: "/1" }) });
        websocket.onmessage({ data: JSON.stringify({ requestId: "/1" }) });
        await socket.unregister("/1");
        websocket.onmessage({ data: JSON.stringify({ requestId: "/1" }) });
        expect(callback).toHaveBeenCalledTimes(2);
    });

    test("Disconnection makes direct listeners to fail", async () => {
        expect.assertions(2);
        const socket = new WebSocketClient("server.local.host");
        websocket.open();
        const request = { method: "GET", XTestDelay: 1 };
        const callback = event => expect(event.data.statusCode).toBe(408);
        websocket.close();
        await socket.send(request, callback).catch(e => expect(e.statusCode).toBe(408));
    });

    test("Online / Offline switch trigger an event", () => {
        expect.assertions(4);
        const socket = new WebSocketClient("server.local.host");
        let state = true;
        const online = event => expect(event.online).toBe(state);
        socket.onOnlineChange(online);
        websocket.open();
        expect(socket.isOnline()).toBe(state);
        state = false;
        websocket.close();
        expect(socket.isOnline()).toBe(state);
    });

    test("Plugins can watch all native event plus all sent request", async () => {
        const open = jest.fn(),
            request = jest.fn(),
            response = jest.fn(),
            message = jest.fn(),
            close = jest.fn();
        const plugin = {
            install(socket, dummy) {
                expect(dummy).toBe("dummy");
                socket.addEventListener("open", open);
                socket.addEventListener("request", request);
                socket.addEventListener("response", response);
                socket.addEventListener("message", message);
                socket.addEventListener("close", close);
            }
        };
        const socket = new WebSocketClient("server.local.host");
        WebSocketClient.use(plugin, "dummy");
        websocket.open();
        expect(open).toHaveBeenCalled();
        await socket.register("1");
        expect(request).toHaveBeenCalled();
        expect(message).toHaveBeenCalled();
        request.mockClear();
        response.mockClear();
        await socket.send({});
        expect(request).toHaveBeenCalled();
        expect(response).toHaveBeenCalled();
        websocket.close();
        expect(close).toHaveBeenCalled();
    });

    test("Reset method close the websocket, clear all listeners and prevent reconnection.", async () => {
        const ping = jest.fn(),
            online = jest.fn(),
            bus = jest.fn();
        let socket = new WebSocketClient("server.local.host");
        websocket.open();
        socket.onOnlineChange(online);
        await socket.register("/dummy", bus);
        await socket.ping(ping);
        await socket.reset();
        socket = new WebSocketClient("server.local.host");
        websocket.onmessage({ data: JSON.stringify({ requestId: "/dummy" }) });
        await socket.ping().catch(() => {});
        expect(bus).not.toHaveBeenCalled();
        expect(online).toHaveBeenCalledTimes(1);
        expect(ping).toHaveBeenCalledTimes(1);
    });

    test.skip("After reconnection all registered listener are registered again", () => {
        expect(false).toBeTruthy();
    });
    test.skip("Registering to a bus while offline work once online", () => {
        expect(false).toBeTruthy();
    });
    test.skip("Failing to register to a bus do not leak the bus listener", () => {
        expect(false).toBeTruthy();
    });
    test.skip("Registering two time the same bus should not (or should ?) send two register request", () => {
        expect(false).toBeTruthy();
    });
    test.skip("Disconnect will try to reconnect automatically", () => {
        expect(false).toBeTruthy();
    });
});

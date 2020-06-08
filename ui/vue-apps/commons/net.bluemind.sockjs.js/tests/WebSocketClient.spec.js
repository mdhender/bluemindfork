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

jest.useFakeTimers();
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

xdescribe("WebSocketClient", () => {
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

    test("Getting or posting data must trigger the listener only once", () => {
        const socket = new WebSocketClient("server.local.host");
        websocket.open();
        const request = { method: "GET", requestId: "1" };
        const callback = jest.fn();
        socket.send(request, callback);
        socket.send(request);
        expect(callback).toHaveBeenCalledTimes(1);
    });

    test("Ping to be called immediately if ping method is called, and periodically otherwise", () => {
        expect.hasAssertions();
        const socket = new WebSocketClient("server.local.host");
        websocket.open();
        const callback = jest.fn();
        socket.ping(callback).then(() => {
            jest.runOnlyPendingTimers();
            expect(callback).toHaveBeenCalledTimes(2);
        });
    });

    test("Listener registered on a bus must be called each time a message is sent until unregistered", () => {
        expect.hasAssertions();
        const socket = new WebSocketClient("server.local.host");
        websocket.open();
        const callback = jest.fn();
        socket.register("/1", callback);
        websocket.onmessage({ data: JSON.stringify({ requestId: "/1" }) });
        websocket.onmessage({ data: JSON.stringify({ requestId: "/1" }) });
        socket.unregister("/1");
        websocket.onmessage({ data: JSON.stringify({ requestId: "/1" }) });
        expect(callback).toHaveBeenCalledTimes(2);
    });

    test("Deconnection make direct listeners fail", () => {
        expect.assertions(2);
        const socket = new WebSocketClient("server.local.host");
        websocket.open();
        const request = { method: "GET", XTestDelay: 1 };
        const callback = event => expect(event.data.statusCode).toBe(408);
        const promise = socket.send(request, callback).catch(e => expect(e.statusCode).toBe(408));
        websocket.close();
        jest.runOnlyPendingTimers();
        return promise;
    });
    test("Online / Offline switch trigger an event", () => {
        expect.assertions(4);
        const socket = new WebSocketClient("server.local.host");
        let state = true;
        const online = event => expect(event.online).toBe(state);
        socket.onOnline(online);
        websocket.open();
        expect(socket.isOnline()).toBe(state);
        state = false;
        websocket.close();
        expect(socket.isOnline()).toBe(state);
    });
    test("Plugins can watch all native event plus all sent request", () => {
        const open = jest.fn(),
            send = jest.fn(),
            message = jest.fn(),
            close = jest.fn();
        const plugin = {
            install(socket, dummy) {
                expect(dummy).toBe("dummy");
                socket.addEventListener("open", open);
                socket.addEventListener("send", send);
                socket.addEventListener("message", message);
                socket.addEventListener("close", close);
            }
        };
        const socket = new WebSocketClient("server.local.host");
        socket.use(plugin, "dummy");
        websocket.open();
        expect(open).toHaveBeenCalled();
        socket.register("1");
        expect(send).toHaveBeenCalled();
        expect(message).toHaveBeenCalled();
        websocket.close();
        expect(close).toHaveBeenCalled();
    });
    test("Reset method close the websocket, clear all listeners and prevent reconnection.", () => {
        const ping = jest.fn(),
            online = jest.fn(),
            bus = jest.fn();
        let socket = new WebSocketClient("server.local.host");
        websocket.open();
        socket.onOnline(online);
        socket.register("/dummy", bus);
        socket.ping(ping);
        socket.reset();
        socket = new WebSocketClient("server.local.host");
        websocket.onmessage({ data: JSON.stringify({ requestId: "/dummy" }) });
        socket.ping();
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

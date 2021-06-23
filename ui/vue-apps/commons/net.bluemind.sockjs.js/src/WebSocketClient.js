import SockJS from "sockjs-client";
import injector from "@bluemind/inject";
import global from "@bluemind/global";
import UUIDGenerator from "@bluemind/uuid";
import { EventTarget } from "@bluemind/event";
import WebSocketEventTarget from "./WebSocketEventTarget";
import OnlineEvent from "./OnlineEvent";
import RestEvent from "./RestEvent";

const WEBSOCKET_DEFAULT_URL = "/eventbus/";

const websocket = (global.$websocket = global.$websocket || createWebsocket());

/**
 *
 * @param url specify an URL for the websocket you want to use. Default matchs Bluemind websocket.
 *
 */
export default class WebSocketClient {
    constructor(url = WEBSOCKET_DEFAULT_URL) {
        this.persistentRegistrations = [];
        this.onOnlineChange(({ online }) => {
            if (online) {
                this.persistentRegistrations.forEach(({ path, listener }) => this.register(path, listener));
            }
        });

        if (!isInit(url)) {
            websocket.url = url;
            websocket.client = createSockJsClient();
        }
    }

    async register(path, listener) {
        if (!this.persistentRegistrations.some(pending => pending.path === path && pending.listener === listener)) {
            this.persistentRegistrations.push({ path, listener });
        }
        try {
            await this.send({ method: Method.REGISTER, path }, listener);
        } catch (error) {
            console.log("[Push] Waiting for websocket to be online", error);
        }
    }

    send(request, listener) {
        return send(request, listener);
    }

    unregister(path, listener) {
        this.persistentRegistrations = this.persistentRegistrations.filter(
            pending => pending.path !== path || pending.listener !== listener
        );
        return this.send({ method: Method.UNREGISTER, path }, listener);
    }

    ping(listener) {
        return ping(listener);
    }

    isOnline() {
        return websocket.online;
    }

    onOnlineChange(listener) {
        websocket.handler.register(OnlineEvent.TYPE, listener);
    }

    reset() {
        reset();
    }

    static use(plugin, ...args) {
        use(plugin, ...args);
    }
}

const PING_ID = "55CA9ACE-0B20-4891-BB3F-40D12BFD476B";
const PING_INTERVAL = 20 * 1000;
const HEARTBEAT_INTERVAL = 100 * 1000;

const Method = {
    REGISTER: "register",
    UNREGISTER: "unregister"
};

function createWebsocket() {
    return {
        client: null,
        handler: new WebSocketEventTarget(),
        plugins: new EventTarget(),
        online: false,
        url: null,
        timers: {
            ping: null,
            heartbeat: null,
            connect: null
        }
    };
}

function isInit(url) {
    return url === websocket.url && websocket.client;
}

function createSockJsClient() {
    clearTimeout(websocket.timers.connect);
    if (websocket.client !== null && websocket.client.readyState !== SockJS.CLOSED) {
        return websocket.client;
    }
    const client = new SockJS(websocket.url);
    client.onopen = function () {
        websocket.timers.ping = setTimeout(ping, PING_INTERVAL);
        online();
        websocket.plugins.dispatchEvent(new Event("open"));
    };

    client.onheartbeat = function () {
        clearTimeout(websocket.timers.heartbeat);
        websocket.timers.heartbeat = setTimeout(() => websocket.client.close(), HEARTBEAT_INTERVAL);
    };

    client.onclose = function () {
        clearTimeout(websocket.timers.ping);
        clearTimeout(websocket.timers.heartbeat);
        offline();
        websocket.timers.connect = setTimeout(reconnect, 1000);
        websocket.plugins.dispatchEvent(new Event("close"));
    };

    client.onmessage = function (event) {
        var response = JSON.parse(event.data);
        websocket.handler.dispatchEvent(new RestEvent(response.requestId, response));
        websocket.plugins.dispatchEvent(new RestEvent("message", response));
    };

    return client;
}

function send(request, listener) {
    request.requestId = request.requestId || UUIDGenerator.generate();
    request.headers = request.headers || {};
    request.headers["X-BM-ApiKey"] = request.headers["X-BM-ApiKey"] || injector.getProvider("UserSession").get().sid;
    request.params = request.params || {};

    if (listener) {
        if (request.method === Method.REGISTER) {
            websocket.handler.register(request.path, listener);
        } else {
            websocket.handler.addReplyListener(request.requestId, listener);
        }
    }
    if (request.method === Method.UNREGISTER) {
        websocket.handler.unregister(request.path);
    }

    websocket.plugins.dispatchEvent(new RestEvent("request", request));

    const promise = new Promise(resolver.bind(this, request.requestId));
    if (websocket.client.readyState !== SockJS.OPEN) {
        websocket.handler.disconnected(request.requestId);
    } else {
        websocket.client.send(JSON.stringify(request));
    }
    return promise;
}

function ping(callback) {
    if (callback) {
        websocket.handler.register(PING_ID, callback);
    }
    if (websocket.timers.ping !== null) {
        clearTimeout(websocket.timers.ping);
        websocket.timers.ping = null;
        websocket.handler.addReplyListener(PING_ID, () => (websocket.timers.ping = setTimeout(ping, PING_INTERVAL)));
        const request = {
            method: "GET",
            requestId: PING_ID,
            path: "/api/auth/ping"
        };
        return send(request, callback);
    } else {
        return new Promise(resolver.bind(this, PING_ID));
    }
}

function online() {
    setOnline(true);
}

function offline() {
    if (setOnline(false)) {
        websocket.handler.broadcastDisconnected();
    }
}

function setOnline(state) {
    if (websocket.online !== state) {
        websocket.online = state;
        websocket.handler.dispatchEvent(new OnlineEvent(state));
        websocket.plugins.dispatchEvent(new OnlineEvent(state));
        return true;
    }
    return false;
}

function reconnect() {
    websocket.client = createSockJsClient();
}

function resolver(requestId, resolve, reject) {
    websocket.handler.addReplyListener(requestId, event => {
        websocket.plugins.dispatchEvent(new RestEvent("response", event));
        if (event.data.statusCode !== 200) {
            reject(event.data);
        } else {
            resolve(event.data);
        }
    });
}

function use(plugin, ...args) {
    args.unshift(websocket.plugins);
    if (typeof plugin.install === "function") {
        plugin.install.apply(plugin, args);
    } else if (typeof plugin === "function") {
        plugin.apply(null, args);
    }
}

function reset() {
    websocket.client.close();
    websocket.handler.clear();
    websocket.plugins.clear();
    websocket.url = null;
    clearTimeout(websocket.timers.heartbeat);
    clearTimeout(websocket.timers.ping);
    clearTimeout(websocket.timers.reconnect);
}

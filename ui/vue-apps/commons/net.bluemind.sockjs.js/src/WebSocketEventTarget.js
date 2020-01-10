import RestEvent from "./RestEvent";
import { EventTarget } from "@bluemind/event";

export default class WebSocketEventTarget extends EventTarget {
    register(type, listener) {
        this.addEventListener(type, listener, {});
    }

    unregister(type) {
        this.removeEventListener(type);
    }

    addReplyListener(type, listener) {
        this.addEventListener(type, listener, { once: true });
    }

    removeReplyListener(type) {
        this.removeEventListener(type);
    }

    disconnected(type) {
        this.dispatchEvent(RestEvent.disconnected(type));
    }

    broadcast(data) {
        for (const type in this.listeners) {
            this.listeners[type].forEach(listener => {
                if (listener.options.once) {
                    this.dispatchEvent(new RestEvent(type, data));
                }
            });
        }
    }

    broadcastDisconnected() {
        for (const type in this.listeners) {
            this.listeners[type].forEach(listener => {
                if (listener.options.once) {
                    this.disconnected(type);
                }
            });
        }
    }
}

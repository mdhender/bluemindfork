import Event from "./Event";

export default class EventTarget {
    constructor() {
        this.listeners = {};
    }

    addEventListener(type, listener, options) {
        const listeners = this.listeners[type] || (this.listeners[type] = []);
        if (listeners.findIndex(l => l.listener === listener) < 0) {
            listeners.push({ target: this, listener: listener, options: options || {} });
        }
    }

    removeEventListener(type, listener) {
        if (listener) {
            const i = this.listeners[type].findIndex(l => l.listener === listener);
            if (-1 < i) this.listeners[type].splice(i, 1);
        } else {
            delete this.listeners[type];
        }
    }

    dispatchEvent(event) {
        if (typeof event == "string") {
            event = new Event(event);
        }
        if (this.listeners[event.type]) {
            const listeners = this.listeners[event.type].slice();
            listeners.forEach(info => {
                if (!event.stopped) {
                    if (info.options.once) {
                        this.removeEventListener(event.type, info.listener);
                    }
                    info.listener.call(info.target, event);
                }
            });
        }
        return true;
    }

    clear() {
        for (let type in this.listeners) {
            this.removeEventListener(type);
        }
        this.listeners = {};
    }
}

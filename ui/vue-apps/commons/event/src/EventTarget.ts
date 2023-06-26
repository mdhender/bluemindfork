type EventListenerEntry = {
    target: EventTarget;
    listener: EventListener;
    options: AddEventListenerOptions;
};
export default class EventTarget implements EventTarget {
    listeners: Record<string, EventListenerEntry[]> = {};

    addEventListener(type: string, listener: EventListener, options?: AddEventListenerOptions): void {
        const listeners = this.listeners[type] || (this.listeners[type] = []);
        if (listeners.findIndex(l => l.listener === listener) < 0) {
            listeners.push({ target: this, listener: listener, options: options || {} });
        }
    }

    removeEventListener(type: string, listener?: EventListener): void {
        if (listener) {
            const i = this.listeners[type]?.findIndex(l => l.listener === listener);
            if (-1 < i) {
                this.listeners[type].splice(i, 1);
            }
        } else {
            delete this.listeners[type];
        }
    }

    dispatchEvent(event: Event): boolean {
        if (typeof event === "string") {
            event = new Event(event);
        }
        if (this.listeners[event.type]) {
            const listeners = this.listeners[event.type].slice();
            listeners.forEach(({ listener, target, options }) => {
                if (options.once) {
                    this.removeEventListener(event.type, listener);
                }
                listener.call(target, event);
            });
        }
        return true;
    }

    clear() {
        for (const type in this.listeners) {
            this.removeEventListener(type);
        }
        this.listeners = {};
    }

    has(type: string, listener: EventListener) {
        if (listener) {
            return this.listeners[type]?.findIndex(l => l.listener === listener) >= 0;
        }
        return this.listeners[type] && this.listeners[type].length > 0;
    }
}

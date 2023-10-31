export class MockInteractable {
    constructor() {
        this.listeners = {};
        this.options = {};
    }
    on(type, callback) {
        this.listeners[type] = this.listeners[type] || [];
        this.listeners[type].push(callback);
        return this;
    }
    off(type, callback) {
        if (this.listeners[type]) {
            this.listeners[type] = this.listeners[type].filter(listener => listener !== callback);
        }
        return this;
    }
    fire(type, args) {
        (this.listeners[type] || []).forEach(listener => listener.apply(this, args));
    }
    context() {
        return window.document;
    }
}

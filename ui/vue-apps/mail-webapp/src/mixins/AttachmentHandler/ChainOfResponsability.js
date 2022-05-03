export default class {
    constructor(vm, priority = 0) {
        this.priority = priority;
        this.vm = vm;
    }
    chain(handler) {
        if (handler === this) {
            return this;
        }
        if (this.priority > handler.priority) {
            this.next = this.next ? this.next.chain(handler) : handler;
            return this;
        }
        return handler.chain(this);
    }
}

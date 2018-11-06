export default class {
    constructor(type, opt_target) {
        this.type = type;
        this.target = opt_target;
        this.currentTarget = this.target;
        this.stopped = false;
        this.defaultPrevented = true;
        this.isTrusted = false;
    }

    stopPropagation() {
        this.stopped = true;
    }

    preventDefault() {
        this.defaultPrevented = false;
    }
}

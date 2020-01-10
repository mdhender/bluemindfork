import { _Bus } from "./install";

export default class Client {
    $on(event, callback) {
        return _Bus.$on(event, callback);
    }

    $off() {
        return _Bus.$off.apply(_Bus, arguments);
    }

    $once(event, callback) {
        return _Bus.$once(event, callback);
    }

    $emit(event, payload) {
        _Bus.$emit.apply(_Bus, ["*", event, payload]);
        return _Bus.$emit.apply(_Bus, [event, event, payload]);
    }
}

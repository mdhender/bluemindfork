import WebSocketClient from "@bluemind/sockjs";
import injector from "@bluemind/inject";

const socket = new WebSocketClient();

export default {
    observe(type, uid) {
        if (type && uid) {
            socket.register("bm." + type + ".hook." + uid + ".changed", this.notify);
        }
    },

    forget(type, uid) {
        if (type && uid) {
            socket.unregister("bm." + type + ".hook." + uid + ".changed");
        }
    },

    notify({ data }) {
        const bus = injector.getProvider("GlobalEventBus").get();
        const [, type, uid] = data.requestId.match(/^bm\.([^.]*).hook.(.*).changed$/);
        bus.$emit(type + "_changed", { container: uid });
    }
};

import WebSocketClient from "@bluemind/sockjs";

export default {
    install(Vue, { url, VueBus }) {
        const socket = new WebSocketClient(url || "/event/bus");
        Vue.prototype.$socket = socket;

        if (VueBus) {
            const client = new VueBus.Client();
            socket.onOnline(event => {
                client.$emit(event.type, event.online);
            });
            socket.ping(event => {
                if (event.statusCode !== 200) {
                    client.$emit("disconnected");
                }
            });
        }
    }
};

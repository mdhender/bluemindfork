import WebSocketClient from "@bluemind/sockjs";

export default {
    install(Vue, VueBus) {
        Vue.prototype.$socket = new WebSocketClient();
        if (VueBus) {
            const client = new VueBus.Client();
            WebSocketClient.use(handler => {
                handler.addEventListener("response", ({ data }) => {
                    if (data.statusCode === 401) {
                        client.$emit("disconnected");
                    }
                });
                handler.addEventListener("online", ({ online }) => {
                    client.$emit("online", online);
                });
            });
        }
    }
};

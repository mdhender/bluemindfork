import { mapState } from "vuex";

export default {
    computed: {
        ...mapState("mail", ["activeFolder", "folders", "mailboxes"])
    },
    async created() {
        await this.initialized;
        try {
            (await this.$_ServerPush_sendMessage({ type: "INIT" }, false, [])).forEach(updated => {
                this.$_ServerPush_refreshUI({ body: { mailbox: updated } });
            });
            Object.values(this.mailboxes).forEach(mailbox => {
                this.$socket.register(`mailreplica.${mailbox.owner}.updated`, this.$_ServerPush_handle(mailbox));
            });
        } catch (error) {
            console.error("[SW] failed to init service worker", error);
        }
    },
    methods: {
        async $_ServerPush_sendMessage(message, skip, defaultResponse) {
            return new Promise(resolve => {
                const messageChannel = new MessageChannel();
                if (!serviceWorker || skip) {
                    resolve(defaultResponse);
                } else {
                    messageChannel.port1.onmessage = message => {
                        messageChannel.port1.close();
                        resolve(message.data);
                    };
                    serviceWorker.postMessage(message, [messageChannel.port2]);
                }
            });
        },
        $_ServerPush_handle(mailbox) {
            return async ({ data }) => {
                const message = { type: "SYNCHRONIZE", body: data.body };
                try {
                    const updated = await this.$_ServerPush_sendMessage(message, !mailbox.offlineSync, true);
                    if (!data.body.isHierarchy && updated) {
                        this.$_ServerPush_refreshUI(data);
                    }
                } catch (error) {
                    console.error(`[SW] failed to send '${message}' to service worker`, error);
                }
            };
        },
        async $_ServerPush_refreshUI(data) {
            if (data.body.mailbox in this.folders) {
                this.$bus.$emit("mail-webapp/unread_folder_count", this.folders[data.body.mailbox]);
            }
            if (data.body.mailbox === this.activeFolder) {
                this.$bus.$emit("mail-webapp/pushed_folder_changes", data);
            }
        }
    }
};
const serviceWorker = navigator.serviceWorker && navigator.serviceWorker.controller;

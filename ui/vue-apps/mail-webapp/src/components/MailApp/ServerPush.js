import { mapState } from "vuex";

export default {
    computed: {
        ...mapState("mail", ["activeFolder", "folders", "mailboxes"]),
        $_ServerPush_serviceWorkerController() {
            return navigator.serviceWorker && navigator.serviceWorker.controller;
        }
    },
    async created() {
        await this.initialized;
        try {
            this.$_ServerPush_registerListener();
            await this.$_ServerPush_sendMessage({ type: "INIT" }, false);
            Object.values(this.mailboxes).forEach(mailbox => {
                this.$socket.register(`mailreplica.${mailbox.owner}.updated`, this.$_ServerPush_handle(mailbox));
            });
        } catch (error) {
            console.error("[SW] failed to init service worker", error);
        }
    },
    methods: {
        $_ServerPush_registerListener() {
            if (navigator.serviceWorker) {
                navigator.serviceWorker.addEventListener("message", event => {
                    if (event.data.type === "refresh") {
                        event.data.folderUids.forEach(folderUid => this.$_ServerPush_refreshUI(folderUid));
                    }
                });
            }
        },
        async $_ServerPush_sendMessage(message, skip, defaultResponse = null) {
            if (this.$_ServerPush_serviceWorkerController && !skip) {
                await this.$_ServerPush_serviceWorkerController.postMessage(message);
            } else if (defaultResponse) {
                await this.$_ServerPush_refreshUI(defaultResponse);
            }
        },
        $_ServerPush_handle(mailbox) {
            return async ({ data }) => {
                const message = { type: "SYNCHRONIZE", body: data.body };
                try {
                    await this.$_ServerPush_sendMessage(message, !mailbox.offlineSync, data.body.mailbox);
                } catch (error) {
                    console.error(`[SW] failed to send '${message}' to service worker`, error);
                }
            };
        },
        async $_ServerPush_refreshUI(folderUid) {
            if (folderUid in this.folders) {
                this.$bus.$emit("mail-webapp/unread_folder_count", this.folders[folderUid]);
            }
            if (folderUid === this.activeFolder) {
                this.$bus.$emit("mail-webapp/pushed_folder_changes", folderUid);
            }
        }
    }
};

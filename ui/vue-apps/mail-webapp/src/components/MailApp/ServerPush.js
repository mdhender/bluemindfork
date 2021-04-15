import { mapGetters, mapState } from "vuex";

import { MY_MAILBOX_KEY, MAILBOXES_ARE_LOADED } from "~getters";
import { WaitForMixin } from "~mixins";

export default {
    mixins: [WaitForMixin],
    computed: {
        ...mapState("mail", ["activeFolder", "folders", "mailboxes"]),
        ...mapGetters("mail", { MY_MAILBOX_KEY, MAILBOXES_ARE_LOADED }),
        $_ServerPush_serviceWorkerController() {
            return navigator.serviceWorker && navigator.serviceWorker.controller;
        }
    },
    async created() {
        try {
            this.$_ServerPush_registerListener();
            await this.$_ServerPush_sendMessage({ type: "INIT" }, false);
        } catch (error) {
            console.error("[SW] failed to init service worker", error);
        }
    },
    watch: {
        MAILBOXES_ARE_LOADED() {
            if (this.MAILBOXES_ARE_LOADED) {
                Object.values(this.mailboxes).forEach(mailbox => {
                    this.$socket.register(`mailreplica.${mailbox.owner}.updated`, this.$_ServerPush_handle(mailbox));
                });
            }
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
        async $_ServerPush_sendMessage(message, skipSync, defaultResponse = null) {
            if (this.$_ServerPush_serviceWorkerController && !skipSync) {
                await this.$_ServerPush_serviceWorkerController.postMessage(message);
            } else if (defaultResponse) {
                await this.$_ServerPush_refreshUI(defaultResponse);
            }
        },
        $_ServerPush_handle(mailbox) {
            return async ({ data }) => {
                const message = { type: "SYNCHRONIZE", body: data.body };
                try {
                    const isUserMailbox = mailbox.key === this.MY_MAILBOX_KEY;
                    const skipSync = !mailbox.offlineSync || !isUserMailbox;
                    await this.$_ServerPush_sendMessage(message, skipSync, data.body.mailbox);
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

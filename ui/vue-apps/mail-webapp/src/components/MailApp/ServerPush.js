import { mapGetters, mapState } from "vuex";
import throttle from "lodash.throttle";

import { MAILBOXES, MY_MAILBOX_KEY, MAILBOXES_ARE_LOADED } from "~/getters";
import { WaitForMixin } from "~/mixins";

export default {
    mixins: [WaitForMixin],
    data: () => ({ listenerRegistry: [] }),
    computed: {
        ...mapState("mail", ["activeFolder", "folders"]),
        ...mapGetters("mail", { MAILBOXES, MY_MAILBOX_KEY, MAILBOXES_ARE_LOADED }),
        $_ServerPush_serviceWorkerController() {
            return navigator.serviceWorker && navigator.serviceWorker.controller;
        }
    },
    async created() {
        try {
            if (navigator.serviceWorker) {
                navigator.serviceWorker.addEventListener("message", this.$_ServerPush_serviceWorkerListener);
                this.listenerRegistry.push(() =>
                    navigator.serviceWorker.removeEventListener("message", this.$_ServerPush_serviceWorkerListener);
                );
                this.$_ServerPush_serviceWorkerController?.postMessage({ type: "INIT" });
            }
            await this.$waitFor(MAILBOXES_ARE_LOADED);
            this.MAILBOXES.forEach(mailbox => {
                const callback = this.$_ServerPush_handle(mailbox);
                this.$socket.register(`mailreplica.${mailbox.owner}.updated`, callback);
                this.listenerRegistry.push(() =>
                    this.$socket.unregister(`mailreplica.${mailbox.owner}.updated`, callback)
                );
            });
        } catch (error) {
            // eslint-disable-next-line no-console
            console.error("[SW] failed to init service worker", error);
        }
    },
    beforeDestroy() {
        this.listenerRegistry.forEach(unregister => unregister());
    },
    methods: {
        $_ServerPush_serviceWorkerListener(event) {
            if (event.data.type === "refresh") {
                event.data.folderUids.forEach(folderUid => this.$_ServerPush_refreshUI(folderUid));
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
                    // eslint-disable-next-line no-console
                    console.error(`[SW] failed to send '${message}' to service worker`, error);
                }
            };
        },
        async $_ServerPush_refreshUI(folderUid) {
            if (folderUid in this.folders) {
                this.$bus.$emit("mail-webapp/unread_folder_count", this.folders[folderUid]);
            }
            if (folderUid === this.activeFolder) {
                throttledFolderChanges(folderUid, this.$bus);
            }
        }
    }
};

const throttledFolderChanges = throttle((folderUid, bus) => {
    bus.$emit("mail-webapp/pushed_folder_changes", folderUid);
}, 5000);

export default class ServerPushHandler {
    constructor(bus, mailState, serviceWorker) {
        this.bus = bus;
        this.mailState = mailState;
        this.serviceWorker = serviceWorker;
    }

    static async build(bus, mailState, serviceWorker) {
        const handler = new ServerPushHandler(bus, mailState, serviceWorker);
        await handler.initServiceWorkerSync();
        return handler;
    }

    async initServiceWorkerSync() {
        try {
            const updatedFolderUid = await this.sendMessage({ type: "INIT" }, false, []);
            updatedFolderUid.forEach(updated => {
                this.refreshUI({ body: { mailbox: updated } });
            });
        } catch (error) {
            console.error("[SW] failed to init service worker", error);
        }
    }

    handle(mailbox) {
        return async ({ data }) => {
            const message = { type: "SYNCHRONIZE", body: data.body };
            try {
                const updated = await this.sendMessage(message, !mailbox.offlineSync, true);
                if (!data.body.isHierarchy && updated) {
                    this.refreshUI(data);
                }
            } catch (error) {
                console.error(`[SW] failed to send '${message}' to service worker`, error);
            }
        };
    }

    async sendMessage(message, skip, defaultResponse) {
        return new Promise(resolve => {
            const messageChannel = new MessageChannel();
            if (!this.hasServiceWorkerController() || skip) {
                resolve(defaultResponse);
            } else {
                messageChannel.port1.onmessage = message => {
                    messageChannel.port1.close();
                    resolve(message.data);
                };
                this.serviceWorker.controller.postMessage(message, [messageChannel.port2]);
            }
        });
    }

    refreshUI(data) {
        if (data.body.mailbox in this.mailState.folders) {
            this.bus.$emit("mail-webapp/unread_folder_count", this.mailState.folders[data.body.mailbox]);
        }
        if (data.body.mailbox === this.mailState.activeFolder) {
            this.bus.$emit("mail-webapp/pushed_folder_changes", data);
        }
    }

    hasServiceWorkerController() {
        return this.serviceWorker && this.serviceWorker.controller;
    }
}

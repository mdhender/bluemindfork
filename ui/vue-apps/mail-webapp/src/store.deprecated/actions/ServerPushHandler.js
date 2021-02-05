export default class ServerPushHandler {
    constructor(bus, mailState, serviceWorker) {
        this.bus = bus;
        this.mailState = mailState;
        this.serviceWorker = serviceWorker;
        this.initServiceWorkerSync(bus, mailState);
    }

    initServiceWorkerSync() {
        if (this.hasServiceWorkerController()) {
            this.serviceWorker.controller.postMessage({ type: "INIT" });
        }
    }

    handle(mailbox) {
        return ({ data }) => {
            const promise = new Promise(resolve => {
                const messageChannel = new MessageChannel();
                if (!this.hasServiceWorkerController() || !mailbox.offlineSync) {
                    resolve(true);
                } else {
                    const message = { type: "SYNCHRONIZE", body: data.body };
                    messageChannel.port1.onmessage = message => {
                        messageChannel.port1.close();
                        resolve(message);
                    };
                    this.serviceWorker.controller.postMessage(message, [messageChannel.port2]);
                }
            });
            return promise.then(updated => {
                if (!data.body.isHierarchy && updated) {
                    this.refreshUI(data);
                }
            });
        };
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

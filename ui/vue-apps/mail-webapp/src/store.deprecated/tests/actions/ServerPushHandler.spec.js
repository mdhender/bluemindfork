import ServerPushHandler from "../../actions/ServerPushHandler";
window.MessageChannel = require("worker_threads").MessageChannel;

let bus;

const buildFakeServiceWorkerAcknowledgingUpdate = updated => {
    return {
        controller: {
            postMessage: (message, ports) => {
                if (message.type === "INIT") {
                    ports[0].postMessage([]);
                } else if (message.type === "SYNCHRONIZE") {
                    ports[0].postMessage(updated);
                }
            }
        }
    };
};

const mailboxWithOfflineSync = offlineSync => ({
    name: "test-mailbox",
    offlineSync: offlineSync
});

const mailStateWithActiveFolder = activeFolder => ({
    activeFolder,
    folders: {
        knownFolder: "with something inside"
    }
});

const serverEventOnKnownFolderIsHierarchy = isHierarchy => ({
    body: {
        isHierarchy,
        mailbox: "knownFolder"
    }
});

describe("[ServerPushHandler]", () => {
    beforeEach(() => {
        bus = {
            $emit: jest.fn()
        };
    });
    describe("[Constructor]", () => {
        test("Should setup service worker given one provided", async () => {
            const serviceWorker = buildFakeServiceWorkerAcknowledgingUpdate();
            const postMessageSpy = jest.spyOn(serviceWorker.controller, "postMessage");

            await ServerPushHandler.build(bus, null, serviceWorker);

            expect(postMessageSpy).toHaveBeenLastCalledWith({ type: "INIT" }, expect.anything());
        });
        test("Should not call service worker given none provided", async () => {
            await ServerPushHandler.build(bus, null, null);
        });
    });
    describe("[With Service Worker]", () => {
        test("Should be called and not refresh ui on hierarchy event", async () => {
            const mailbox = mailboxWithOfflineSync(true);
            const mailState = mailStateWithActiveFolder("knownFolder");
            const hierarchyEvent = serverEventOnKnownFolderIsHierarchy(true);

            const serviceWorker = buildFakeServiceWorkerAcknowledgingUpdate(false);
            const postMessageSpy = jest.spyOn(serviceWorker.controller, "postMessage");
            const handler = await ServerPushHandler.build(bus, mailState, serviceWorker);
            const refreshUISpy = jest.spyOn(handler, "refreshUI");

            await handler.handle(mailbox)({ data: hierarchyEvent });

            expect(postMessageSpy).toHaveBeenLastCalledWith(
                { type: "SYNCHRONIZE", ...hierarchyEvent },
                expect.anything()
            );
            expect(refreshUISpy).not.toHaveBeenCalled();
        });
        test("Should be called and refresh ui on folder event given offline sync and folder updated", async () => {
            const mailbox = mailboxWithOfflineSync(true);
            const mailState = mailStateWithActiveFolder("knownFolder");
            const folderEvent = serverEventOnKnownFolderIsHierarchy(false);

            const serviceWorker = buildFakeServiceWorkerAcknowledgingUpdate(true);
            const postMessageSpy = jest.spyOn(serviceWorker.controller, "postMessage");
            const handler = await ServerPushHandler.build(bus, mailState, serviceWorker);
            const refreshUISpy = jest.spyOn(handler, "refreshUI");

            await handler.handle(mailbox)({ data: folderEvent });

            expect(postMessageSpy).toHaveBeenLastCalledWith({ type: "SYNCHRONIZE", ...folderEvent }, expect.anything());
            expect(refreshUISpy).toHaveBeenLastCalledWith(folderEvent);
        });
        test("Should be called but not refresh ui on folder event given offline sync and folder not updated", async () => {
            const mailbox = mailboxWithOfflineSync(true);
            const mailState = mailStateWithActiveFolder("knownFolder");
            const folderEvent = serverEventOnKnownFolderIsHierarchy(false);

            const serviceWorker = buildFakeServiceWorkerAcknowledgingUpdate(false);
            const postMessageSpy = jest.spyOn(serviceWorker.controller, "postMessage");
            const handler = await ServerPushHandler.build(bus, mailState, serviceWorker);
            const refreshUISpy = jest.spyOn(handler, "refreshUI");

            await handler.handle(mailbox)({ data: folderEvent });

            expect(postMessageSpy).toHaveBeenLastCalledWith({ type: "SYNCHRONIZE", ...folderEvent }, expect.anything());
            expect(refreshUISpy).not.toHaveBeenLastCalledWith(folderEvent);
        });
        test("Should not be called on folder event given no offline sync", async () => {
            const mailbox = mailboxWithOfflineSync(false);
            const mailState = mailStateWithActiveFolder("knownFolder");
            const folderEvent = serverEventOnKnownFolderIsHierarchy(false);

            const serviceWorker = buildFakeServiceWorkerAcknowledgingUpdate(true);
            const postMessageSpy = jest.spyOn(serviceWorker.controller, "postMessage");
            const handler = await ServerPushHandler.build(bus, mailState, serviceWorker);
            const refreshUISpy = jest.spyOn(handler, "refreshUI");

            await handler.handle(mailbox)({ data: folderEvent });

            expect(postMessageSpy).toHaveBeenCalledWith({ type: "INIT" }, expect.anything());
            expect(postMessageSpy).not.toHaveBeenLastCalledWith(
                { type: "SYNCHRONIZE", ...folderEvent },
                expect.anything()
            );
            expect(refreshUISpy).toHaveBeenLastCalledWith(folderEvent);
        });
    });
    describe("[Without Service Worker]", () => {
        test("Should not refresh ui on hierarchy event", async () => {
            const mailbox = mailboxWithOfflineSync(true);
            const mailState = mailStateWithActiveFolder("knownFolder");
            const hierarchyEvent = serverEventOnKnownFolderIsHierarchy(true);

            const handler = await ServerPushHandler.build(bus, mailState, null);
            const refreshUISpy = jest.spyOn(handler, "refreshUI");

            await handler.handle(mailbox)({ data: hierarchyEvent });

            expect(refreshUISpy).not.toHaveBeenCalled();
        });
        test("Should refresh ui on folder event given offline sync", async () => {
            const mailbox = mailboxWithOfflineSync(true);
            const mailState = mailStateWithActiveFolder("knownFolder");
            const folderEvent = serverEventOnKnownFolderIsHierarchy(false);

            const handler = await ServerPushHandler.build(bus, mailState, null);
            const refreshUISpy = jest.spyOn(handler, "refreshUI");

            await handler.handle(mailbox)({ data: folderEvent });

            expect(refreshUISpy).toHaveBeenLastCalledWith(folderEvent);
        });
        test("Should refresh ui on folder event given no offline sync", async () => {
            const mailbox = mailboxWithOfflineSync(false);
            const mailState = mailStateWithActiveFolder("knownFolder");
            const folderEvent = serverEventOnKnownFolderIsHierarchy(false);

            const handler = await ServerPushHandler.build(bus, mailState, null);
            const refreshUISpy = jest.spyOn(handler, "refreshUI");

            await handler.handle(mailbox)({ data: folderEvent });

            expect(refreshUISpy).toHaveBeenLastCalledWith(folderEvent);
        });
    });
    describe("[Refresh UI]", () => {
        test("Should refresh folder and unread count given folder active and known in state", async () => {
            const mailState = mailStateWithActiveFolder("knownFolder");
            const folderEvent = serverEventOnKnownFolderIsHierarchy(false);

            const handler = await ServerPushHandler.build(bus, mailState, null);
            await handler.refreshUI(folderEvent);

            expect(bus.$emit).toHaveBeenCalledWith("mail-webapp/unread_folder_count", "with something inside");
            expect(bus.$emit).toHaveBeenCalledWith("mail-webapp/pushed_folder_changes", folderEvent);
        });
        test("Should not refresh folder given folder not active", async () => {
            const mailState = mailStateWithActiveFolder("anotherFolder");
            const folderEvent = serverEventOnKnownFolderIsHierarchy(false);

            const handler = await ServerPushHandler.build(bus, mailState, null);
            await handler.refreshUI(folderEvent);

            expect(bus.$emit).toHaveBeenCalledWith("mail-webapp/unread_folder_count", "with something inside");
            expect(bus.$emit).not.toHaveBeenCalledWith("mail-webapp/pushed_folder_changes", expect.anything());
        });
        test("Should not refresh unread count given unknown folder in state", async () => {
            const mailState = mailStateWithActiveFolder("unknownFolder");
            const folderEvent = serverEventOnKnownFolderIsHierarchy(false);
            folderEvent.body.mailbox = "unknownFolder";

            const handler = await ServerPushHandler.build(bus, mailState, null);
            await handler.refreshUI(folderEvent);

            expect(bus.$emit).not.toHaveBeenCalledWith("mail-webapp/unread_folder_count", expect.anything());
            expect(bus.$emit).toHaveBeenCalledWith("mail-webapp/pushed_folder_changes", folderEvent);
        });
    });
});

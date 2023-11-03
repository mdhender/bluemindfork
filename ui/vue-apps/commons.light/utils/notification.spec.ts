import { getNotifications, isAvailable, requestPermission, showNotification } from "./notification";
import { getRegistration, isServiceWorkerContext } from "./service-worker";

jest.mock("./service-worker", () => ({
    getRegistration: jest.fn().mockResolvedValue({
        getNotifications: jest.fn().mockResolvedValue({}),
        showNotification: jest.fn().mockResolvedValue({})
    }),
    isServiceWorkerContext: jest.fn().mockReturnValue(false)
}));

describe("notification", () => {
    const Notification = { requestPermission: jest.fn().mockResolvedValue("granted") };

    beforeEach(() => {
        Object.defineProperty(self, "Notification", { value: jest.fn(), configurable: true });
        Object.assign(self.Notification, Notification);
        Object.assign(self.Notification, { permission: "granted" });
        jest.clearAllMocks();
    });

    describe("isAvailable", () => {
        test("Notification is available if Notification is present in global", async () => {
            expect(isAvailable()).toBeTruthy();
            delete (self as Partial<typeof globalThis>).Notification;
            expect(isAvailable()).toBeFalsy();
        });
        test("Notification is available in service worker context", () => {
            delete (self as Partial<typeof globalThis>).Notification;
            (isServiceWorkerContext as jest.Mock).mockReturnValueOnce(true);
            expect(isAvailable()).toBeTruthy();
        });
    });
    describe("requestPermission", () => {
        test("requestPermission return granted without calling system method if permission is granted", async () => {
            const permission = await requestPermission();
            expect(Notification.requestPermission).not.toHaveBeenCalled();
            expect(permission).toEqual("granted");
        });
        test("requestPermission return granted without calling system method if permission is denied", async () => {
            Object.assign(self.Notification, { permission: "denied" });
            const permission = await requestPermission();
            expect(Notification.requestPermission).not.toHaveBeenCalled();
            expect(permission).toEqual("denied");
        });
        test("requestPermission call system method if permission is not set", async () => {
            Object.assign(self.Notification, { permission: "default" });
            const permission = await requestPermission();
            expect(Notification.requestPermission).toHaveBeenCalled();
            expect(permission).toEqual("granted");
        });
        test("In ServiceWorker, since it's impossible to know, it's assumed that permission is granted", async () => {
            (isServiceWorkerContext as jest.Mock).mockReturnValueOnce(true);
            Object.assign(self.Notification, { permission: "denied" });
            const permission = await requestPermission();
            expect(permission).toEqual("granted");
        });
    });
    describe("showNotification", () => {
        test("showNotification use serviceworker showNotification if available", async () => {
            const title = "title",
                options = {};
            await showNotification(title, options);
            expect((await getRegistration()).showNotification).toHaveBeenCalledWith(title, options);
        });
        test("showNotification use native Noficication if service worker is not available", async () => {
            const title = "title",
                options = {};
            (getRegistration as jest.Mock).mockResolvedValueOnce(undefined);
            await showNotification(title, options);
            expect((await getRegistration()).showNotification).not.toHaveBeenCalled();
            expect(self.Notification).toHaveBeenCalledWith(title, options);
        });
        test("showNotification call requestPermission if permission is not granted", async () => {
            Object.assign(self.Notification, { permission: "default" });
            await showNotification("");
            expect(Notification.requestPermission).toHaveBeenCalled();
            expect((await getRegistration()).showNotification).toHaveBeenCalled();
        });
        test("showNotification do nothing if permission is not granted", async () => {
            Object.assign(self.Notification, { permission: "denied" });
            await showNotification("");
            expect((await getRegistration()).showNotification).not.toHaveBeenCalled();
            expect(self.Notification).not.toHaveBeenCalled();
        });
    });
    describe("getNotifications", () => {
        test("getNotifications use serviceworker showNotification if available", async () => {
            const options = {},
                notifications = ["notifications..."];
            ((await getRegistration()).getNotifications as jest.Mock).mockResolvedValueOnce(notifications);
            const returnValue = await getNotifications(options);
            expect((await getRegistration()).getNotifications).toHaveBeenCalledWith(options);
            expect(returnValue).toBe(notifications);
        });
        test("getNotifications return empty array if serviceworker not available", async () => {
            (getRegistration as jest.Mock).mockResolvedValueOnce(undefined);
            const returnValue = await getNotifications();
            expect((await getRegistration()).getNotifications).not.toHaveBeenCalled();
            expect(returnValue).toEqual([]);
        });
    });
});

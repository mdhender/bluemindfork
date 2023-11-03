import { getRegistration, isAvailable, isServiceWorkerContext, isSupported } from "./service-worker";

describe("service-worker", () => {
    const navigator = { ...self.navigator, serviceWorker: {} };
    Object.defineProperty(self, "navigator", { value: navigator });
    beforeEach(() => {
        navigator.serviceWorker = {};
    });
    describe("isServiceWorkerContext", () => {
        test("Context is in service worker if ServiceWorkerGlobalScope is defined", () => {
            self.ServiceWorkerGlobalScope = true;
            expect(isServiceWorkerContext()).toBeTruthy();
            delete self.ServiceWorkerGlobalScope;
            expect(isServiceWorkerContext()).toBeFalsy();
        });
    });
    describe("isSupported", () => {
        test("Service worker is supported if navigator.serviceWorker is defined", () => {
            expect(isSupported()).toBeTruthy();
            delete navigator.serviceWorker;
            expect(isSupported()).toBeFalsy();
        });
        test("Service worker is supported in ServiceWorkerGlobalScope", () => {
            delete navigator.serviceWorker;
            self.ServiceWorkerGlobalScope = true;
            expect(isSupported()).toBeTruthy();
            delete self.ServiceWorkerGlobalScope;
        });
    });
    describe("isAvailable", () => {
        test("Service worker is never available if serviceWorker is not supported", async () => {
            delete navigator.serviceWorker;
            await expect(isAvailable()).resolves.toBeFalsy();
        });
        test("Service worker is available if navigator.serviceWorker is ready and controller is set", async () => {
            navigator.serviceWorker.ready = Promise.resolve();
            navigator.serviceWorker.controller = {};
            await expect(isAvailable()).resolves.toBeTruthy();
            navigator.serviceWorker.controller = null;
            await expect(isAvailable()).resolves.toBeFalsy();
        });
        test("Service worker is always available in ServiceWorkerGlobalScope", async () => {
            navigator.serviceWorker = undefined;
            self.ServiceWorkerGlobalScope = true;
            await expect(isAvailable()).resolves.toBeTruthy();
            delete self.ServiceWorkerGlobalScope;
        });
    });
    describe("getRegistration", () => {
        test("getRegistration throw an exception if service worker is not supported", async () => {
            delete navigator.serviceWorker;
            await expect(getRegistration()).rejects.toBe("Service worker is not supported");
        });
        test("getRegistration throw an exception if service worker is not available", async () => {
            navigator.serviceWorker.ready = Promise.resolve();
            navigator.serviceWorker.controller = null;
            await expect(getRegistration()).rejects.toBe("Service worker is not available");
        });
        test("getRegistration return ready's registration is service worker is ready", async () => {
            const registration = {};
            navigator.serviceWorker.ready = Promise.resolve(registration);
            navigator.serviceWorker.controller = true;
            await expect(getRegistration()).resolves.toBe(registration);
        });
        test("getRegistration return global registration in ServiceWorkerGlobalScope", async () => {
            self.ServiceWorkerGlobalScope = true;
            self.registration = {};
            await expect(getRegistration()).resolves.toBe(self.registration);
            delete self.ServiceWorkerGlobalScope;
            delete self.registration;
        });
    });
});

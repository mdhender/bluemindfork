declare const self: ServiceWorkerGlobalScope | Window;
const window = self as Window;
const serviceWorker = self as ServiceWorkerGlobalScope;

export function isServiceWorkerContext() {
    return "ServiceWorkerGlobalScope" in self;
}
export function isSupported() {
    return isServiceWorkerContext() ? true : "serviceWorker" in self.navigator;
}

export async function isAvailable(): Promise<boolean> {
    if (isServiceWorkerContext()) {
        return true;
    } else if (isSupported()) {
        await window.navigator.serviceWorker.ready;
        return Boolean(window.navigator.serviceWorker.controller);
    }
    return false;
}

export async function getRegistration(): Promise<ServiceWorkerRegistration> {
    if (isServiceWorkerContext()) {
        return serviceWorker.registration;
    } else if (isSupported()) {
        const registration = await window.navigator.serviceWorker.ready;
        if (window.navigator.serviceWorker.controller) {
            return registration;
        } else {
            throw "Service worker is not available";
        }
    }
    throw "Service worker is not supported";
}

export default { isAvailable, getRegistration, isServiceWorkerContext, isSupported };

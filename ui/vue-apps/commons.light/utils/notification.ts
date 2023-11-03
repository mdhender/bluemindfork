import { getRegistration, isServiceWorkerContext } from "./service-worker";

/** Check if Notification API is availble. */
export function isAvailable() {
    return isServiceWorkerContext() ? true : "Notification" in self;
}

/** [MDN Reference](https://developer.mozilla.org/docs/Web/API/Notification/requestPermission_static) */
export function requestPermission(): Promise<NotificationPermission> {
    if (isServiceWorkerContext()) {
        return Promise.resolve("granted");
    } else if (isAvailable()) {
        if (Notification.permission !== "denied" && Notification.permission !== "granted") {
            return Notification.requestPermission();
        } else {
            return Promise.resolve(Notification.permission);
        }
    }
    console.error("Notification not supported");
    return Promise.resolve("denied");
}
/** [MDN Reference](https://developer.mozilla.org/docs/Web/API/ServiceWorkergetRegistration()/showNotification) */
export async function showNotification(title: string, options?: NotificationOptions): Promise<void> {
    const permission = await requestPermission();
    if (permission === "granted") {
        try {
            (await getRegistration()).showNotification(title, options);
        } catch (e) {
            console.error("Permanent Notification not supported, no service worker", e);
            new Notification(title, options);
        }
    }
}
/** [MDN Reference](https://developer.mozilla.org/docs/Web/API/ServiceWorkergetRegistration()/getNotifications) */
export async function getNotifications(filter?: GetNotificationOptions): Promise<Notification[]> {
    try {
        return (await getRegistration()).getNotifications(filter);
    } catch (e) {
        console.error("Permanent Notification not supported, no service worker", e);
        return [];
    }
}

export default { getNotifications, requestPermission, showNotification };

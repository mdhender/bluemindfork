declare const self: ServiceWorkerGlobalScope;

let lastClickedNotification: string;
export function onMailNotificationClick(event: NotificationEvent) {
    event.notification.close();
    if (lastClickedNotification !== event.notification.data.internalId) {
        lastClickedNotification = event.notification.data.internalId;
        event.waitUntil(openMail(event.notification));
    }
}

async function openMail(notification: Notification): Promise<WindowClient | null> {
    const clients = await self.clients.matchAll({ includeUncontrolled: false, type: "window" });
    const mailApp = clients.find(client => /\/webapp\/mail\//.test(client.url)) as WindowClient | undefined;
    const url = `${self.origin}/webapp/mail/.t/${notification.data.internalId}`;
    if (mailApp) {
        await mailApp.focus();
        return mailApp.navigate(url);
    } else {
        return self.clients.openWindow(url);
    }
}

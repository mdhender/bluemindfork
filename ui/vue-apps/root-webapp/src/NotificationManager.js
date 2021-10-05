import global from "@bluemind/global";
import router from "@bluemind/router";
import BmRoles from "@bluemind/roles";
import WebsocketClient from "@bluemind/sockjs";
import debounce from "lodash.debounce";

export default class NotificationManager {
    constructor() {
        this.isAvailable = "Notification" in window;
    }

    hasPermission() {
        return Notification.permission === "granted";
    }

    userAlreadyAnswered() {
        return Notification.permission === "denied" || this.hasPermission();
    }

    requestPermissionIfNeeded() {
        if (!this.userAlreadyAnswered() && !this.hasPermission()) {
            return Notification.requestPermission();
        }
        return Promise.resolve();
    }

    send(title, body, icon, onClickHandler) {
        if (this.isAvailable) {
            const notification = new Notification(title, { icon, body, image: icon, badge: icon });
            notification.onclick = params => {
                if (this.hasPermission()) {
                    onClickHandler(params);
                }
            };
        }
    }

    async setNotificationWhenReceivingMail(userSession) {
        global.hasNotifWhenReceivingMail = true;
        //TODO: The whole think should be provider by an extension.
        if (userSession.roles.includes(BmRoles.HAS_MAIL)) {
            await this.requestPermissionIfNeeded();
            const mailAppExtension = window.bmExtensions_["webapp.banner"].find(
                ({ bundle }) => bundle === "net.bluemind.webapp.mail.js"
            );
            if (!mailAppExtension) {
                return;
            }
            const mailIconAsSvg = mailAppExtension.application.children.icon.children.svg.body;
            const mailIconAsBlobURL = URL.createObjectURL(new Blob([mailIconAsSvg], { type: "image/svg+xml" }));

            const address = userSession.userId + ".notifications.mails";

            const sendNotification = ({ data }) => {
                const onNotifClick = () => {
                    const mailAppPath = mailAppExtension.application.href;
                    const mailAppRootPath = mailAppPath.split("/").filter(Boolean)[0];
                    const rootPath = new URL(document.baseURI).pathname.split("/").filter(Boolean)[0];
                    if (rootPath === mailAppRootPath) {
                        router.push({ name: "mail:conversation", params: { conversationpath: data.internalId } });
                    } else {
                        const origin = document.location.origin;
                        const conversationPath = `.t/${data.internalId}`;
                        document.location.href = `${origin}${mailAppPath}${conversationPath}`;
                    }
                };

                this.send(data.sender, data.subject, mailIconAsBlobURL, onNotifClick);
            };

            new WebsocketClient().register(
                address,
                this.debounceReduce(sendNotification, 500, this.reduceNotification)
            );
        }
    }

    // from https://github.com/jashkenas/underscore/issues/310
    debounceReduce(func, wait, reduce) {
        let allArgs;
        const wrapper = debounce(() => {
            let args = allArgs;
            allArgs = undefined;
            func(args);
        }, wait);
        return (...args) => {
            allArgs = reduce(allArgs, [...args]);
            wrapper();
        };
    }

    reduceNotification(accumulator, lastCallArgs) {
        accumulator = accumulator || { count: 0 };
        accumulator.count += 1;
        accumulator.data = {
            internalId: lastCallArgs[0].data.body.internalId,
            subject: lastCallArgs[0].data.body.body,
            sender: lastCallArgs[0].data.body.title
        };
        return accumulator;
    }
}

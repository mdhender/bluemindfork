import ItemUri from "@bluemind/item-uri";
import router from "@bluemind/router";
import store from "@bluemind/store";
import WebsocketClient from "@bluemind/sockjs";
import debounce from "lodash.debounce";

export default class NotificationManager {
    constructor() {
        this.isAvailable = "Notification" in window;
        if (this.isAvailable) {
            this._init();
        }
    }

    // FIXME: cant use ES6 private class method because of https://github.com/babel/babel/issues/10752 so use convention name "_"
    _init() {
        this.hasPermission = Notification.permission === "granted";
        this.userAlreadyAnswered = Notification.permission === "denied" || this.hasPermission;
    }

    requestPermissionIfNeeded() {
        if (!this.userAlreadyAnswered && !this.hasPermission) {
            return Notification.requestPermission().then(() => this._init());
        }
        return Promise.resolve();
    }

    send(title, body, icon, onClickHandler) {
        if (this.isAvailable && this.hasPermission) {
            const notification = new Notification(title, { icon, body, image: icon, badge: icon });
            notification.onclick = onClickHandler;
        }
    }

    async setNotificationWhenReceivingMail(userSession) {
        if (userSession.roles.includes("hasMail")) {
            await this.requestPermissionIfNeeded();
            const mailAppExtension = window.bmExtensions_["net.bluemind.banner"].find(
                extension => extension.application.role === "hasMail" && extension.application.href.includes("mail")
            );
            const mailIconAsSvg = mailAppExtension.application.children["icon-svg"].body;
            const mailIconAsBlobURL = URL.createObjectURL(new Blob([mailIconAsSvg], { type: "image/svg+xml" }));

            const address = userSession.userId + ".notifications.mails";

            const sendNotification = ({ data }) => {
                const onNotifClick = () => {
                    // FIXME : will not work if mail store or mail routes are not init
                    const key = ItemUri.encode(Number(data.internalId), store.getters["mail/MY_INBOX"].key);
                    router.push({ name: "mail:message", params: { message: key } });
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

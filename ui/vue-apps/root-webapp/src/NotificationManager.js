export default class NotificationManager {
    constructor() {
        this.isAvailable = "Notification" in window;
        if (this.isAvailable) {
            this._init();
        }
        if (!this.userAlreadyAnswered) {
            Notification.requestPermission(
                function() {
                    this._init();
                }.bind(this)
            );
        }
    }

    // FIXME: cant use ES6 private class method because of https://github.com/babel/babel/issues/10752 so use convention name "_"
    _init() {
        this.hasPermission = Notification.permission === "granted";
        this.userAlreadyAnswered = Notification.permission === "denied" || this.hasPermission;
    }

    send(title, body, icon) {
        if (this.isAvailable && this.hasPermission) {
            new Notification(title, { icon, body, image: icon, badge: icon });
        }
    }
}

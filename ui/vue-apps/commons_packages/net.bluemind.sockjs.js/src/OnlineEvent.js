export default class OnlineEvent extends Event {
    constructor(state) {
        super(OnlineEvent.TYPE);
        this.online = state;
    }
}

OnlineEvent.TYPE = "online";

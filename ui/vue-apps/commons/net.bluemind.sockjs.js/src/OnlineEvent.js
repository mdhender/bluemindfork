import { Event } from "@bluemind/event";

export default class OnlineEvent extends Event {
    constructor(state) {
        super(OnlineEvent.TYPE);
        this.online = state;
    }
}

OnlineEvent.TYPE = "online";
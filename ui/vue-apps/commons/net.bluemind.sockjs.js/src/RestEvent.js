import { Event } from "@bluemind/event";

export default class RestEvent extends Event {
    constructor(type, data) {
        super(type);
        if (data instanceof RestEvent) {
            this.data = data.data;
            this.originalEvent = data;
        } else {
            this.data = data;
        }
    }

    static disconnected(requestId) {
        const data = {
            body: {
                requestId: requestId,
                errorCode: "FAILURE",
                errorType: "ClientFault",
                message: "WebSocket is not available"
            },
            statusCode: 408
        };
        return new RestEvent(requestId, data);
    }
}

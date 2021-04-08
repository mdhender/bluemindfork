import MessagePathParam from "./MessagePathParam";
import MessageQueryParam from "./MessageQueryParam";

export default [
    {
        name: "v:mail:message",
        redirect(to) {
            let messagequery = MessageQueryParam.build(to.params.messagequery, to.params);
            let messagepath = MessagePathParam.build(to.params.messagepath, to.params.message);
            let params = messagequery ? { messagequery } : {};
            if (messagepath) {
                return { name: "mail:message", params: { ...params, messagepath } };
            } else {
                return { name: "mail:home", params };
            }
        }
    },
    {
        name: "v:mail:home",
        redirect(to) {
            let messagequery = MessageQueryParam.build(to.params.messagequery, to.params);
            return {
                name: "mail:home",
                params: messagequery ? { messagequery } : {}
            };
        }
    }
];

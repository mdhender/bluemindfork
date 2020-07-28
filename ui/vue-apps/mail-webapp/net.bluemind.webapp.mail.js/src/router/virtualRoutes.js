import MessageQueryParam from "./MessageQueryParam";
import ItemUri from "@bluemind/item-uri";

export default [
    {
        name: "v:mail:message",
        redirect(to) {
            let messagequery = MessageQueryParam.build(to.params.messagequery, to.params);
            let params = Object.assign({}, to.params, messagequery ? { messagequery } : {});
            if (params.message && ItemUri.isItemUri(params.message)) {
                params.message = ItemUri.item(params.message);
                return { name: "mail:message", params };
            } else if (params.message) {
                return { name: "mail:message", params };
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

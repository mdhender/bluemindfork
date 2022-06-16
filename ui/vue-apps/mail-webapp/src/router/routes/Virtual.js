import MessagePathParam from "../MessagePathParam";
import MessageQueryParam from "../MessageQueryParam";
import ConversationPathParam from "../ConversationPathParam";

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
        name: "v:mail:conversation",
        redirect(to) {
            let messagequery = MessageQueryParam.build(to.params.messagequery, to.params);
            let conversationpath = ConversationPathParam.build(
                to.params.messagepath,
                to.params.conversation,
                to.params.action,
                to.params.related
            );
            let params = messagequery ? { messagequery } : {};
            if (conversationpath) {
                return {
                    name: "mail:conversation",
                    params: { ...params, conversationpath }
                };
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
].map(route => Object.assign(route, { path: "" }));

import { mapGetters, mapState } from "vuex";

import { MY_DRAFTS } from "~/getters";
import { MessageCreationModes } from "~/model/message";
import { draftPath } from "~/model/draft";
import MessagePathParam from "~/router/MessagePathParam";

export default {
    computed: {
        ...mapGetters("mail", { MY_DRAFTS }),
        ...mapState("mail", ["activeFolder", "folders"]),
        ...mapState("session", { settings: ({ settings }) => settings.remote }),
        $_ReplyAndForwardRoutesMixin_conversationsActivated() {
            return this.settings.mail_thread === "true" && this.folders[this.activeFolder].allowConversations;
        }
    },
    methods: {
        reply(conversation, message) {
            this.$_ReplyAndForwardRoutesMixin_goTo(MessageCreationModes.REPLY, message, conversation);
        },
        replyAll(conversation, message) {
            this.$_ReplyAndForwardRoutesMixin_goTo(MessageCreationModes.REPLY_ALL, message, conversation);
        },
        forward(message) {
            this.$_ReplyAndForwardRoutesMixin_goTo(MessageCreationModes.FORWARD, message);
        },
        $_ReplyAndForwardRoutesMixin_goTo(action, message, conversation) {
            if (this.$_ReplyAndForwardRoutesMixin_mustRedirectToConversation(action)) {
                this.$router.navigate({
                    name: "v:mail:conversation",
                    params: { conversation, action, related: message }
                });
            } else {
                const messagepath = draftPath(this.MY_DRAFTS);
                const query = { action, message: MessagePathParam.build("", message) };
                this.$router.navigate({ name: "mail:message", params: { messagepath }, query });
            }
        },
        $_ReplyAndForwardRoutesMixin_mustRedirectToConversation(action) {
            return (
                this.$_ReplyAndForwardRoutesMixin_conversationsActivated &&
                (action === MessageCreationModes.REPLY_ALL || action === MessageCreationModes.REPLY)
            );
        }
    }
};

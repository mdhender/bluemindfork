import { mapGetters } from "vuex";
import { messageUtils } from "@bluemind/mail";
import { CONVERSATIONS_ACTIVATED, MY_DRAFTS } from "~/getters";
import MessagePathParam from "~/router/MessagePathParam";
import { DraftMixin, ComposerInitMixin, MailRoutesMixin } from "~/mixins";

const { MessageCreationModes } = messageUtils;

export default {
    mixins: [DraftMixin, ComposerInitMixin, MailRoutesMixin],
    computed: {
        ...mapGetters("mail", {
            $_ReplyAndForwardRoutesMixin_CONVERSATIONS_ACTIVATED: CONVERSATIONS_ACTIVATED,
            MY_DRAFTS
        })
    },
    methods: {
        reply(message, conversation) {
            this.$_ReplyAndForwardRoutesMixin_goTo(MessageCreationModes.REPLY, message, conversation);
        },
        replyAll(message, conversation) {
            this.$_ReplyAndForwardRoutesMixin_goTo(MessageCreationModes.REPLY_ALL, message, conversation);
        },
        forward(message) {
            this.$router.push(this.$_ReplyAndForwardRoutesMixin_route(MessageCreationModes.FORWARD, message));
        },
        replyRoute(message) {
            return this.$_ReplyAndForwardRoutesMixin_route(MessageCreationModes.REPLY, message);
        },
        replyAllRoute(message) {
            return this.$_ReplyAndForwardRoutesMixin_route(MessageCreationModes.REPLY_ALL, message);
        },
        forwardRoute(message) {
            return this.$_ReplyAndForwardRoutesMixin_route(MessageCreationModes.FORWARD, message);
        },
        async $_ReplyAndForwardRoutesMixin_goTo(action, related, conversation) {
            if (conversation && this.$_ReplyAndForwardRoutesMixin_CONVERSATIONS_ACTIVATED) {
                await this.saveAndCloseOpenDrafts(conversation);
                this.initRelatedMessage(action, {
                    internalId: related.remoteRef.internalId,
                    folderKey: related.folderRef.key
                });
            } else {
                this.$router.push(this.$_ReplyAndForwardRoutesMixin_route(action, related));
            }
        },
        $_ReplyAndForwardRoutesMixin_route(action, message) {
            const messagepath = this.draftPath(this.MY_DRAFTS);
            const query = { action, message: MessagePathParam.build("", message) };
            return this.$router.relative({ name: "mail:message", params: { messagepath }, query });
        }
    }
};

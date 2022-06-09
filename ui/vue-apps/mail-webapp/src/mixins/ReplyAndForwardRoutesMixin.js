import { mapGetters } from "vuex";
import { message } from "@bluemind/mail";
import { CONVERSATIONS_ACTIVATED, MY_DRAFTS } from "~/getters";
import MessagePathParam from "~/router/MessagePathParam";
import { DraftMixin, ComposerInitMixin, MailRoutesMixin } from "~/mixins";

const { MessageCreationModes } = message;

export default {
    mixins: [DraftMixin, ComposerInitMixin, MailRoutesMixin],
    computed: {
        ...mapGetters("mail", {
            $_ReplyAndForwardRoutesMixin_CONVERSATIONS_ACTIVATED: CONVERSATIONS_ACTIVATED,
            MY_DRAFTS
        })
    },
    methods: {
        async reply(conversation, message) {
            if (this.$_ReplyAndForwardRoutesMixin_CONVERSATIONS_ACTIVATED) {
                await this.saveAndCloseOpenDrafts(conversation);
            }
            this.$_ReplyAndForwardRoutesMixin_goTo(MessageCreationModes.REPLY, message);
        },
        async replyAll(conversation, message) {
            if (this.$_ReplyAndForwardRoutesMixin_CONVERSATIONS_ACTIVATED) {
                await this.saveAndCloseOpenDrafts(conversation);
            }
            this.$_ReplyAndForwardRoutesMixin_goTo(MessageCreationModes.REPLY_ALL, message);
        },
        forward(message) {
            this.$_ReplyAndForwardRoutesMixin_goTo(MessageCreationModes.FORWARD, message);
        },
        $_ReplyAndForwardRoutesMixin_goTo(action, related) {
            if (this.$_ReplyAndForwardRoutesMixin_CONVERSATIONS_ACTIVATED && action !== MessageCreationModes.FORWARD) {
                this.initRelatedMessage(action, {
                    internalId: related.remoteRef.internalId,
                    folderKey: related.folderRef.key
                });
            } else {
                const messagepath = this.draftPath(this.MY_DRAFTS);
                const query = { action, message: MessagePathParam.build("", related) };
                this.$router.navigate({ name: "mail:message", params: { messagepath }, query });
            }
        }
    }
};

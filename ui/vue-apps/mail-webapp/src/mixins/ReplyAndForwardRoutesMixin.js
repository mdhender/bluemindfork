import { mapGetters, mapState } from "vuex";
import { MY_DRAFTS } from "~/getters";
import { MessageCreationModes } from "~/model/message";
import { draftPath } from "~/model/draft";
import MessagePathParam from "~/router/MessagePathParam";
import { DraftMixin, ComposerInitMixin } from "~/mixins";

export default {
    mixins: [DraftMixin, ComposerInitMixin],
    computed: {
        ...mapGetters("mail", { MY_DRAFTS }),
        ...mapState("mail", ["activeFolder", "folders"]),
        ...mapState("session", { settings: ({ settings }) => settings.remote }),
        $_ReplyAndForwardRoutesMixin_conversationsActivated() {
            return this.settings.mail_thread === "true" && this.folders[this.activeFolder].allowConversations;
        }
    },
    methods: {
        async reply(conversation, message) {
            await this.saveAndCloseOpenDrafts(conversation);
            this.$_ReplyAndForwardRoutesMixin_goTo(MessageCreationModes.REPLY, message);
        },
        async replyAll(conversation, message) {
            await this.saveAndCloseOpenDrafts(conversation);
            this.$_ReplyAndForwardRoutesMixin_goTo(MessageCreationModes.REPLY_ALL, message);
        },
        forward(message) {
            this.$_ReplyAndForwardRoutesMixin_goTo(MessageCreationModes.FORWARD, message);
        },
        $_ReplyAndForwardRoutesMixin_goTo(action, related) {
            if (this.$_ReplyAndForwardRoutesMixin_conversationsActivated && action !== MessageCreationModes.FORWARD) {
                this.initRelatedMessage(action, {
                    internalId: related.remoteRef.internalId,
                    folderKey: related.folderRef.key
                });
            } else {
                const messagepath = draftPath(this.MY_DRAFTS);
                const query = { action, message: MessagePathParam.build("", related) };
                this.$router.navigate({ name: "mail:message", params: { messagepath }, query });
            }
        }
    }
};

<template>
    <mail-conversation-panel class="mail-route-conversation" />
</template>

<script>
import ConversationPathParam from "~/router/ConversationPathParam";
import { LoadingStatus } from "~/model/loading-status";
import { MessageCreationModes } from "~/model/message";
import {
    RESET_ACTIVE_MESSAGE,
    RESET_PARTS_DATA,
    SET_ACTIVE_MESSAGE,
    SET_CURRENT_CONVERSATION,
    UNSELECT_ALL_CONVERSATIONS,
    UNSET_CURRENT_CONVERSATION
} from "~/mutations";
import { MY_MAILBOX, SELECTION_IS_EMPTY } from "~/getters";
import { FETCH_CONVERSATION_IF_NOT_LOADED, FETCH_MESSAGE_METADATA } from "~/actions";
import { mapActions, mapGetters, mapMutations, mapState } from "vuex";
import MailConversationPanel from "./MailThread/MailConversationPanel";
import { WaitForMixin, ComposerInitMixin } from "~/mixins";

export default {
    name: "MailRouteConversation",
    components: { MailConversationPanel },
    mixins: [ComposerInitMixin, WaitForMixin],
    computed: {
        ...mapState("mail", ["activeFolder", "folders"]),
        ...mapState("session", { settings: ({ settings }) => settings.remote }),
        ...mapGetters("mail", { MY_MAILBOX, SELECTION_IS_EMPTY })
    },
    watch: {
        "$route.params.conversationpath": {
            async handler(conversationPath) {
                try {
                    this.RESET_PARTS_DATA();
                    this.RESET_ACTIVE_MESSAGE();
                    this.UNSET_CURRENT_CONVERSATION();
                    let assert = mailbox => mailbox && mailbox.loading === LoadingStatus.LOADED;
                    await this.$waitFor(MY_MAILBOX, assert);

                    const { folderKey, internalId, action, relatedFolderKey, relatedId } = ConversationPathParam.parse(
                        conversationPath,
                        this.activeFolder
                    );

                    if (!this.SELECTION_IS_EMPTY) {
                        this.UNSELECT_ALL_CONVERSATIONS();
                    }

                    switch (action) {
                        case MessageCreationModes.REPLY:
                        case MessageCreationModes.REPLY_ALL:
                        case MessageCreationModes.FORWARD:
                            await this.initRelatedMessage(action, {
                                internalId: relatedId,
                                folderKey: relatedFolderKey
                            });
                            break;
                        case MessageCreationModes.NEW:
                            this.initNewMessage();
                            break;
                        default:
                            break;
                    }

                    let conversation;
                    const folder = this.folders[folderKey];
                    const conversationsActivated = this.settings.mail_thread === "true" && folder.allowConversations;
                    conversation = await this.FETCH_CONVERSATION_IF_NOT_LOADED({
                        uid: internalId,
                        folder,
                        conversationsActivated
                    });
                    await this.FETCH_MESSAGE_METADATA({ messages: conversation.messages });

                    if (conversation) {
                        this.SET_CURRENT_CONVERSATION(conversation);
                        this.SET_ACTIVE_MESSAGE({ key: conversation.messages[0] });
                    }
                } catch (e) {
                    this.$router.push({ name: "mail:home" });
                    throw e;
                }
            },
            immediate: true
        }
    },
    methods: {
        ...mapMutations("mail", {
            RESET_PARTS_DATA,
            RESET_ACTIVE_MESSAGE,
            SET_ACTIVE_MESSAGE,
            SET_CURRENT_CONVERSATION,
            UNSELECT_ALL_CONVERSATIONS,
            UNSET_CURRENT_CONVERSATION
        }),
        ...mapActions("mail", { FETCH_CONVERSATION_IF_NOT_LOADED, FETCH_MESSAGE_METADATA })
    }
};
</script>

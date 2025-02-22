<template>
    <mail-conversation-panel class="mail-route-conversation" />
</template>

<script>
import { conversationUtils, loadingStatusUtils } from "@bluemind/mail";
import ConversationPathParam from "~/router/ConversationPathParam";
import {
    RESET_ACTIVE_MESSAGE,
    RESET_PARTS_DATA,
    SET_ACTIVE_MESSAGE,
    SET_CURRENT_CONVERSATION,
    UNSELECT_ALL_CONVERSATIONS,
    UNSET_CURRENT_CONVERSATION
} from "~/mutations";
import { CONVERSATIONS_ACTIVATED, CONVERSATION_MESSAGE_BY_KEY, SELECTION_IS_EMPTY } from "~/getters";
import { FETCH_CONVERSATION_IF_NOT_LOADED, FETCH_MESSAGE_IF_NOT_LOADED, FETCH_MESSAGE_METADATA } from "~/actions";
import { mapActions, mapGetters, mapMutations, mapState } from "vuex";
import MailConversationPanel from "./MailThread/MailConversationPanel";
import { WaitForMixin } from "~/mixins";

const { idToUid: conversationIdToUid } = conversationUtils;
const { LoadingStatus } = loadingStatusUtils;

export default {
    name: "MailRouteConversation",
    components: { MailConversationPanel },
    mixins: [WaitForMixin],

    computed: {
        ...mapState("mail", ["activeFolder", "folders"]),
        ...mapGetters("mail", { CONVERSATION_MESSAGE_BY_KEY, SELECTION_IS_EMPTY })
    },

    watch: {
        "$route.params.conversationpath": {
            async handler(conversationPath) {
                try {
                    this.RESET_PARTS_DATA();
                    this.RESET_ACTIVE_MESSAGE();
                    this.UNSET_CURRENT_CONVERSATION();

                    await this.$waitFor("activeFolder");
                    await this.$waitFor(
                        () => this.$store.state["root-app"].identities,
                        ({ length }) => length
                    );

                    let { folderKey, internalId } = ConversationPathParam.parse(conversationPath, this.activeFolder);

                    if (!this.SELECTION_IS_EMPTY) {
                        this.UNSELECT_ALL_CONVERSATIONS();
                    }

                    const conversationsActivated = this.$store.getters[`mail/${CONVERSATIONS_ACTIVATED}`];

                    const isMessageId = /^\d+$/.test(internalId);
                    if (isMessageId && conversationsActivated) {
                        const message = await this.FETCH_MESSAGE_IF_NOT_LOADED({
                            internalId: parseInt(internalId),
                            folder: this.folders[folderKey]
                        });
                        if (message?.conversationId) {
                            internalId = conversationIdToUid(message.conversationId);
                        }
                    }

                    const folder = this.folders[folderKey];

                    const conversation = await this.FETCH_CONVERSATION_IF_NOT_LOADED({
                        uid: internalId,
                        folder,
                        conversationsActivated
                    });

                    if (conversation) {
                        let messages = this.CONVERSATION_MESSAGE_BY_KEY(conversation.key).filter(
                            message => message.loading !== LoadingStatus.LOADED
                        );
                        await this.FETCH_MESSAGE_METADATA({ messages: messages.map(m => m.key) });

                        messages = this.CONVERSATION_MESSAGE_BY_KEY(conversation.key);
                        this.SET_CURRENT_CONVERSATION(conversation);
                        this.SET_ACTIVE_MESSAGE(messages[0]);
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
        ...mapActions("mail", { FETCH_CONVERSATION_IF_NOT_LOADED, FETCH_MESSAGE_IF_NOT_LOADED, FETCH_MESSAGE_METADATA })
    }
};
</script>

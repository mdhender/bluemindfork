<template>
    <div class="conversation-list-item-left d-flex flex-column align-items-center">
        <conversation-avatar
            v-if="conversationsActivated && conversation && conversationSize > 1"
            :class="[SELECTION_IS_EMPTY ? '' : 'd-none']"
            :text="conversationSize > 99 ? '+99' : conversationSize"
            :font-size="conversationSize > 99 ? 'smaller' : 'unset'"
            :title="$t('mail.conversation.icon.title', { count: conversationSize })"
        />
        <bm-avatar v-else :alt="fromOrTo" :class="[SELECTION_IS_EMPTY ? '' : 'd-none']" />
        <bm-check
            :checked="CONVERSATION_IS_SELECTED(conversation.key)"
            :class="[SELECTION_IS_EMPTY ? 'd-none' : 'd-block']"
            @change="$emit('toggle-select', conversation.key, true)"
            @click.exact.native.stop
            @keyup.native.space.stop
        />
        <template v-if="userSettings.mail_message_list_style === 'full'">
            <bm-icon v-if="conversation.hasAttachment" icon="paper-clip" />
            <bm-icon v-if="conversation.hasICS" icon="event" />
        </template>
        <template v-else>
            <mail-attachment-icon :message="conversation" />
        </template>
    </div>
</template>

<script>
import { BmAvatar, BmCheck, BmIcon } from "@bluemind/styleguide";
import { mapGetters, mapState } from "vuex";
import { MY_DRAFTS, MY_SENT, CONVERSATION_IS_SELECTED, SELECTION_IS_EMPTY } from "~/getters";
import ConversationAvatar from "./ConversationAvatar";
import MailAttachmentIcon from "../MailAttachmentIcon";

export default {
    name: "ConversationListItemLeft",
    components: {
        BmAvatar,
        BmCheck,
        BmIcon,
        ConversationAvatar,
        MailAttachmentIcon
    },
    props: {
        conversation: {
            type: Object,
            required: true
        },
        conversationSize: {
            type: Number,
            required: true
        }
    },
    computed: {
        ...mapState("mail", ["activeFolder", "folders"]),
        ...mapGetters("mail", { CONVERSATION_IS_SELECTED, SELECTION_IS_EMPTY, MY_DRAFTS, MY_SENT }),
        ...mapState("session", { userSettings: ({ settings }) => settings.remote }),

        fromOrTo() {
            const conversationFolder = this.conversation.folderRef.key;
            const isSentOrDraftBox = [this.MY_DRAFTS.key, this.MY_SENT.key].includes(conversationFolder);
            if (isSentOrDraftBox) {
                const firstRecipient = this.conversation.to[0];
                return firstRecipient ? (firstRecipient.dn ? firstRecipient.dn : firstRecipient.address) : "";
            } else {
                return this.conversation.from.dn ? this.conversation.from.dn : this.conversation.from.address;
            }
        },
        conversationsActivated() {
            return this.userSettings.mail_thread === "true" && this.folders[this.activeFolder].allowConversations;
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/variables";

.conversation-list-item-left {
    min-width: $sp-2 + 1.3rem;

    $avatar-height: 2em !important;

    .bm-avatar {
        height: $avatar-height;
    }

    .bm-check {
        height: $avatar-height;
        // align with avatar
        transform: translateX(4px);
    }

    .custom-control-label::after,
    .custom-control-label::before {
        top: 0.2rem !important;
    }
}
</style>

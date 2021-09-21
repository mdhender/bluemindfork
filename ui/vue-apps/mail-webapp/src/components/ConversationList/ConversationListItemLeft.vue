<template>
    <div
        class="conversation-list-item-left d-flex flex-column align-items-center"
        :class="{ 'has-icon': conversation.hasICS || conversation.hasAttachment }"
    >
        <conversation-avatar
            v-if="conversationsActivated && conversation && conversationSize > 1"
            :class="[selectionMode === SELECTION_MODE.MONO ? '' : 'd-none']"
            :text="conversationSize > 99 ? '+99' : conversationSize"
            :font-size="conversationSize > 99 ? 'smaller' : 'unset'"
            :title="$t('mail.conversation.icon.title', { count: conversationSize })"
        />
        <div v-else class="avatar" :class="[selectionMode === SELECTION_MODE.MONO ? '' : 'd-none']">
            <bm-avatar :alt="fromOrTo" />
        </div>
        <bm-check
            v-if="multiple"
            :checked="selectionMode === SELECTION_MODE.MULTI && isSelected"
            :class="[selectionMode === SELECTION_MODE.MONO ? 'd-none' : 'd-block']"
            @change="$emit('check')"
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
import { MY_DRAFTS, MY_SENT } from "~/getters";
import ConversationAvatar from "./ConversationAvatar";
import MailAttachmentIcon from "../MailAttachmentIcon";
import { SELECTION_MODE } from "./ConversationList.vue";

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
        },
        isSelected: {
            type: Boolean,
            required: true
        },
        multiple: {
            type: Boolean,
            required: true
        },
        selectionMode: {
            type: String,
            required: true
        }
    },
    data() {
        return { SELECTION_MODE };
    },
    computed: {
        ...mapState("mail", ["activeFolder", "folders"]),
        ...mapGetters("mail", { MY_DRAFTS, MY_SENT }),
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

    $avatar-height: 2em;

    $icons-height: 1em;

    .bm-avatar {
        height: $avatar-height;
    }

    &.has-icon .avatar {
        height: calc(#{$avatar-height} + #{$icons-height});
    }

    .bm-check {
        height: $avatar-height;
        // align with avatar
        transform: translateX(4px);
    }

    &.has-icon .bm-check {
        height: calc(#{$avatar-height} + #{$icons-height});
    }

    .custom-control-label::after,
    .custom-control-label::before {
        top: 0.2rem !important;
    }
}
</style>

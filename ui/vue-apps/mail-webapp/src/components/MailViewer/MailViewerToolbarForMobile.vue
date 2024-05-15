<template>
    <bm-icon-dropdown
        v-if="!CONVERSATION_LIST_DELETED_FILTER_ENABLED"
        variant="regular-accent"
        size="sm"
        icon="3dots-vertical"
        no-caret
        class="mail-viewer-toolbar-for-mobile d-flex justify-content-end"
        :aria-label="$tc('mail.toolbar.more.aria')"
        :title="$tc('mail.toolbar.more.aria')"
        v-on="$listeners"
    >
        <bm-dropdown-item-button
            icon="arrow-left-broken"
            @click="initRelatedMessage(MY_DRAFTS, MessageCreationModes.REPLY, messageRemoteRefs)"
        >
            {{ $t("mail.content.reply.aria") }}
        </bm-dropdown-item-button>
        <bm-dropdown-divider />
        <bm-dropdown-item-button
            icon="arrows-left-broken"
            @click="initRelatedMessage(MY_DRAFTS, MessageCreationModes.REPLY_ALL, messageRemoteRefs)"
        >
            {{ $t("mail.content.reply_all.aria") }}
        </bm-dropdown-item-button>
        <bm-dropdown-divider />
        <bm-dropdown-item-button
            icon="arrow-right"
            @click="initRelatedMessage(MY_DRAFTS, MessageCreationModes.FORWARD, messageRemoteRefs)"
        >
            {{ $t("common.forward") }}
        </bm-dropdown-item-button>
        <bm-dropdown-divider />
        <bm-dropdown-item-button
            v-if="message.flags && !message.flags.includes(Flag.SEEN)"
            icon="mail-open"
            @click="MARK_MESSAGE_AS_READ(message)"
        >
            {{ $tc("mail.actions.mark_read", 1) }}
        </bm-dropdown-item-button>
        <bm-dropdown-item-button v-else icon="mail-dot" @click="MARK_MESSAGE_AS_UNREAD(message)">
            {{ $tc("mail.actions.mark_unread", 1) }}
        </bm-dropdown-item-button>
        <bm-dropdown-divider />
        <bm-dropdown-item-button
            v-if="message.flags && !message.flags.includes(Flag.FLAGGED)"
            icon="flag"
            @click.prevent.stop="MARK_MESSAGE_AS_FLAGGED(message)"
        >
            {{ $t("mail.actions.mark_flagged") }}
        </bm-dropdown-item-button>
        <bm-dropdown-item-button
            v-else
            icon="flag-fill"
            class="flag-fill"
            @click.prevent.stop="MARK_MESSAGE_AS_UNFLAGGED(message)"
        >
            {{ $t("mail.actions.mark_unflagged") }}
        </bm-dropdown-item-button>
        <bm-dropdown-divider />
        <bm-dropdown-item-button icon="trash" @click.exact.prevent.stop="MOVE_MESSAGES_TO_TRASH(message, conversation)">
            {{ $t("mail.actions.remove") }}
        </bm-dropdown-item-button>
    </bm-icon-dropdown>
</template>

<script>
import { BmIconDropdown, BmDropdownDivider, BmDropdownItemButton } from "@bluemind/ui-components";
import { mapActions, mapGetters } from "vuex";
import { Flag } from "@bluemind/email";
import {
    MARK_MESSAGE_AS_FLAGGED,
    MARK_MESSAGE_AS_READ,
    MARK_MESSAGE_AS_UNFLAGGED,
    MARK_MESSAGE_AS_UNREAD
} from "~/actions";
import { messageUtils } from "@bluemind/mail";
import { RemoveMixin } from "~/mixins";
import { CONVERSATION_LIST_DELETED_FILTER_ENABLED, MY_DRAFTS } from "~/getters";
import { useComposerInit } from "~/composables/composer/ComposerInit";

const { MessageCreationModes } = messageUtils;

export default {
    name: "MailViewerToolbarForMobile",
    components: {
        BmIconDropdown,
        BmDropdownDivider,
        BmDropdownItemButton
    },
    mixins: [RemoveMixin],
    props: {
        message: {
            type: Object,
            required: true
        },
        conversation: {
            type: Object,
            required: true
        }
    },
    setup() {
        const { initRelatedMessage } = useComposerInit();
        return { initRelatedMessage };
    },
    data() {
        return {
            MessageCreationModes,
            Flag
        };
    },
    computed: {
        ...mapGetters("mail", { CONVERSATION_LIST_DELETED_FILTER_ENABLED, MY_DRAFTS }),
        messageRemoteRefs() {
            return { internalId: this.message.remoteRef.internalId, folderKey: this.message.folderRef.key };
        }
    },
    methods: {
        ...mapActions("mail", {
            MARK_MESSAGE_AS_FLAGGED,
            MARK_MESSAGE_AS_READ,
            MARK_MESSAGE_AS_UNFLAGGED,
            MARK_MESSAGE_AS_UNREAD
        })
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.mail-viewer-toolbar-for-mobile {
    .dropdown-divider {
        border-top: 1px solid $neutral-fg-lo2 !important;
        margin: 0.05rem 0;
    }
    .dropdown-menu {
        box-shadow: none;
        position: fixed !important;
        top: auto !important;
        bottom: 0px !important;
        transform: none !important;
        right: 0px !important;
        line-height: 2;
    }
    .b-dropdown-item-button.flag-fill .bm-icon {
        color: $warning-fg;
    }
}
</style>

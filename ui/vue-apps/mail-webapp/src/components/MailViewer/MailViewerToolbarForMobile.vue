<template>
    <bm-button-toolbar key-nav class="mail-viewer-toolbar bg-surface">
        <bm-icon-dropdown
            variant="compact"
            size="lg"
            icon="3dots-v"
            no-caret
            class="mail-viewer-toolbar-for-mobile d-flex justify-content-end"
            :aria-label="$tc('mail.toolbar.more.aria')"
            :title="$tc('mail.toolbar.more.aria')"
            v-on="$listeners"
        >
            <bm-dropdown-item-button icon="reply" @click="initReplyOrForward(MessageCreationModes.REPLY, message)">
                {{ $t("mail.content.reply.aria") }}
            </bm-dropdown-item-button>
            <bm-dropdown-divider />
            <bm-dropdown-item-button
                icon="reply-all"
                @click="initReplyOrForward(MessageCreationModes.REPLY_ALL, message)"
            >
                {{ $t("mail.content.reply_all.aria") }}
            </bm-dropdown-item-button>
            <bm-dropdown-divider />
            <bm-dropdown-item-button icon="forward" @click="initReplyOrForward(MessageCreationModes.FORWARD, message)">
                {{ $t("common.forward") }}
            </bm-dropdown-item-button>
            <bm-dropdown-divider />
            <bm-dropdown-item-button
                v-if="message.flags && !message.flags.includes(Flag.SEEN)"
                icon="read"
                @click="MARK_MESSAGE_AS_READ(message)"
            >
                {{ $tc("mail.actions.mark_as_read", 1) }}
            </bm-dropdown-item-button>
            <bm-dropdown-item-button v-else icon="unread" @click="MARK_MESSAGE_AS_UNREAD(message)">
                {{ $tc("mail.actions.mark_as_unread", 1) }}
            </bm-dropdown-item-button>
            <bm-dropdown-divider />
            <bm-dropdown-item-button
                v-if="message.flags && !message.flags.includes(Flag.FLAGGED)"
                icon="flag-outline"
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
            <bm-dropdown-item-button
                icon="trash"
                @click.exact.prevent.stop="MOVE_MESSAGES_TO_TRASH(conversation, message)"
            >
                {{ $t("mail.actions.remove") }}
            </bm-dropdown-item-button>
        </bm-icon-dropdown>
    </bm-button-toolbar>
</template>

<script>
import { BmButtonToolbar, BmIconDropdown, BmDropdownDivider, BmDropdownItemButton } from "@bluemind/styleguide";
import { mapActions } from "vuex";
import { Flag } from "@bluemind/email";
import {
    MARK_MESSAGE_AS_FLAGGED,
    MARK_MESSAGE_AS_READ,
    MARK_MESSAGE_AS_UNFLAGGED,
    MARK_MESSAGE_AS_UNREAD
} from "~/actions";
import { messageUtils } from "@bluemind/mail";
import { RemoveMixin, ComposerInitMixin } from "~/mixins";

const { MessageCreationModes } = messageUtils;

export default {
    name: "MailViewerToolbarForMobile",
    components: {
        BmButtonToolbar,
        BmIconDropdown,
        BmDropdownDivider,
        BmDropdownItemButton
    },
    mixins: [RemoveMixin, ComposerInitMixin],
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
    data() {
        return {
            MessageCreationModes,
            Flag
        };
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
@import "@bluemind/styleguide/css/_variables.scss";

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

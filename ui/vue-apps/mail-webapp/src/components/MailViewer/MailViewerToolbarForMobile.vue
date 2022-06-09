<template>
    <bm-button-toolbar key-nav class="mail-viewer-toolbar bg-surface">
        <bm-dropdown
            :no-caret="true"
            variant="simple-neutral"
            class="mail-viewer-toolbar-for-mobile d-flex justify-content-end"
            :aria-label="$tc('mail.toolbar.more.aria')"
            :title="$tc('mail.toolbar.more.aria')"
            v-on="$listeners"
        >
            <template slot="button-content">
                <bm-icon class="text-secondary" icon="3dots" size="2x" />
            </template>
            <bm-dropdown-item-button @click="initReplyOrForward(MessageCreationModes.REPLY, message)">
                <bm-icon icon="reply" size="1x" />
                <span class="pl-1">{{ $t("mail.content.reply.aria") }}</span>
            </bm-dropdown-item-button>
            <bm-dropdown-divider />
            <bm-dropdown-item-button @click="initReplyOrForward(MessageCreationModes.REPLY_ALL, message)">
                <bm-icon icon="reply-all" size="1x" />
                <span class="pl-1">{{ $t("mail.content.reply_all.aria") }}</span>
            </bm-dropdown-item-button>
            <bm-dropdown-divider />
            <bm-dropdown-item-button @click="initReplyOrForward(MessageCreationModes.FORWARD, message)">
                <bm-icon icon="forward" size="1x" />
                <span class="pl-1">{{ $t("common.forward") }}</span>
            </bm-dropdown-item-button>
            <bm-dropdown-divider />
            <bm-dropdown-item-button
                v-if="message.flags && !message.flags.includes(Flag.SEEN)"
                @click="MARK_MESSAGE_AS_READ(message)"
            >
                <bm-icon icon="read" size="1x" />
                <span class="pl-1">{{ $tc("mail.actions.mark_as_read", 1) }}</span>
            </bm-dropdown-item-button>
            <bm-dropdown-item-button v-else @click="MARK_MESSAGE_AS_UNREAD(message)">
                <bm-icon icon="unread" size="1x" />
                <span class="pl-1">{{ $tc("mail.actions.mark_as_unread", 1) }}</span>
            </bm-dropdown-item-button>
            <bm-dropdown-divider />
            <bm-dropdown-item-button
                v-if="message.flags && !message.flags.includes(Flag.FLAGGED)"
                @click.prevent.stop="MARK_MESSAGE_AS_FLAGGED(message)"
            >
                <bm-icon icon="flag-outline" size="1x" />
                <span class="pl-1">{{ $t("mail.actions.mark_flagged") }}</span>
            </bm-dropdown-item-button>
            <bm-dropdown-item-button v-else @click.prevent.stop="MARK_MESSAGE_AS_UNFLAGGED(message)">
                <bm-icon icon="flag-fill" size="1x" class="text-warning" />
                <span class="pl-1">{{ $t("mail.actions.mark_unflagged") }}</span>
            </bm-dropdown-item-button>
            <bm-dropdown-divider />
            <bm-dropdown-item-button @click.exact.prevent.stop="MOVE_MESSAGES_TO_TRASH(conversation, message)">
                <bm-icon icon="trash" size="1x" />
                <span class="pl-1">{{ $t("mail.actions.remove") }}</span>
            </bm-dropdown-item-button>
        </bm-dropdown>
    </bm-button-toolbar>
</template>

<script>
import { BmButtonToolbar, BmDropdown, BmDropdownDivider, BmDropdownItemButton, BmIcon } from "@bluemind/styleguide";
import { mapActions } from "vuex";
import { Flag } from "@bluemind/email";
import {
    MARK_MESSAGE_AS_FLAGGED,
    MARK_MESSAGE_AS_READ,
    MARK_MESSAGE_AS_UNFLAGGED,
    MARK_MESSAGE_AS_UNREAD
} from "~/actions";
import { MessageCreationModes } from "~/model/message";
import { RemoveMixin, ComposerInitMixin } from "~/mixins";

export default {
    name: "MailViewerToolbarForMobile",
    components: {
        BmButtonToolbar,
        BmDropdown,
        BmDropdownDivider,
        BmDropdownItemButton,
        BmIcon
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
}
</style>

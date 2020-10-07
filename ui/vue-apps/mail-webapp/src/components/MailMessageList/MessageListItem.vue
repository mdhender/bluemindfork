<template>
    <bm-list-group-item
        v-touch:touchhold="onTouch"
        class="d-flex message-list-item"
        :class="{
            ['message-list-item-' + userSettings.mail_message_list_style]: true,
            'not-seen': !message.flags.includes(Flag.SEEN),
            'warning-custom': message.flags.includes(Flag.FLAGGED),
            active: isMessageSelected(message.key) || currentMessageKey === message.key
        }"
        role="link"
        @click="navigateTo"
        @keyup.enter.exact="navigateTo"
        @mouseenter="mouseIn = true"
        @mouseleave="mouseIn = false"
    >
        <screen-reader-only-message-information :message="message" />
        <message-list-item-left :message="message" @toggle-select="$emit('toggle-select', message.key, true)" />
        <message-list-item-middle
            class="flex-fill px-2"
            :message="message"
            :is-important="message.flags.includes(Flag.FLAGGED)"
            :mouse-in="mouseIn"
        />
        <message-list-item-quick-action-buttons v-if="mouseIn" :message="message" @purge="purge" />
    </bm-list-group-item>
</template>

<script>
import { BmListGroupItem, BmTooltip } from "@bluemind/styleguide";
import { mapGetters, mapState } from "vuex";
import MessageListItemLeft from "./MessageListItemLeft";
import MessageListItemMiddle from "./MessageListItemMiddle";
import MessageListItemQuickActionButtons from "./MessageListItemQuickActionButtons";
import ScreenReaderOnlyMessageInformation from "./ScreenReaderOnlyMessageInformation";
import { Flag } from "@bluemind/email";

export default {
    name: "MessageListItem",
    components: {
        BmListGroupItem,
        MessageListItemLeft,
        MessageListItemMiddle,
        MessageListItemQuickActionButtons,
        ScreenReaderOnlyMessageInformation
    },
    directives: { BmTooltip },
    props: {
        message: {
            type: Object,
            required: true
        },
        isMuted: {
            type: Boolean,
            required: false,
            default: false
        }
    },
    data() {
        return {
            tooltip: {
                cursor: "cursor",
                text: this.$t("mail.actions.move")
            },
            mouseIn: false,
            Flag
        };
    },
    computed: {
        ...mapState("mail", ["folders", "activeFolder"]),
        ...mapGetters("mail-webapp", ["isMessageSelected", "nextMessageKey"]),
        ...mapState("mail-webapp", ["selectedMessageKeys"]),
        ...mapState("session", ["userSettings"]),
        ...mapState("mail-webapp/currentMessage", { currentMessageKey: "key" })
    },
    methods: {
        async purge() {
            const confirm = await this.$bvModal.msgBoxConfirm(
                this.$tc("mail.actions.purge.modal.content", this.selectedMessageKeys.length || 1, {
                    subject: this.message.subject
                }),
                {
                    title: this.$tc("mail.actions.purge.modal.title", this.selectedMessageKeys.length || 1),
                    okTitle: this.$t("common.delete"),
                    cancelVariant: "outline-secondary",
                    cancelTitle: this.$t("common.cancel"),
                    centered: true,
                    hideHeaderClose: false
                }
            );
            if (confirm) {
                // do this before followed async operations
                const nextMessageKey = this.nextMessageKey;
                this.$store.dispatch("mail-webapp/purge", this.message.key);
                if (this.currentMessageKey === this.message.key) {
                    this.$router.navigate({ name: "v:mail:message", params: { message: nextMessageKey } });
                }
            }
        },
        onTouch() {
            this.$emit("toggleSelect", this.message.key);
        },
        navigateTo() {
            const params = { message: this.message.key };
            if (this.activeFolder !== this.message.folderRef.key) {
                params.folder = this.message.folderRef.key;
            }
            this.$router.navigate({ name: "v:mail:message", params });
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/variables";

.message-list-item {
    border-left: transparent solid 4px !important;
    .states .fa-event {
        color: $calendar-color;
    }
    &.not-seen {
        border-left: theme-color("primary") 4px solid !important;
    }

    &.not-seen .mail-message-list-item-sender,
    &.not-seen .mail-message-list-item-subject {
        font-weight: $font-weight-bold;
    }

    &.message-list-item-full {
        padding-top: $sp-1 !important;
        padding-bottom: $sp-1 !important;
        .mail-message-list-item-subject,
        .mail-message-list-item-date {
            line-height: $line-height-sm;
        }
        .mail-message-list-item-preview {
            display: block;
        }
    }

    &.message-list-item-compact {
        padding-top: $sp-1 !important;
        padding-bottom: $sp-1 !important;
        .mail-message-list-item-subject,
        .mail-message-list-item-date {
            line-height: $line-height-sm;
        }
        .mail-message-list-item-preview {
            display: none;
        }
    }

    &.message-list-item-normal .mail-message-list-item-preview,
    &.message-list-item-null .mail-message-list-item-preview {
        display: none;
    }

    // obtain the same enlightment that BAlert applies on $warning TODO move to variables.scss in SG
    $custom-warning-color: theme-color-level("warning", $alert-bg-level);

    &.warning-custom:not(.active) {
        background-color: $custom-warning-color;
    }
}
</style>

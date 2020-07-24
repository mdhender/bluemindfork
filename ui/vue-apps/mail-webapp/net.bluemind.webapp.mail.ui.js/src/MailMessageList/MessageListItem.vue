<template>
    <bm-draggable
        class="message-list-item"
        :class="{ muted: isMuted }"
        :tooltip="tooltip"
        name="message"
        :data="message"
        disable-touch
        @dragenter="e => setTooltip(e.relatedData)"
        @dragleave="resetTooltip"
        @drop="e => moveMessage(e.relatedData)"
        @dragstart="$emit('dragstart', $event)"
        @dragend="$emit('dragend', $event)"
    >
        <bm-list-group-item
            v-touch:touchhold="onTouch"
            class="d-flex"
            active-class="active"
            :class="[
                ...message.states,
                isMessageSelected(message.key) || currentMessageKey === message.key ? 'active' : '',
                `message-list-item-${userSettings.mail_message_list_style}`,
                isImportant ? 'warning-custom' : ''
            ]"
            role="link"
            @click="navigateTo"
            @keyup.enter.exact="navigateTo"
            @mouseenter="mouseIn = true"
            @mouseleave="mouseIn = false"
        >
            <message-list-item-left :message="message" @toggleSelect="$emit('toggleSelect', message.key, true)" />
            <message-list-item-middle
                class="flex-fill px-2"
                :message="message"
                :is-important="isImportant"
                :mouse-in="mouseIn"
            />
            <transition name="fade-in" mode="out-in">
                <message-list-item-quick-action-buttons v-if="mouseIn" :message="message" />
            </transition>
        </bm-list-group-item>
        <template v-slot:shadow>
            <mail-message-list-item-shadow :message="message" :count="selectedMessageKeys.length" />
        </template>
    </bm-draggable>
</template>

<script>
import { BmListGroupItem, BmTooltip, BmDraggable } from "@bluemind/styleguide";
import { mapActions, mapGetters, mapState } from "vuex";
import ItemUri from "@bluemind/item-uri";
import MailMessageListItemShadow from "./MailMessageListItemShadow";
import MessageListItemLeft from "./MessageListItemLeft";
import MessageListItemMiddle from "./MessageListItemMiddle";
import MessageListItemQuickActionButtons from "./MessageListItemQuickActionButtons";
import { Flag } from "@bluemind/email";

export default {
    name: "MessageListItem",
    components: {
        BmDraggable,
        BmListGroupItem,
        MessageListItemLeft,
        MessageListItemMiddle,
        MailMessageListItemShadow,
        MessageListItemQuickActionButtons
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
            mouseIn: false
        };
    },
    computed: {
        ...mapGetters("mail-webapp/folders", ["getFolderByKey"]),
        ...mapGetters("mail-webapp", ["isMessageSelected", "nextMessageKey", "currentMailbox"]),
        ...mapState("mail-webapp", ["selectedMessageKeys", "userSettings"]),
        ...mapState("mail-webapp/currentMessage", { currentMessageKey: "key" }),
        isImportant() {
            return this.message.flags.some(f => Flag.FLAGGED === f);
        }
    },
    methods: {
        ...mapActions("mail-webapp", ["move"]),
        setTooltip(folder) {
            if (folder) {
                const draggedMessageFolderUid = ItemUri.container(this.message.key);
                const dropzoneFolderUid = ItemUri.item(folder.key);
                const dropzoneFolderIsReadOnly = !folder.writable;

                if (draggedMessageFolderUid === dropzoneFolderUid) {
                    this.tooltip.text = this.$t("mail.actions.move.item.warning.self", {
                        path: folder.fullName
                    });
                    this.tooltip.cursor = "forbidden";
                } else if (dropzoneFolderIsReadOnly) {
                    this.tooltip.text = this.$t("mail.actions.move.item.warning.readonly", {
                        path: folder.fullName
                    });
                    this.tooltip.cursor = "forbidden";
                } else {
                    this.tooltip.text = this.$t("mail.actions.move.item", { path: folder.fullName });
                    this.tooltip.cursor = "cursor";
                }
            }
        },
        resetTooltip() {
            this.tooltip.text = this.$t("mail.actions.move");
            this.tooltip.cursor = "cursor";
        },
        moveMessage(folder) {
            const draggedMessageFolderUid = ItemUri.container(this.message.key);
            const dropzoneFolderUid = ItemUri.item(folder.key);
            const dropzoneFolderIsReadOnly = !folder.writable;
            if (draggedMessageFolderUid !== dropzoneFolderUid && !dropzoneFolderIsReadOnly) {
                if (this.message.key === this.currentMessageKey) {
                    this.$router.navigate({ name: "v:mail:message", params: { message: this.nextMessageKey } });
                }
                if (this.selectedMessageKeys.includes(this.message.key)) {
                    this.move({ messageKey: [...this.selectedMessageKeys], folder: this.getFolderByKey(folder.key) });
                } else {
                    this.move({ messageKey: this.message.key, folder: this.getFolderByKey(folder.key) });
                }
            }
        },
        onTouch() {
            this.$emit("toggleSelect", this.message.key);
        },
        navigateTo() {
            const folder = ItemUri.encode(ItemUri.container(this.message.key), this.currentMailbox.mailboxUid);
            this.$router.navigate({ name: "v:mail:message", params: { folder, message: this.message.key } });
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/variables";

.message-list-item .states .fa-event {
    color: $calendar-color;
}

.message-list-item {
    cursor: pointer;

    &.muted > div {
        opacity: 0.55;
    }

    &:hover .bm-check {
        display: block !important;
    }

    &:hover .bm-avatar {
        display: none !important;
    }

    div.list-group-item.not-seen {
        border-left: theme-color("primary") 4px solid !important;
    }

    .not-seen .mail-message-list-item-sender,
    .not-seen .mail-message-list-item-subject {
        font-weight: $font-weight-bold;
    }

    div.list-group-item {
        border-left: transparent solid 4px !important;
    }

    .list-group-item:focus {
        outline: $outline;
        &:hover {
            background-color: $component-active-bg-darken;
        }
    }

    &:focus {
        outline: $outline !important;
    }

    &:focus &:hover {
        background-color: $component-active-bg-darken;
    }

    .message-list-item-full {
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

    .message-list-item-compact {
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

    .message-list-item-null .mail-message-list-item-preview {
        display: none;
    }

    // obtain the same enlightment that BAlert applies on $warning TODO move to variables.scss in SG
    $custom-warning-color: lighten($warning, 33.9%);

    .warning-custom {
        background-color: $custom-warning-color;
    }

    .fade-in-enter-active {
        transition: opacity 0.35s linear 0.15s;
    }

    .fade-in-leave-active {
        opacity: 0;
    }

    .fade-in-enter,
    .fade-in-leave-to {
        opacity: 0;
        position: absolute;
        right: 0;
    }
}
</style>

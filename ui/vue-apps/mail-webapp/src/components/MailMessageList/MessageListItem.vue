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
            <i18n path="mail.list.sr_info" tag="p" class="sr-only">
                <template #title>
                    <p id="srInfoTitle">{{ $t("mail.list.sr_info.title") }}</p>
                </template>
                <template #props>
                    <i18n path="mail.list.sr_info.props" tag="ul" aria-labelledby="srInfoTitle">
                        <template v-if="message.flags.includes(Flags.FLAGGED)" #flagged>
                            <li>{{ $t("mail.list.sr_info.flagged") }}</li>
                        </template>
                        <template v-if="message.states.includes('not-seen')" #unread>
                            <li>{{ $t("mail.list.sr_info.unread") }}</li>
                        </template>
                        <template v-else #read>
                            <li>{{ $t("mail.list.sr_info.read") }}</li>
                        </template>
                        <template v-if="message.states.includes('is-ics')" #event>
                            <li>{{ $t("mail.list.sr_info.event") }}</li>
                        </template>
                        <template v-if="message.states.includes('has-attachment')" #attachment>
                            <li>{{ $t("mail.list.sr_info.attachment") }}</li>
                        </template>
                        <template v-if="message.flags.includes(Flags.ANSWERED)" #replied>
                            <li>{{ $t("mail.list.sr_info.replied") }}</li>
                        </template>
                        <template v-if="message.flags.includes(Flags.FORWARDED)" #forwarded>
                            <li>{{ $t("mail.list.sr_info.forwarded") }}</li>
                        </template>
                    </i18n>
                </template>
            </i18n>
            <message-list-item-left :message="message" @toggleSelect="$emit('toggleSelect', message.key, true)" />
            <message-list-item-middle
                class="flex-fill px-2"
                :message="message"
                :is-important="isImportant"
                :mouse-in="mouseIn"
            />
            <transition name="fade-in" mode="out-in">
                <message-list-item-quick-action-buttons v-if="mouseIn" :message="message" @purge="purge" />
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
            mouseIn: false,
            Flags: Flag
        };
    },
    computed: {
        ...mapState("mail", ["folders", "activeFolder"]),
        ...mapGetters("mail-webapp", ["isMessageSelected", "nextMessageKey"]),
        ...mapState("mail-webapp", ["selectedMessageKeys"]),
        ...mapState("session", ["userSettings"]),
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
                const dropzoneFolderUid = folder.key;
                const dropzoneFolderIsReadOnly = !folder.writable;

                if (draggedMessageFolderUid === dropzoneFolderUid) {
                    this.tooltip.text = this.$t("mail.actions.move.item.warning.self", {
                        path: folder.path
                    });
                    this.tooltip.cursor = "forbidden";
                } else if (dropzoneFolderIsReadOnly) {
                    this.tooltip.text = this.$t("mail.actions.move.item.warning.readonly", {
                        path: folder.path
                    });
                    this.tooltip.cursor = "forbidden";
                } else {
                    this.tooltip.text = this.$t("mail.actions.move.item", { path: folder.path });
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
            const dropzoneFolderUid = folder.key;
            const dropzoneFolderIsReadOnly = !folder.writable;
            if (draggedMessageFolderUid !== dropzoneFolderUid && !dropzoneFolderIsReadOnly) {
                if (this.message.key === this.currentMessageKey) {
                    this.$router.navigate({ name: "v:mail:message", params: { message: this.nextMessageKey } });
                }
                if (this.selectedMessageKeys.includes(this.message.key)) {
                    this.move({ messageKey: [...this.selectedMessageKeys], folder: this.folders[folder.key] });
                } else {
                    this.move({ messageKey: this.message.key, folder: this.folders[folder.key] });
                }
            }
        },
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
            if (this.activeFolder !== ItemUri.container(this.message.key)) {
                params.folder = ItemUri.container(this.message.key);
            }
            this.$router.navigate({ name: "v:mail:message", params });
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

    &:focus-within,
    &:hover {
        .bm-check {
            display: block !important;
        }
        .bm-avatar {
            display: none !important;
        }
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

    &:focus .list-group-item {
        outline: $outline;
        outline-offset: -1px;
        &:hover {
            background-color: $component-active-bg-darken;
        }
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

    .message-list-item-normal .mail-message-list-item-preview,
    .message-list-item-null .mail-message-list-item-preview {
        display: none;
    }

    // obtain the same enlightment that BAlert applies on $warning TODO move to variables.scss in SG
    $custom-warning-color: theme-color-level("warning", $alert-bg-level);

    .warning-custom:not(.active) {
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

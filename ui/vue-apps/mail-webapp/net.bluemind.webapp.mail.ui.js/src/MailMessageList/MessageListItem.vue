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
            active-class="active"
            :class="[
                ...message.states,
                isMessageSelected(message.key) || currentMessageKey === message.key ? 'active' : '',
                `message-list-item-${userSettings.mail_message_list_style}`
            ]"
            role="link"
            @click="navigateTo"
            @keyup.enter.exact="navigateTo"
            @mouseenter="mouseIn = true"
            @mouseleave="mouseIn = false"
        >
            <bm-row class="align-items-center flex-nowrap no-gutters">
                <bm-col cols="1" class="selector pr-2 text-center">
                    <bm-avatar :alt="from" :class="[anyMessageSelected ? 'd-none' : '']" />
                    <bm-check
                        :checked="isMessageSelected(message.key)"
                        :class="[anyMessageSelected ? 'd-block' : 'd-none']"
                        @click.exact.native.prevent.stop="$emit('toggleSelect', message.key, true)"
                    />
                </bm-col>
                <bm-col cols="8" class="text-overflow">
                    <div
                        v-bm-tooltip.ds500.viewport
                        :title="from"
                        class="text-overflow mw-100 mail-message-list-item-sender h3 text-dark"
                    >
                        {{ from }}
                    </div>
                </bm-col>
                <bm-col cols="3">
                    <transition name="fade" mode="out-in">
                        <div v-if="!quickActionButtonsVisible" class="float-right">
                            <component :is="widget" v-for="widget in widgets" :key="widget.template" class="ml-2" />
                        </div>
                        <message-list-item-quick-action-buttons v-if="quickActionButtonsVisible" :message="message" />
                    </transition>
                </bm-col>
            </bm-row>
            <bm-row class="no-gutters">
                <bm-col cols="1" class="mail-attachment">
                    <component :is="state" v-if="!!state" class="ml-1" />
                </bm-col>
                <bm-col class="text-overflow d-flex">
                    <div
                        v-bm-tooltip.ds500.bottom.viewport
                        :title="message.subject"
                        class="text-overflow mw-100 mail-message-list-item-subject flex-grow-1 text-secondary "
                    >
                        {{ message.subject }}
                    </div>
                    <div class="pl-2 mail-message-list-item-date">
                        <span class="text-nowrap d-none d-sm-block d-md-none d-xl-block">{{ displayedDate }}</span>
                        <span class="text-nowrap d-sm-none d-md-block d-xl-none">{{ smallerDisplayedDate }}</span>
                    </div>
                </bm-col>
            </bm-row>
            <bm-row class="no-gutters mail-message-list-item-preview">
                <bm-col cols="1"> </bm-col>
                <bm-col class="text-overflow d-flex">
                    <div
                        v-bm-tooltip.ds500.bottom.viewport
                        :title="message.preview"
                        class="text-overflow mw-100 flex-grow-1 text-dark text-condensed"
                    >
                        {{ message.preview || "&nbsp;" }}
                    </div>
                </bm-col>
            </bm-row>
        </bm-list-group-item>
        <template v-slot:shadow>
            <mail-message-list-item-shadow :message="message" :count="selectedMessageKeys.length" />
        </template>
    </bm-draggable>
</template>

<script>
const STATE_COMPONENT = {
    ["has-attachment"]: {
        components: { BmIcon },
        template: '<bm-icon icon="paper-clip"/>',
        priority: 99
    }
};
const FLAG_COMPONENT = {
    [Flag.FLAGGED]: {
        components: { BmIcon },
        template: '<bm-icon icon="flag-fill"/>'
    },
    [Flag.FORWARDED]: {
        components: { BmIcon },
        template: '<bm-icon icon="forward"/>'
    },
    [Flag.ANSWERED]: {
        components: { BmIcon },
        template: '<bm-icon icon="reply"/>'
    }
};

import { BmAvatar, BmCheck, BmCol, BmIcon, BmListGroupItem, BmRow, BmTooltip, BmDraggable } from "@bluemind/styleguide";
import { DateComparator } from "@bluemind/date";
import { Flag } from "@bluemind/email";
import { mapActions, mapGetters, mapState } from "vuex";
import ItemUri from "@bluemind/item-uri";
import MessageListItemQuickActionButtons from "./MessageListItemQuickActionButtons";
import MailMessageListItemShadow from "./MailMessageListItemShadow";

export default {
    name: "MessageListItem",
    components: {
        BmAvatar,
        BmCheck,
        BmCol,
        BmDraggable,
        BmIcon,
        BmListGroupItem,
        BmRow,
        MessageListItemQuickActionButtons,
        MailMessageListItemShadow
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
        ...mapGetters("mail-webapp", ["isMessageSelected", "nextMessageKey"]),
        ...mapState("mail-webapp", ["selectedMessageKeys", "currentFolderKey", "messageFilter", "userSettings"]),
        ...mapState("mail-webapp/currentMessage", { currentMessageKey: "key" }),
        displayedDate: function() {
            const today = new Date();
            const messageDate = this.message.date;
            if (DateComparator.isSameDay(messageDate, today)) {
                return this.$d(messageDate, "short_time");
            } else if (DateComparator.isSameYear(messageDate, today)) {
                return this.$d(messageDate, "relative_date");
            }
            return this.$d(messageDate, "short_date");
        },
        smallerDisplayedDate: function() {
            return this.displayedDate.substring(this.displayedDate.indexOf(" ") + 1);
        },
        widgets() {
            return this.message.flags.map(flag => FLAG_COMPONENT[flag]).filter(widget => !!widget);
        },
        state() {
            return this.message.states
                .map(state => STATE_COMPONENT[state])
                .filter(state => !!state)
                .sort((a, b) => a.order < b.order)
                .shift();
        },
        from() {
            return this.message.from.dn ? this.message.from.dn : this.message.from.address;
        },
        quickActionButtonsVisible() {
            return this.mouseIn && this.selectedMessageKeys.length <= 1;
        },
        anyMessageSelected() {
            return this.selectedMessageKeys.length > 0;
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
            this.$router.navigate({ name: "v:mail:message", params: { message: this.message.key } });
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/variables";

.message-list-item {
    cursor: pointer;
    .mail-message-list-item-preview {
        display: none;
    }
}

.message-list-item .bm-avatar {
    width: 1.3rem !important;
    height: 1.3rem !important;
}

.message-list-item.muted > div {
    opacity: 0.55;
}

.message-list-item .selector {
    min-width: $sp-2 + 1.3rem;
}

.message-list-item .selector .bm-check {
    min-height: 1.3rem;
    min-width: $sp-2 + 1.3rem;
    margin-left: 0.3rem;
}

.message-list-item-full {
    padding-top: $sp-1 !important;
    padding-bottom: $sp-1 !important;
    .mail-message-list-item-subject,
    .mail-message-list-item-date {
        line-height: $line-height-sm;
    }
    .mail-message-list-item-preview {
        display: flex;
    }
}

.message-list-item-compact {
    padding-top: $sp-1 !important;
    padding-bottom: $sp-1 !important;
    .mail-message-list-item-subject,
    .mail-message-list-item-date {
        line-height: $line-height-sm;
    }
}

.message-list-item:hover .selector .bm-check {
    display: block !important;
}

.message-list-item:hover .selector .bm-avatar {
    display: none !important;
}

.message-list-item div.list-group-item.not-seen {
    border-left: theme-color("primary") 4px solid !important;
}

.message-list-item div.list-group-item {
    border-left: transparent solid 4px !important;
}

.message-list-item .list-group-item:focus {
    outline: $outline;
    &:hover {
        background-color: $component-active-bg-darken;
    }
}

.message-list-item {
    &:focus {
        outline: $outline !important;
    }
    &:focus &:hover {
        background-color: $component-active-bg-darken;
    }
}

//FIXME: All those class should not be here or should be scoped...
.custom-control-label::after,
.custom-control-label::before {
    top: 0.2rem !important;
}

.not-seen .mail-message-list-item-sender,
.not-seen .mail-message-list-item-subject {
    font-weight: $font-weight-bold;
}

.text-overflow {
    text-overflow: ellipsis;
    overflow: hidden;
    white-space: nowrap;
}

.fade-enter-active {
    transition: opacity 0.2s;
}

.fade-leave-active {
    transition: opacity 0.3s;
}

.fade-enter, .fade-leave-to /* .fade-leave-active below version 2.1.8 */ {
    opacity: 0;
}
</style>

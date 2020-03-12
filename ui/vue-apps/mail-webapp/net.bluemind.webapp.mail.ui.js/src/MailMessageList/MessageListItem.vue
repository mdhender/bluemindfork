<template>
    <bm-draggable
        :tooltip="tooltip"
        name="message"
        :data="message"
        @dragenter="e => setTooltip(e.relatedData)"
        @dragleave="resetTooltip"
        @drop="e => moveMessage(e.relatedData)"
    >
        <bm-list-group-item
            :to="to"
            class="message-list-item"
            active-class="active"
            :class="[...message.states, isMessageSelected(message.key) ? 'active' : '']"
            @mouseenter.native="mouseIn = true"
            @mouseleave.native="mouseIn = false"
        >
            <bm-row class="align-items-center flex-nowrap no-gutters">
                <bm-col cols="1" class="selector">
                    <bm-avatar :alt="from" :class="[anyMessageSelected ? 'd-none' : '']" />
                    <bm-check
                        :checked="isMessageSelected(message.key)"
                        :class="[anyMessageSelected ? 'd-block' : 'd-none']"
                        @click.exact.native.prevent.stop="$emit('toggleSelect', message.key)"
                    />
                </bm-col>
                <bm-col cols="8" class="text-overflow">
                    <div v-bm-tooltip.ds500.viewport :title="from" class="text-overflow mw-100 sender h3 text-dark">
                        {{ from }}
                    </div>
                </bm-col>
                <bm-col cols="3">
                    <transition name="fade" mode="out-in">
                        <div v-if="!quickActionButtonsVisible" class="float-right">
                            <component :is="widget" v-for="widget in widgets" :key="widget.template" />
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
                        class="text-overflow mw-100 subject flex-grow-1"
                    >
                        {{ message.subject }}
                    </div>
                    <div class="pl-2">
                        <span class="text-nowrap d-none d-sm-block d-md-none d-xl-block">{{ displayedDate }}</span>
                        <span class="text-nowrap d-sm-none d-md-block d-xl-none">{{ smallerDisplayedDate }}</span>
                    </div>
                </bm-col>
            </bm-row>
        </bm-list-group-item>
        <template v-slot:shadow>
            <bm-row class="message-list-item-drag-shadow py-2 no-gutters align-items-center">
                <bm-col cols="1" class="text-right">
                    <bm-icon icon="6dots-v" class="bm-drag-handle" />
                </bm-col>
                <bm-col cols="2" class="pl-1">
                    <bm-avatar :alt="from" />
                </bm-col>
                <bm-col cols="9" class="text-overflow font-weight-bold ">
                    {{ message.subject }}
                </bm-col>
            </bm-row>
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
    flagged: {
        components: { BmIcon },
        template: '<bm-icon icon="flag-fill"/>'
    },
    $forwarded: {
        components: { BmIcon },
        template: '<bm-icon icon="forward"/>'
    },
    answered: {
        components: { BmIcon },
        template: '<bm-icon icon="reply"/>'
    }
};

import { BmAvatar, BmCheck, BmCol, BmIcon, BmListGroupItem, BmRow, BmTooltip, BmDraggable } from "@bluemind/styleguide";
import { DateComparator } from "@bluemind/date";
import { mapActions, mapGetters, mapState } from "vuex";
import ItemUri from "@bluemind/item-uri";
import MessageListItemQuickActionButtons from "./MessageListItemQuickActionButtons";

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
        MessageListItemQuickActionButtons
    },
    directives: { BmTooltip },
    props: {
        to: {
            required: false,
            type: [String, Object],
            default: null
        },
        message: {
            type: Object,
            required: true
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
        ...mapState("mail-webapp", ["currentMessageKey", "selectedMessageKeys", "currentFolderKey", "messageFilter"]),
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
                    this.$router.push("" + (this.nextMessageKey || ""));
                }
                this.move({ messageKey: this.message.key, folder: this.getFolderByKey(folder.key) });
            }
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/variables";

.message-list-item {
    cursor: pointer;
}

.message-list-item .bm-avatar {
    width: 1.3rem !important;
    height: 1.3rem !important;
}

.message-list-item .selector .custom-check {
    display: none !important;
    margin-left: 0.825rem;
    padding-left: 0.825rem;
    min-height: 1.4rem;
}

.message-list-item .selector:hover .bm-avatar {
    display: none !important;
}

.message-list-item .selector:hover .custom-check {
    display: block !important;
}

.custom-control-label::after,
.custom-control-label::before {
    top: 0.2rem !important;
}

.list-group-item.message-list-item.not-seen {
    border-left: theme-color("primary") 4px solid !important;
}

a.list-group-item.message-list-item {
    border-left: transparent solid 4px !important;
}

.list-group-item.message-list-item:focus {
    outline: $outline;
    &:hover {
        background-color: $component-active-bg-darken;
    }
}

.message-list-item-drag-shadow {
    width: 240px;
    background-color: $surface-bg;
}

//FIXME: All those class should not be here or should be scoped...
.bm-draggable {
    margin: 1px;
    &:focus {
        outline: $outline !important;
        &:hover {
            background-color: $component-active-bg-darken;
        }
    }
}

.custom-control-label::after,
.custom-control-label::before {
    top: 0.2rem !important;
}

.not-seen .sender,
.not-seen .subject {
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

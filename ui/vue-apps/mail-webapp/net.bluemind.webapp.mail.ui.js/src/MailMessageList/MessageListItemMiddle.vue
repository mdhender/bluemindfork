<template>
    <div class="message-list-item-middle d-flex flex-column text-truncate">
        <div class="d-flex flex-row">
            <div
                v-bm-tooltip.viewport
                :title="from"
                class="mail-message-list-item-sender h3 text-dark text-truncate flex-fill"
            >
                {{ from }}
            </div>
            <transition name="fade-out" mode="out-in">
                <div v-if="isSearchMode && !mouseIn" class="d-flex slide">
                    <mail-folder-icon
                        class="text-secondary text-truncate"
                        :class="[isActive ? 'bg-info' : isImportant ? 'warning-custom' : 'bg-white']"
                        :shared="isFolderOfMailshare(folder)"
                        :folder="folder"
                    >
                        <i class="font-weight-bold">{{ folder.name }}</i>
                    </mail-folder-icon>
                </div>
                <div v-else-if="!mouseIn" class="d-flex justify-content-end">
                    <component :is="widget" v-for="widget in widgets" :key="widget.template" />
                </div>
            </transition>
        </div>
        <div class="d-flex flex-row ">
            <div class="d-flex flex-column flex-fill overflow-hidden">
                <div
                    v-bm-tooltip.viewport
                    :title="message.subject"
                    class="mail-message-list-item-subject text-secondary text-truncate"
                >
                    {{ message.subject }}
                </div>
                <div
                    v-bm-tooltip.viewport
                    :title="message.preview"
                    class="mail-message-list-item-preview text-dark text-condensed text-truncate"
                >
                    {{ message.preview || "&nbsp;" }}
                </div>
            </div>
            <transition name="fade-out" mode="out-in">
                <div v-if="!mouseIn" class="mail-message-list-item-date text-secondary align-self-end">
                    <span class="d-none d-sm-block d-md-none d-xl-block">
                        {{ displayedDate }}
                    </span>
                    <span class="d-sm-none d-md-block d-xl-none">
                        {{ smallerDisplayedDate }}
                    </span>
                </div>
            </transition>
        </div>
    </div>
</template>

<script>
import { BmIcon, BmTooltip } from "@bluemind/styleguide";
import { DateComparator } from "@bluemind/date";
import { Flag } from "@bluemind/email";
import { mapGetters, mapState } from "vuex";
import ItemUri from "@bluemind/item-uri";
import MailFolderIcon from "../MailFolderIcon";

const FLAG_COMPONENT = {
    [Flag.FLAGGED]: {
        components: { BmIcon },
        template: '<bm-icon class="text-warning" icon="flag-fill"/>',
        order: 3
    },
    [Flag.FORWARDED]: {
        components: { BmIcon },
        template: '<bm-icon icon="forward"/>',
        order: 1
    },
    [Flag.ANSWERED]: {
        components: { BmIcon },
        template: '<bm-icon icon="reply"/>',
        order: 2
    }
};

export default {
    name: "MessageListItemLeft",
    directives: { BmTooltip },
    components: { BmIcon, MailFolderIcon },
    props: {
        message: {
            type: Object,
            required: true
        },
        isImportant: {
            type: Boolean,
            required: true
        },
        mouseIn: {
            type: Boolean,
            required: true
        }
    },
    data() {
        return {
            tooltip: {
                cursor: "cursor",
                text: this.$t("mail.actions.move")
            }
        };
    },
    computed: {
        ...mapGetters("mail-webapp", ["isMessageSelected", "my", "isSearchMode"]),
        ...mapState("mail-webapp/currentMessage", { currentMessageKey: "key" }),
        ...mapState("mail", ["mailboxes", "folders"]),
        from() {
            return this.message.from.dn ? this.message.from.dn : this.message.from.address;
        },
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
            return this.message.flags
                .map(flag => FLAG_COMPONENT[flag])
                .filter(widget => !!widget)
                .sort((a, b) => a.order - b.order);
        },
        folder() {
            return Object.values(this.folders).find(folder => folder.key === ItemUri.container(this.message.key));
        },
        isActive() {
            return this.isMessageSelected(this.message.key) || this.message.key === this.currentMessageKey;
        }
    },
    methods: {
        isFolderOfMailshare(folder) {
            return this.mailboxes[folder.mailbox].type === "mailshares";
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/variables";

.message-list-item-middle {
    .custom-control-label::after,
    .custom-control-label::before {
        top: 0.2rem !important;
    }

    .fade-out-leave-active {
        transition: opacity 0s linear 0.15s;
    }

    .fade-out-enter,
    .fade-out-leave-to {
        opacity: 0;
        position: absolute;
        right: $sp-3;
    }
}
</style>

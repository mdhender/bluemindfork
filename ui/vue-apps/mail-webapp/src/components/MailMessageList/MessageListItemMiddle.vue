<template>
    <div class="message-list-item-middle d-flex flex-column text-truncate">
        <div class="d-flex flex-row">
            <div v-if="MY_DRAFTS.key === message.folderRef.key" class="text-danger mx-1 font-italic">
                [{{ $t("common.folder.draft") }}]
            </div>
            <div :title="fromOrTo" class="mail-message-list-item-sender h3 text-dark text-truncate flex-fill">
                {{ fromOrTo }}
            </div>
            <div v-if="MESSAGE_LIST_IS_SEARCH_MODE && !mouseIn" class="d-flex slide">
                <mail-folder-icon
                    class="text-secondary text-truncate"
                    :shared="isFolderOfMailshare(folder)"
                    :folder="folder"
                >
                    <i class="font-weight-bold">{{ folder.name }}</i>
                </mail-folder-icon>
            </div>
            <div v-else-if="!mouseIn" class="d-flex justify-content-end">
                <component :is="widget" v-for="widget in widgets" :key="widget.template" class="ml-2" />
            </div>
        </div>
        <div class="d-flex flex-row">
            <div class="d-flex flex-column flex-fill overflow-hidden">
                <div :title="displayedSubject" class="mail-message-list-item-subject text-secondary text-truncate">
                    {{ displayedSubject }}
                </div>
                <div
                    :title="displayedPreview"
                    class="mail-message-list-item-preview text-dark text-condensed text-truncate"
                >
                    {{ displayedPreview }}
                </div>
            </div>
            <div v-show="!mouseIn" class="mail-message-list-item-date text-secondary align-self-end">
                <span class="d-none d-lg-block">
                    {{ displayedDate }}
                </span>
                <span class="d-block d-lg-none">
                    {{ smallerDisplayedDate }}
                </span>
            </div>
        </div>
    </div>
</template>

<script>
import { BmIcon } from "@bluemind/styleguide";
import { DateComparator } from "@bluemind/date";
import { Flag } from "@bluemind/email";
import { mapGetters, mapState } from "vuex";
import MailFolderIcon from "../MailFolderIcon";
import { MailboxType } from "~model/mailbox";
import { MY_DRAFTS, MY_SENT, MESSAGE_LIST_IS_SEARCH_MODE, MESSAGE_IS_SELECTED } from "~getters";

const FLAG_COMPONENT = {
    [Flag.FLAGGED]: {
        components: { BmIcon },
        template:
            '<bm-icon :aria-label="$t(\'mail.list.flagged.aria\')" aria-hidden="false" class="text-warning" icon="flag-fill"/>',
        order: 3
    },
    [Flag.FORWARDED]: {
        components: { BmIcon },
        template: '<bm-icon :aria-label="$t(\'mail.list.forwarded.aria\')" aria-hidden="false" icon="forward"/>',
        order: 1
    },
    [Flag.ANSWERED]: {
        components: { BmIcon },
        template: '<bm-icon :aria-label="$t(\'mail.list.replied.aria\')" aria-hidden="false" icon="reply"/>',
        order: 2
    }
};

export default {
    name: "MessageListItemMiddle",
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
    computed: {
        ...mapGetters("mail", { MY_DRAFTS, MY_SENT, MESSAGE_LIST_IS_SEARCH_MODE, MESSAGE_IS_SELECTED }),
        ...mapState("mail-webapp/currentMessage", { currentMessageKey: "key" }),
        ...mapState("mail", ["mailboxes", "folders", "messages"]),
        displayedDate: function () {
            const today = new Date();
            const messageDate = this.message.date;
            if (DateComparator.isSameDay(messageDate, today)) {
                return this.$d(messageDate, "short_time");
            } else if (DateComparator.isSameYear(messageDate, today)) {
                return this.$d(messageDate, "relative_date");
            }
            return this.$d(messageDate, "short_date");
        },
        smallerDisplayedDate: function () {
            return this.displayedDate.substring(this.displayedDate.indexOf(" ") + 1);
        },
        widgets() {
            return this.message.flags
                .map(flag => FLAG_COMPONENT[flag])
                .filter(widget => !!widget)
                .sort((a, b) => a.order - b.order);
        },
        folder() {
            return this.folders[this.message.folderRef.key];
        },
        isActive() {
            return this.MESSAGE_IS_SELECTED(this.message.key) || this.message.key === this.currentMessageKey;
        },
        fromOrTo() {
            const messageFolder = this.message.folderRef.key;
            const isSentOrDraftBox = [this.MY_DRAFTS.key, this.MY_SENT.key].includes(messageFolder);
            if (isSentOrDraftBox) {
                return this.message.to.map(to => (to.dn ? to.dn : to.address)).join(", ");
            } else {
                return this.message.from.dn ? this.message.from.dn : this.message.from.address;
            }
        },
        displayedSubject() {
            const subject = this.message.subject;
            if (!subject || subject.trim() === "") {
                return this.$t("mail.viewer.no.subject");
            } else {
                return subject;
            }
        },
        displayedPreview() {
            const preview = this.message.preview;
            if (!preview || preview.trim() === "") {
                return this.$t("mail.viewer.no.preview");
            } else {
                return preview;
            }
        }
    },
    methods: {
        isFolderOfMailshare(folder) {
            return this.mailboxes[folder.mailboxRef.key].type === MailboxType.MAILSHARE;
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

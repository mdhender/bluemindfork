<template>
    <bm-icon v-if="noText" class="mail-folder-icon" fixed-width :icon="icon" :aria-label="ariaLabel" />
    <bm-label-icon
        v-else
        class="mail-folder-icon text-truncate"
        :class="{ 'caption-italic': variant === 'caption' }"
        :icon="icon"
        :icon-size="variant === 'caption' ? 'xs' : 'md'"
        :aria-label="ariaLabel"
        :tooltip="tooltip"
        v-bind="$attrs"
        >{{ folder.name }}</bm-label-icon
    >
</template>

<script>
import { BmLabelIcon, BmIcon } from "@bluemind/ui-components";
import { DEFAULT_FOLDER_NAMES } from "~/store/folders/helpers/DefaultFolders";
import { mailboxUtils } from "@bluemind/mail";

const { MailboxType } = mailboxUtils;

export default {
    name: "MailFolderIcon",
    components: {
        BmLabelIcon,
        BmIcon
    },
    props: {
        folder: {
            type: Object,
            required: true
        },
        mailbox: {
            type: Object,
            default: null
        },
        noText: {
            type: Boolean,
            default: false
        },
        variant: {
            type: String,
            default: "regular",
            validator: function (value) {
                return ["regular", "caption"].includes(value);
            }
        }
    },
    computed: {
        icon() {
            const modifier = MailboxType.isShared(this.mailbox?.type) ? "-shared" : "";
            switch (this.folder.imapName) {
                case DEFAULT_FOLDER_NAMES.INBOX:
                    return "inbox";
                case DEFAULT_FOLDER_NAMES.DRAFTS:
                    return "pencil" + modifier;
                case DEFAULT_FOLDER_NAMES.TRASH:
                    return "trash" + modifier;
                case DEFAULT_FOLDER_NAMES.JUNK:
                    return "forbidden";
                case DEFAULT_FOLDER_NAMES.OUTBOX:
                    return "clock";
                case DEFAULT_FOLDER_NAMES.SENT:
                    return "paper-plane" + modifier;
                case DEFAULT_FOLDER_NAMES.TEMPLATES:
                    return "documents";
                case DEFAULT_FOLDER_NAMES.ROOT:
                    return "user";
                default:
                    return "folder" + modifier;
            }
        },
        ariaLabel() {
            switch (this.mailbox?.type) {
                case MailboxType.MAILSHARE:
                    return this.$t("common.mailshares");
                case MailboxType.GROUP:
                    return this.$t("mail.folders.groups");
                default:
                    return this.folder.name;
            }
        },
        tooltip() {
            if (!this.folder.parent) {
                if (MailboxType.isShared(this.mailbox?.type) && this.mailbox?.address) {
                    return this.mailbox.address;
                }
                return this.folder.name;
            }
            return this.folder.path;
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/mixins";
@import "~@bluemind/ui-components/src/css/variables";

.mail-folder-icon.bm-label-icon {
    & > div {
        @include text-overflow;
    }
    &.caption-italic {
        color: $neutral-fg-lo1;
    }
}
</style>

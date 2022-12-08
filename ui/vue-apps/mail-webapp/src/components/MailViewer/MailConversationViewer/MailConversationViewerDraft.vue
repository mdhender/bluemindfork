<template>
    <mail-conversation-viewer-item
        class="mail-conversation-viewer-draft draft"
        v-bind="$props"
        :is-draft="true"
        v-on="$listeners"
    >
        <template slot="head">
            <div class="conversation-viewer-draft-head z-index-110">
                <span class="text-danger">
                    [<span class="font-italic">{{ $t("common.folder.draft") }}</span
                    >]
                </span>
                <span class="draft-save-date d-none d-lg-inline-block">{{ formattedDraftSaveDate }}</span>
            </div>
            <div class="d-flex flex-fill d-flex justify-content-end align-items-center text-neutral">
                <mail-viewer-draft-toolbar-for-mobile
                    class="d-lg-none"
                    :conversation="conversation"
                    :message="message"
                    @shown="$emit('darken', true)"
                    @hidden="$emit('darken', false)"
                    @edit="$emit('darken', false)"
                />
                <mail-viewer-draft-toolbar class="d-none d-lg-flex" :conversation="conversation" :message="message" />
            </div>
        </template>
        <template slot="subhead">
            <div class="d-flex d-lg-none conversation-viewer-row">
                <div
                    class="vertical-line vertical-line-after-avatar"
                    :class="{ 'vertical-line-transparent': index === conversation.messages.length - 1 }"
                />
                <div class="draft-save-date pb-4">{{ formattedDraftSaveDate }}</div>
            </div>
        </template>
        <template slot="to">
            <span>{{ $t("common.to") }}</span>
            {{ message.to.map(to => to.dn || to.address).join(", ") }}
        </template>
        <template slot="content">
            <div v-if="!isMessageExpanded" class="d-flex flex-fill align-items-center pb-2 pr-3">
                <div class="text-truncate">{{ message.preview }}</div>
            </div>
            <div v-else class="d-flex flex-fill pb-2 pr-3">
                <body-viewer
                    class="flex-fill"
                    :message="message"
                    @remote-content="from => $emit('remote-content', from)"
                />
            </div>
        </template>
        <template slot="bottom">
            <span class="d-flex flex-fill pb-5 text-danger font-italic">{{ $t("mail.draft.not.sent") }}</span>
        </template>
    </mail-conversation-viewer-item>
</template>
<script>
import MailConversationViewerItem from "./MailConversationViewerItem";
import MailViewerDraftToolbar from "../MailViewerDraftToolbar";
import MailViewerDraftToolbarForMobile from "../MailViewerDraftToolbarForMobile";
import MailConversationViewerItemMixin from "./MailConversationViewerItemMixin";
import { FormattedDateMixin } from "~/mixins";
import BodyViewer from "../BodyViewer.vue";

export default {
    name: "MailConversationViewerDraft",
    components: {
        BodyViewer,
        MailConversationViewerItem,
        MailViewerDraftToolbar,
        MailViewerDraftToolbarForMobile
    },
    mixins: [FormattedDateMixin, MailConversationViewerItemMixin],
    computed: {
        formattedDraftSaveDate() {
            const formatted = this.formatMessageDate(this.message);
            return formatted.time ? this.$t("mail.save.date.time", formatted) : this.$t("mail.save.date", formatted);
        }
    }
};
</script>

<style lang="scss">
@import "@bluemind/ui-components/src/css/_type.scss";
@import "~@bluemind/ui-components/src/css/variables";

.mail-conversation-viewer-draft {
    .conversation-viewer-draft-head {
        height: $input-height;
        display: flex;
        gap: $sp-3;
        align-items: center;
    }

    .draft-save-date {
        @extend %regular;
        color: $neutral-fg-lo1;
    }

    .click-to-collapse-zone {
        cursor: pointer;
    }
}
</style>

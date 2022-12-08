<template>
    <div class="mail-conversation-viewer-header conversation-viewer-row pr-3 pb-6 d-flex justify-content-between">
        <div class="title">{{ displayedSubject }}</div>
        <bm-icon-button
            v-if="!expanded"
            :title="$t('common.expand')"
            icon="expand"
            @click="
                $emit('do-show-hidden-messages');
                $emit('expand');
            "
        />
        <bm-icon-button v-else :title="$t('common.collapse')" icon="collapse" @click="$emit('collapse')" />
    </div>
</template>
<script>
import { BmIconButton } from "@bluemind/ui-components";
export default {
    name: "MailConversationViewerHeader",
    components: { BmIconButton },
    props: {
        subject: {
            type: String,
            default: ""
        },
        expanded: {
            type: Boolean,
            required: true
        }
    },
    computed: {
        displayedSubject() {
            if (!this.subject || !this.subject.trim()) {
                return this.$t("mail.viewer.no.subject");
            }
            return this.subject;
        }
    }
};
</script>

<template>
    <div class="mail-conversation-viewer-header pl-5 pb-2 d-flex justify-content-between align-items-center">
        <h1>{{ displayedSubject }}</h1>
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
import { BmIconButton } from "@bluemind/styleguide";
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

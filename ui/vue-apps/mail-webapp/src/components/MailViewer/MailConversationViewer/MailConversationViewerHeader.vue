<template>
    <div class="mail-conversation-viewer-header pl-5 pb-2 d-flex justify-content-between align-items-center">
        <h1>{{ displayedSubject }}</h1>
        <bm-button
            v-if="!expanded"
            class="px-3"
            variant="inline-neutral"
            :title="$t('common.expand')"
            @click="
                $emit('do-show-hidden-messages');
                $emit('expand');
            "
        >
            <bm-icon icon="expand" size="lg" />
        </bm-button>
        <bm-button
            v-else
            class="px-3"
            variant="inline-neutral"
            :title="$t('common.collapse')"
            @click="$emit('collapse')"
        >
            <bm-icon icon="collapse" size="lg" />
        </bm-button>
    </div>
</template>
<script>
import { BmButton, BmIcon } from "@bluemind/styleguide";
export default {
    name: "MailConversationViewerHeader",
    components: { BmButton, BmIcon },
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

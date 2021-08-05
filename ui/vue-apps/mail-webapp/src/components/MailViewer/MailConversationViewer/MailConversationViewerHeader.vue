<template>
    <div class="mail-conversation-viewer-header pl-5 pb-2 d-flex justify-content-between align-items-center">
        <h1>{{ subject }}</h1>
        <bm-button
            v-if="!expanded"
            class="px-3"
            variant="inline-secondary"
            :title="$t('common.expand')"
            @click="
                $emit('do-show-hidden-messages');
                $emit('expand');
            "
        >
            <bm-icon icon="expand" />
        </bm-button>
        <bm-button
            v-else
            class="px-3"
            variant="inline-secondary"
            :title="$t('common.collapse')"
            @click="$emit('collapse')"
        >
            <bm-icon icon="collapse" />
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
            required: true
        },
        expanded: {
            type: Boolean,
            required: true
        }
    }
};
</script>

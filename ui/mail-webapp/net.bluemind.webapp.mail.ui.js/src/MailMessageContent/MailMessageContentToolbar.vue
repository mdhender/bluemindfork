<template>
    <bm-button-toolbar key-nav class="mail-message-content-toolbar float-right">
        <bm-button
            v-bm-tooltip.d500 
            variant="outline-primary"
            :aria-label="$t('mail.content.reply.aria')"
            :title="$t('mail.content.reply.aria')"

            :to="{ name: 'replyTo'}"
        >
            <bm-icon icon="reply" size="2x" />
        </bm-button>
        <bm-button
            v-bm-tooltip.d500 
            variant="outline-primary"
            :aria-label="$t('mail.content.reply_all.aria')"
            :title="$t('mail.content.reply_all.aria')"
            :to="{ name: 'replyToAll'}"
        >
            <bm-icon icon="reply-all" size="2x" />
        </bm-button>
        <bm-button
            v-bm-tooltip.d500 
            variant="outline-primary"
            :aria-label="$t('mail.content.forward.aria')"
            :title="$t('mail.content.forward.aria')"
            :to="{ name: 'forwardTo'}"
        >
            <bm-icon icon="forward" size="2x" />
        </bm-button>
    </bm-button-toolbar>
</template>
    
<script>
import { BmButton, BmButtonToolbar, BmIcon, BmTooltip } from "@bluemind/styleguide";

export default {
    name: "MailMessageContentToolbar",
    components: {
        BmButton,
        BmButtonToolbar,
        BmIcon
    },
    directives: { BmTooltip }
};
</script>

<style scoped>
.mail-message-content-toolbar .btn.btn-outline-primary {
    border: none;
}

</style>
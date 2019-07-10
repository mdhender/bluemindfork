<template>
    <bm-button-toolbar key-nav class="mail-message-content-toolbar float-right">
        <bm-button variant="primary" :aria-label="$t('mail.content.reply.aria')" :to="{ name: 'replyTo'}">
            <bm-icon icon="reply" size="2x" />
        </bm-button>
        <bm-button variant="primary" :aria-label="$t('mail.content.reply_all.aria')" :to="{ name: 'replyToAll'}">
            <bm-icon icon="reply-all" size="2x" />
        </bm-button>
        <bm-button variant="primary" :aria-label="$t('mail.content.forward.aria')" :to="{ name: 'forwardTo'}">
            <bm-icon icon="forward" size="2x" />
        </bm-button>
        <bm-button variant="primary" :aria-label="$t('mail.content.3dots.aria')">
            <bm-icon icon="3dots" size="2x" />
        </bm-button>
    </bm-button-toolbar>
</template>
    
<script>
import BmButton from "@bluemind/styleguide/components/buttons/BmButton";
import BmButtonToolbar from "@bluemind/styleguide/components/buttons/BmButtonToolbar";
import BmIcon from "@bluemind/styleguide/components/BmIcon";

export default {
    name: "MailMessageContentToolbar",
    components: {
        BmButton,
        BmButtonToolbar,
        BmIcon
    }
};
</script>
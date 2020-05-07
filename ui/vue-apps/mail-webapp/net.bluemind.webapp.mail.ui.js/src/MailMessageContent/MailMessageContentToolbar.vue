<template>
    <bm-button-toolbar key-nav class="mail-message-content-toolbar float-right">
        <bm-button
            v-bm-tooltip.ds500
            variant="outline-primary"
            :aria-label="$t('mail.content.reply.aria')"
            :title="$t('mail.content.reply.aria')"
            :to="$router.relative('mail:reply', $route)"
        >
            <bm-icon icon="reply" size="2x" />
        </bm-button>
        <bm-button
            v-bm-tooltip.ds500
            variant="outline-primary"
            :aria-label="$t('mail.content.reply_all.aria')"
            :title="$t('mail.content.reply_all.aria')"
            :to="$router.relative('mail:replyAll', $route)"
        >
            <bm-icon icon="reply-all" size="2x" />
        </bm-button>
        <bm-button
            v-bm-tooltip.ds500
            variant="outline-primary"
            :aria-label="$t('mail.content.forward.aria')"
            :title="$t('mail.content.forward.aria')"
            :to="$router.relative('mail:forward', $route)"
        >
            <bm-icon icon="forward" size="2x" />
        </bm-button>
    </bm-button-toolbar>
</template>

<script>
import { BmButton, BmButtonToolbar, BmIcon, BmTooltip } from "@bluemind/styleguide";
import { mapState } from "vuex";

export default {
    name: "MailMessageContentToolbar",
    components: {
        BmButton,
        BmButtonToolbar,
        BmIcon
    },
    directives: { BmTooltip },
    computed: {
        ...mapState("mail-webapp", ["currentFolderKey"]),
        ...mapState("mail-webapp/currentMessage", { currentMessageKey: "key" })
    }
};
</script>

<style scoped>
.mail-message-content-toolbar .btn.btn-outline-primary {
    border: none;
}
</style>

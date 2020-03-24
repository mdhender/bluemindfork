<template>
    <bm-button-toolbar key-nav class="mail-message-content-toolbar float-right">
        <bm-button
            v-bm-tooltip.ds500
            variant="outline-primary"
            :aria-label="$t('mail.content.reply.aria')"
            :title="$t('mail.content.reply.aria')"
            :to="computeRoute('reply')"
        >
            <bm-icon icon="reply" size="2x" />
        </bm-button>
        <bm-button
            v-bm-tooltip.ds500
            variant="outline-primary"
            :aria-label="$t('mail.content.reply_all.aria')"
            :title="$t('mail.content.reply_all.aria')"
            :to="computeRoute('replyAll')"
        >
            <bm-icon icon="reply-all" size="2x" />
        </bm-button>
        <bm-button
            v-bm-tooltip.ds500
            variant="outline-primary"
            :aria-label="$t('mail.content.forward.aria')"
            :title="$t('mail.content.forward.aria')"
            :to="computeRoute('forward')"
        >
            <bm-icon icon="forward" size="2x" />
        </bm-button>
    </bm-button-toolbar>
</template>

<script>
import MailRouterMixin from "../MailRouterMixin";
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
    mixins: [MailRouterMixin],
    computed: {
        ...mapState("mail-webapp", ["currentFolderKey"]),
        ...mapState("mail-webapp/currentMessage", { currentMessageKey: "key" })
    },
    methods: {
        computeRoute(suffix) {
            return this.computeMessageRoute(this.currentFolderKey, this.currentMessageKey) + "/" + suffix;
        }
    }
};
</script>

<style scoped>
.mail-message-content-toolbar .btn.btn-outline-primary {
    border: none;
}
</style>

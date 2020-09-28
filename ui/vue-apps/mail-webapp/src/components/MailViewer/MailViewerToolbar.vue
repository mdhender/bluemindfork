<template>
    <bm-button-toolbar key-nav class="mail-viewer-toolbar float-right">
        <bm-button
            v-bm-tooltip
            variant="simple-primary"
            :aria-label="$t('mail.content.reply.aria')"
            :title="$t('mail.content.reply.aria')"
            @click="composeReplyOrForward(MessageCreationModes.REPLY)"
        >
            <bm-icon icon="reply" size="2x" />
        </bm-button>
        <bm-button
            v-bm-tooltip
            variant="simple-primary"
            :aria-label="$t('mail.content.reply_all.aria')"
            :title="$t('mail.content.reply_all.aria')"
            @click="composeReplyOrForward(MessageCreationModes.REPLY_ALL)"
        >
            <bm-icon icon="reply-all" size="2x" />
        </bm-button>
        <bm-button
            v-bm-tooltip
            variant="simple-primary"
            :aria-label="$t('mail.content.forward.aria')"
            :title="$t('mail.content.forward.aria')"
            @click="composeReplyOrForward(MessageCreationModes.FORWARD)"
        >
            <bm-icon icon="forward" size="2x" />
        </bm-button>
    </bm-button-toolbar>
</template>

<script>
import { mapActions, mapGetters, mapState } from "vuex";

import { BmButton, BmButtonToolbar, BmIcon, BmTooltip } from "@bluemind/styleguide";

import { MessageCreationModes } from "../../model/message";
import actionTypes from "../../store/actionTypes";

export default {
    name: "MailViewerToolbar",
    components: {
        BmButton,
        BmButtonToolbar,
        BmIcon
    },
    directives: { BmTooltip },
    data() {
        return {
            MessageCreationModes
        };
    },
    computed: {
        ...mapState("mail-webapp/currentMessage", { currentMessageKey: "key" }),
        ...mapGetters("mail", ["MY_DRAFTS"])
    },
    methods: {
        ...mapActions("mail", [actionTypes.CREATE_MESSAGE]),
        async composeReplyOrForward(creationMode) {
            const messageKey = await this.CREATE_MESSAGE({
                myDraftsFolder: this.MY_DRAFTS,
                creationMode,
                previousMessageKey: this.currentMessageKey
            });
            return this.$router.navigate({ name: "v:mail:message", params: { message: messageKey } });
        }
    }
};
</script>

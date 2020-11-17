<template>
    <bm-button-toolbar
        key-nav
        class="mail-viewer-toolbar float-right mail-viewer-mobile-actions bg-white position-sticky"
    >
        <bm-button
            variant="simple-primary"
            :aria-label="$t('mail.content.reply.aria')"
            :title="$t('mail.content.reply.aria')"
            @click="composeReplyOrForward(MessageCreationModes.REPLY)"
        >
            <bm-icon icon="reply" size="2x" />
            <span class="d-lg-none">{{ $t("mail.content.reply.aria") }}</span>
        </bm-button>
        <bm-button
            variant="simple-primary"
            :aria-label="$t('mail.content.reply_all.aria')"
            :title="$t('mail.content.reply_all.aria')"
            @click="composeReplyOrForward(MessageCreationModes.REPLY_ALL)"
        >
            <bm-icon icon="reply-all" size="2x" />
            <span class="d-lg-none">{{ $t("mail.content.reply_all.aria") }}</span>
        </bm-button>
        <bm-button
            variant="simple-primary"
            :aria-label="$t('mail.content.forward.aria')"
            :title="$t('mail.content.forward.aria')"
            @click="composeReplyOrForward(MessageCreationModes.FORWARD)"
        >
            <bm-icon icon="forward" size="2x" />
            <span class="d-lg-none">{{ $t("mail.content.forward.aria") }}</span>
        </bm-button>
    </bm-button-toolbar>
</template>

<script>
import { mapActions, mapGetters, mapMutations, mapState } from "vuex";

import { sanitizeHtml } from "@bluemind/html-utils";
import { InlineImageHelper, MimeType } from "@bluemind/email";
import { inject } from "@bluemind/inject";
import { BmButton, BmButtonToolbar, BmIcon } from "@bluemind/styleguide";

import { FETCH_ACTIVE_MESSAGE_INLINE_PARTS } from "~actions";
import { MY_DRAFTS } from "~getters";
import { ADD_MESSAGES, SET_DRAFT_COLLAPSED_CONTENT, SET_DRAFT_EDITOR_CONTENT } from "~mutations";
import { addSeparator, createReplyOrForward, getEditorContent, COMPOSER_CAPABILITIES } from "../../model/draft";
import { MessageCreationModes } from "../../model/message";
import { getPartsFromCapabilities } from "../../model/part";
import { addSignature } from "../../model/signature";

export default {
    name: "MailViewerToolbar",
    components: {
        BmButton,
        BmButtonToolbar,
        BmIcon
    },
    data() {
        return { MessageCreationModes };
    },
    computed: {
        ...mapGetters("mail", { MY_DRAFTS }),
        ...mapState("mail-webapp/currentMessage", { currentMessageKey: "key" }),
        ...mapState("mail", ["messages", "messageCompose", "activeMessage"]),
        ...mapState("session", { settings: "userSettings" })
    },
    methods: {
        ...mapActions("mail", { FETCH_ACTIVE_MESSAGE_INLINE_PARTS }),
        ...mapMutations("mail", [ADD_MESSAGES, SET_DRAFT_EDITOR_CONTENT, SET_DRAFT_COLLAPSED_CONTENT]),
        async composeReplyOrForward(creationMode) {
            const previousMessage = this.messages[this.currentMessageKey];
            const message = createReplyOrForward(previousMessage, this.MY_DRAFTS, inject("UserSession"), creationMode);
            this.ADD_MESSAGES([message]);

            const parts = getPartsFromCapabilities(previousMessage, COMPOSER_CAPABILITIES);

            await this.FETCH_ACTIVE_MESSAGE_INLINE_PARTS({
                folderUid: previousMessage.folderRef.uid,
                imapUid: previousMessage.remoteRef.imapUid,
                inlines: parts.filter(
                    part => MimeType.isHtml(part) || MimeType.isText(part) || (MimeType.isImage(part) && part.contentId)
                )
            });

            const userPrefTextOnly = false; // FIXME with user settings
            let contentFromPreviousMessage = getEditorContent(
                userPrefTextOnly,
                parts,
                previousMessage,
                this.activeMessage.partsDataByAddress
            );
            if (!userPrefTextOnly) {
                const partsWithCid = parts.filter(part => MimeType.isImage(part) && part.contentId);
                const insertionResult = await InlineImageHelper.insertAsBase64(
                    [contentFromPreviousMessage],
                    partsWithCid,
                    this.activeMessage.partsDataByAddress
                );
                contentFromPreviousMessage = insertionResult.contentsWithImageInserted[0];
                contentFromPreviousMessage = sanitizeHtml(contentFromPreviousMessage);
            }
            const collapsed = addSeparator(
                contentFromPreviousMessage,
                previousMessage,
                creationMode,
                userPrefTextOnly,
                inject("i18n")
            );

            const content =
                this.messageCompose.signature && this.settings.insert_signature === "true"
                    ? addSignature("", userPrefTextOnly, this.messageCompose.signature)
                    : "";
            this.SET_DRAFT_EDITOR_CONTENT(content);
            this.SET_DRAFT_COLLAPSED_CONTENT(collapsed);

            this.$router.navigate({ name: "v:mail:message", params: { message: message.key } });
        }
    }
};
</script>
<style lang="scss" scoped>
@import "~@bluemind/styleguide/css/_variables";
@media (max-width: map-get($grid-breakpoints, "lg")) {
    .mail-viewer-mobile-actions {
        bottom: 0;
        box-shadow: 0 -0.125rem 0.125rem rgba($dark, 0.25);
        justify-content: space-evenly;
    }
}
</style>

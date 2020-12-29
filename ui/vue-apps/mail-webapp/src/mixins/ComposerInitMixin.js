import { mapActions, mapGetters, mapMutations, mapState } from "vuex";

import { InlineImageHelper, MimeType } from "@bluemind/email";
import { inject } from "@bluemind/inject";
import { sanitizeHtml } from "@bluemind/html-utils";

import { FETCH_ACTIVE_MESSAGE_INLINE_PARTS } from "~actions";
import { MY_DRAFTS } from "~getters";
import {
    ADD_MESSAGES,
    SET_ATTACHMENTS_FORWARDED,
    SET_DRAFT_COLLAPSED_CONTENT,
    SET_DRAFT_EDITOR_CONTENT,
    SET_SAVED_INLINE_IMAGES
} from "~mutations";
import { addSignature } from "~model/signature";
import { getPartsFromCapabilities } from "~model/part";
// FIXME: all those methods are helper of mixin, not model..
import {
    addSeparator,
    COMPOSER_CAPABILITIES,
    createEmpty,
    createReplyOrForward,
    getEditorContent,
    handleSeparator
} from "~model/draft";
import { MessageCreationModes } from "~model/message";

/**
 * Manage different cases of composer initialization
 */
export default {
    data() {
        return {
            userPrefTextOnly: false // FIXME: https://forge.bluemind.net/jira/browse/FEATWEBML-88
        };
    },
    computed: {
        ...mapGetters("mail", { $_ComposerInitMixin_MY_DRAFTS: MY_DRAFTS }),
        ...mapState("mail", { $_ComposerInitMixin_activeMessage: "activeMessage" }),
        ...mapState("mail", { $_ComposerInitMixin_signature: ({ messageCompose }) => messageCompose.signature }),
        ...mapState("session", {
            $_ComposerInitMixin_insertSignaturePref: ({ userSettings }) => userSettings.insert_signature
        })
    },
    methods: {
        ...mapActions("mail", {
            $_ComposerInitMixin_FETCH_ACTIVE_MESSAGE_INLINE_PARTS: FETCH_ACTIVE_MESSAGE_INLINE_PARTS
        }),
        ...mapMutations("mail", {
            $_ComposerInitMixin_ADD_MESSAGES: ADD_MESSAGES,
            $_ComposerInitMixin_SET_DRAFT_COLLAPSED_CONTENT: SET_DRAFT_COLLAPSED_CONTENT,
            $_ComposerInitMixin_SET_DRAFT_EDITOR_CONTENT: SET_DRAFT_EDITOR_CONTENT,
            $_ComposerInitMixin_SET_SAVED_INLINE_IMAGES: SET_SAVED_INLINE_IMAGES,
            $_ComposerInitMixin_SET_ATTACHMENTS_FORWARDED: SET_ATTACHMENTS_FORWARDED
        }),

        // case when user clicks on a message in MY_DRAFTS folder
        async initFromRemoteMessage(message) {
            const parts = getPartsFromCapabilities(message, COMPOSER_CAPABILITIES);

            await this.$_ComposerInitMixin_FETCH_ACTIVE_MESSAGE_INLINE_PARTS({
                folderUid: message.folderRef.uid,
                imapUid: message.remoteRef.imapUid,
                inlines: parts.filter(part => MimeType.isHtml(part) || MimeType.isText(part))
            });

            let content = getEditorContent(
                this.userPrefTextOnly,
                parts,
                message,
                this.$_ComposerInitMixin_activeMessage.partsDataByAddress
            );
            if (!this.userPrefTextOnly) {
                const partsWithCid = parts.filter(part => MimeType.isImage(part) && part.contentId);

                const insertionResult = await InlineImageHelper.insertAsUrl(
                    [content],
                    partsWithCid,
                    message.folderRef.uid,
                    message.remoteRef.imapUid
                );
                this.$_ComposerInitMixin_SET_SAVED_INLINE_IMAGES(insertionResult.imageInlined);
                content = insertionResult.contentsWithImageInserted[0];
                content = sanitizeHtml(content);
            }
            const editorData = handleSeparator(content);

            if (this.$_ComposerInitMixin_signature && this.$_ComposerInitMixin_insertSignaturePref === "true") {
                editorData.content = addSignature(
                    editorData.content,
                    this.userPrefTextOnly,
                    this.$_ComposerInitMixin_signature
                );
            }

            this.$_ComposerInitMixin_SET_DRAFT_COLLAPSED_CONTENT(editorData.collapsed);
            this.$_ComposerInitMixin_SET_DRAFT_EDITOR_CONTENT(editorData.content);
        },

        // case of a new message
        async initNewMessage() {
            const message = createEmpty(this.$_ComposerInitMixin_MY_DRAFTS, inject("UserSession"));
            this.$_ComposerInitMixin_ADD_MESSAGES([message]);
            let content = "";
            if (this.$_ComposerInitMixin_signature && this.$_ComposerInitMixin_insertSignaturePref === "true") {
                content = addSignature(content, this.userPrefTextOnly, this.$_ComposerInitMixin_signature);
            }
            this.$_ComposerInitMixin_SET_DRAFT_EDITOR_CONTENT(content);
            this.$_ComposerInitMixin_SET_DRAFT_COLLAPSED_CONTENT(null);
            this.$_ComposerInitMixin_SET_SAVED_INLINE_IMAGES([]);
            return this.$router.navigate({ name: "v:mail:message", params: { message: message.key } });
        },

        // case of a reply or forward message
        async initReplyOrForward(creationMode, previousMessage) {
            const message = createReplyOrForward(
                previousMessage,
                this.$_ComposerInitMixin_MY_DRAFTS,
                inject("UserSession"),
                creationMode
            );
            this.$_ComposerInitMixin_ADD_MESSAGES([message]);

            const parts = getPartsFromCapabilities(previousMessage, COMPOSER_CAPABILITIES);

            await this.$_ComposerInitMixin_FETCH_ACTIVE_MESSAGE_INLINE_PARTS({
                folderUid: previousMessage.folderRef.uid,
                imapUid: previousMessage.remoteRef.imapUid,
                inlines: parts.filter(
                    part => MimeType.isHtml(part) || MimeType.isText(part) || (MimeType.isImage(part) && part.contentId)
                )
            });

            let contentFromPreviousMessage = getEditorContent(
                this.userPrefTextOnly,
                parts,
                previousMessage,
                this.$_ComposerInitMixin_activeMessage.partsDataByAddress
            );

            if (!this.userPrefTextOnly) {
                const partsWithCid = parts.filter(part => MimeType.isImage(part) && part.contentId);
                const insertionResult = await InlineImageHelper.insertAsBase64(
                    [contentFromPreviousMessage],
                    partsWithCid,
                    this.$_ComposerInitMixin_activeMessage.partsDataByAddress
                );
                contentFromPreviousMessage = insertionResult.contentsWithImageInserted[0];
                contentFromPreviousMessage = sanitizeHtml(contentFromPreviousMessage);
            }
            const collapsed = addSeparator(
                contentFromPreviousMessage,
                previousMessage,
                creationMode,
                this.userPrefTextOnly,
                inject("i18n")
            );

            const content =
                this.$_ComposerInitMixin_signature && this.$_ComposerInitMixin_insertSignaturePref === "true"
                    ? addSignature("", this.userPrefTextOnly, this.$_ComposerInitMixin_signature)
                    : "";
            this.$_ComposerInitMixin_SET_DRAFT_EDITOR_CONTENT(content);
            this.$_ComposerInitMixin_SET_DRAFT_COLLAPSED_CONTENT(collapsed);
            this.$_ComposerInitMixin_SET_SAVED_INLINE_IMAGES([]);

            if (creationMode === MessageCreationModes.FORWARD) {
                const forwardedAttachments = await uploadAttachments(previousMessage);
                this.$_ComposerInitMixin_SET_ATTACHMENTS_FORWARDED(forwardedAttachments);
            }

            this.$router.navigate({ name: "v:mail:message", params: { message: message.key } });
        }
    }
};

function uploadAttachments(previousMessage) {
    const service = inject("MailboxItemsPersistence", previousMessage.folderRef.uid);
    return Promise.all(
        previousMessage.attachments.map(attachment =>
            service.fetch(
                previousMessage.remoteRef.imapUid,
                attachment.address,
                attachment.encoding,
                attachment.mime,
                attachment.charset
            )
        )
    );
}

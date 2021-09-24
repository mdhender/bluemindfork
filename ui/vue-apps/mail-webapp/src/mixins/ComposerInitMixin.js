import { mapActions, mapGetters, mapMutations, mapState } from "vuex";

import { InlineImageHelper, MimeType } from "@bluemind/email";
import { inject } from "@bluemind/inject";
import { sanitizeHtml } from "@bluemind/html-utils";

import { FETCH_PART_DATA, FETCH_MESSAGE_IF_NOT_LOADED } from "~/actions";
import { MY_DRAFTS } from "~/getters";
import {
    ADD_MESSAGES,
    SET_ATTACHMENTS_FORWARDED,
    SET_DRAFT_COLLAPSED_CONTENT,
    SET_DRAFT_EDITOR_CONTENT,
    SET_MESSAGE_TMP_ADDRESSES,
    SET_SAVED_INLINE_IMAGES
} from "~/mutations";
import { addSignature } from "~/model/signature";
import { getPartsFromCapabilities } from "~/model/part";
// FIXME: all those methods are helper of mixin, not model..
import {
    addSeparator,
    COMPOSER_CAPABILITIES,
    createEmpty,
    createReplyOrForward,
    getEditorContent,
    handleSeparator
} from "~/model/draft";
import { MessageCreationModes } from "~/model/message";
import apiMessages from "~/store/api/apiMessages";

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
        ...mapState("mail", ["activeFolder", "folders"]),
        ...mapGetters("mail", { $_ComposerInitMixin_MY_DRAFTS: MY_DRAFTS }),
        ...mapState("mail", { $_ComposerInitMixin_partsByMessageKey: ({ partsData }) => partsData.partsByMessageKey }),
        ...mapState("session", { $_ComposerInitMixin_settings: ({ settings }) => settings.remote }),
        $_ComposerInitMixin_insertSignaturePref() {
            return this.$_ComposerInitMixin_settings.insert_signature;
        },
        $_ComposerInitMixin_lang() {
            return this.$_ComposerInitMixin_settings.lang;
        },
        ...mapGetters("root-app", { $_ComposerInitMixin_defaultIdentity: "DEFAULT_IDENTITY" }),
        $_ComposerInitMixin_signature() {
            return this.$_ComposerInitMixin_defaultIdentity.signature;
        }
    },
    methods: {
        ...mapActions("mail", {
            $_ComposerInitMixin_FETCH_PART_DATA: FETCH_PART_DATA
        }),
        ...mapMutations("mail", {
            $_ComposerInitMixin_ADD_MESSAGES: ADD_MESSAGES,
            $_ComposerInitMixin_SET_ATTACHMENTS_FORWARDED: SET_ATTACHMENTS_FORWARDED,
            $_ComposerInitMixin_SET_DRAFT_COLLAPSED_CONTENT: SET_DRAFT_COLLAPSED_CONTENT,
            $_ComposerInitMixin_SET_DRAFT_EDITOR_CONTENT: SET_DRAFT_EDITOR_CONTENT,
            $_ComposerInitMixin_SET_MESSAGE_TMP_ADDRESSES: SET_MESSAGE_TMP_ADDRESSES,
            $_ComposerInitMixin_SET_SAVED_INLINE_IMAGES: SET_SAVED_INLINE_IMAGES
        }),

        // case when user clicks on a message in MY_DRAFTS folder
        async initFromRemoteMessage(message) {
            const messageWithTmpAddresses = await apiMessages.getForUpdate(message);
            this.$_ComposerInitMixin_SET_MESSAGE_TMP_ADDRESSES({
                key: message.key,
                attachments: messageWithTmpAddresses.attachments,
                inlinePartsByCapabilities: messageWithTmpAddresses.inlinePartsByCapabilities
            });

            const parts = getPartsFromCapabilities(messageWithTmpAddresses, COMPOSER_CAPABILITIES);

            await this.$_ComposerInitMixin_FETCH_PART_DATA({
                messageKey: message.key,
                folderUid: message.folderRef.uid,
                imapUid: message.remoteRef.imapUid,
                inlines: parts.filter(part => MimeType.isHtml(part) || MimeType.isText(part))
            });

            let content = getEditorContent(
                this.userPrefTextOnly,
                parts,
                this.$_ComposerInitMixin_partsByMessageKey[message.key],
                this.$_ComposerInitMixin_lang
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

        async initRelatedMessage(action, related) {
            switch (action) {
                case MessageCreationModes.REPLY:
                case MessageCreationModes.REPLY_ALL:
                case MessageCreationModes.FORWARD:
                    try {
                        const previous = await this.$store.dispatch("mail/" + FETCH_MESSAGE_IF_NOT_LOADED, {
                            internalId: related.internalId,
                            folder: this.$store.state.mail.folders[related.folderKey]
                        });
                        return this.initReplyOrForward(action, previous);
                    } catch {
                        return this.initNewMessage();
                    }
                default:
                    return this.initNewMessage();
            }
        },

        // case of a new message
        initNewMessage() {
            const message = createEmpty(this.$_ComposerInitMixin_MY_DRAFTS, inject("UserSession"));
            this.$_ComposerInitMixin_ADD_MESSAGES([message]);
            let content = "";
            if (this.$_ComposerInitMixin_signature && this.$_ComposerInitMixin_insertSignaturePref === "true") {
                content = addSignature(content, this.userPrefTextOnly, this.$_ComposerInitMixin_signature);
            }
            this.$_ComposerInitMixin_SET_DRAFT_EDITOR_CONTENT(content);
            this.$_ComposerInitMixin_SET_DRAFT_COLLAPSED_CONTENT(null);

            this.$_ComposerInitMixin_SET_SAVED_INLINE_IMAGES([]);
            return message;
        },

        // case of a reply or forward message
        async initReplyOrForward(creationMode, previousMessage) {
            const message = createReplyOrForward(
                previousMessage,
                this.$_ComposerInitMixin_MY_DRAFTS,
                inject("UserSession"),
                creationMode
            );

            const parts = getPartsFromCapabilities(previousMessage, COMPOSER_CAPABILITIES);

            await this.$_ComposerInitMixin_FETCH_PART_DATA({
                messageKey: previousMessage.key,
                folderUid: previousMessage.folderRef.uid,
                imapUid: previousMessage.remoteRef.imapUid,
                inlines: parts.filter(
                    part => MimeType.isHtml(part) || MimeType.isText(part) || (MimeType.isImage(part) && part.contentId)
                )
            });

            let contentFromPreviousMessage = getEditorContent(
                this.userPrefTextOnly,
                parts,
                this.$_ComposerInitMixin_partsByMessageKey[previousMessage.key],
                this.$_ComposerInitMixin_lang
            );

            if (!this.userPrefTextOnly) {
                const partsWithCid = parts.filter(part => MimeType.isImage(part) && part.contentId);
                const insertionResult = await InlineImageHelper.insertAsBase64(
                    [contentFromPreviousMessage],
                    partsWithCid,
                    this.$_ComposerInitMixin_partsByMessageKey[previousMessage.key]
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

            if (creationMode !== MessageCreationModes.FORWARD) {
                message.conversationRef = { ...previousMessage.conversationRef };
            }
            this.$_ComposerInitMixin_ADD_MESSAGES([message]);

            if (creationMode === MessageCreationModes.FORWARD) {
                const forwardedAttachments = await uploadAttachments(previousMessage);
                this.$_ComposerInitMixin_SET_ATTACHMENTS_FORWARDED(forwardedAttachments);
            }

            return message;
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

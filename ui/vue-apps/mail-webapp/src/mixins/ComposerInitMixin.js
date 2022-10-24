import { mapActions, mapGetters, mapMutations, mapState } from "vuex";

import { ERROR } from "@bluemind/alert.store";
import { InlineImageHelper, MimeType } from "@bluemind/email";
import { inject } from "@bluemind/inject";
import { sanitizeHtml } from "@bluemind/html-utils";
import { BmRichEditor } from "@bluemind/styleguide";
import { attachmentUtils, draftUtils, loadingStatusUtils, messageUtils, partUtils } from "@bluemind/mail";

import { FETCH_PART_DATA, FETCH_MESSAGE_IF_NOT_LOADED } from "~/actions";
import { MY_DRAFTS } from "~/getters";
import {
    ADD_ATTACHMENT,
    ADD_FILES,
    ADD_MESSAGES,
    SET_ATTACHMENTS,
    SET_DRAFT_COLLAPSED_CONTENT,
    SET_DRAFT_EDITOR_CONTENT,
    SET_MESSAGE_BCC,
    SET_MESSAGE_CC,
    SET_MESSAGES_LOADING_STATUS,
    SET_MESSAGE_SUBJECT,
    SET_MESSAGE_TMP_ADDRESSES,
    SET_MESSAGE_TO,
    SET_SAVED_INLINE_IMAGES
} from "~/mutations";
import apiMessages from "~/store/api/apiMessages";
import { ComposerFromMixin } from "~/mixins";
import { AddAttachmentsCommand } from "~/commands";

const { LoadingStatus } = loadingStatusUtils;
const {
    COMPOSER_CAPABILITIES,
    computeSubject,
    createEmpty,
    createReplyOrForward,
    getEditorContent,
    handleSeparator,
    quotePreviousMessage
} = draftUtils;
const { getPartsFromCapabilities } = partUtils;
const { AttachmentAdaptor } = attachmentUtils;
const { MessageCreationModes } = messageUtils;

/**
 * Manage different cases of composer initialization
 */
export default {
    data() {
        return {
            userPrefTextOnly: false // FIXME: https://forge.bluemind.net/jira/browse/FEATWEBML-88
        };
    },
    mixins: [AddAttachmentsCommand, ComposerFromMixin],
    computed: {
        ...mapState("mail", { $_ComposerInitMixin_partsByMessageKey: ({ partsData }) => partsData.partsByMessageKey }),
        ...mapGetters("mail", { $_ComposerInitMixin_MY_DRAFTS: MY_DRAFTS }),
        $_ComposerInitMixin_lang() {
            return this.$store.state.settings.lang;
        }
    },
    methods: {
        ...mapActions("alert", { ERROR }),
        ...mapActions("mail", { $_ComposerInitMixin_FETCH_PART_DATA: FETCH_PART_DATA }),
        ...mapMutations("mail", {
            $_ComposerInitMixin_ADD_FILES: ADD_FILES,
            $_ComposerInitMixin_ADD_MESSAGES: ADD_MESSAGES,
            $_ComposerInitMixin_SET_DRAFT_COLLAPSED_CONTENT: SET_DRAFT_COLLAPSED_CONTENT,
            $_ComposerInitMixin_SET_DRAFT_EDITOR_CONTENT: SET_DRAFT_EDITOR_CONTENT,
            $_ComposerInitMixin_SET_MESSAGE_TMP_ADDRESSES: SET_MESSAGE_TMP_ADDRESSES,
            $_ComposerInitMixin_SET_SAVED_INLINE_IMAGES: SET_SAVED_INLINE_IMAGES
        }),

        // case when user clicks on a message in MY_DRAFTS folder
        async initFromRemoteMessage(message) {
            const messageWithTmpAddresses = await apiMessages.getForUpdate(message);
            const { files, attachments } = AttachmentAdaptor.extractFiles(messageWithTmpAddresses.attachments, message);
            this.$_ComposerInitMixin_ADD_FILES({ files });
            this.$_ComposerInitMixin_SET_MESSAGE_TMP_ADDRESSES({
                key: message.key,
                attachments: attachments,
                inlinePartsByCapabilities: messageWithTmpAddresses.inlinePartsByCapabilities
            });
            const parts = getPartsFromCapabilities(messageWithTmpAddresses, COMPOSER_CAPABILITIES);

            await this.$_ComposerInitMixin_FETCH_PART_DATA({
                messageKey: message.key,
                folderUid: message.folderRef.uid,
                imapUid: message.remoteRef.imapUid,
                parts: parts.filter(part => MimeType.isHtml(part) || MimeType.isText(part))
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
            this.$_ComposerInitMixin_SET_DRAFT_COLLAPSED_CONTENT(editorData.collapsed);
            this.$_ComposerInitMixin_SET_DRAFT_EDITOR_CONTENT(editorData.content);
        },

        async initRelatedMessage(folder, action, related) {
            try {
                const fetchRelatedFn = () =>
                    this.$store.dispatch("mail/" + FETCH_MESSAGE_IF_NOT_LOADED, {
                        internalId: related.internalId,
                        folder: this.$store.state.mail.folders[related.folderKey]
                    });
                switch (action) {
                    case MessageCreationModes.REPLY:
                    case MessageCreationModes.REPLY_ALL:
                    case MessageCreationModes.FORWARD: {
                        const previous = await fetchRelatedFn();
                        return this.initReplyOrForward(folder, action, previous);
                    }
                    case MessageCreationModes.EDIT_AS_NEW: {
                        const previous = await fetchRelatedFn();
                        return this.initEditAsNew(folder, previous);
                    }
                    case MessageCreationModes.FORWARD_AS_EML: {
                        const previous = await fetchRelatedFn();
                        return this.initForwardEml(previous);
                    }
                    default:
                        return this.initNewMessage(folder);
                }
            } catch {
                return this.initNewMessage(folder);
            }
        },

        // case of a new message
        async initNewMessage(folder) {
            const message = createEmpty(folder);
            this.$_ComposerInitMixin_ADD_MESSAGES({ messages: [message] });
            const identity = this.getIdentityForNewMessage();
            await this.setFrom(identity, message);
            this.$_ComposerInitMixin_SET_DRAFT_EDITOR_CONTENT(BmRichEditor.constants.NEW_LINE);
            this.$_ComposerInitMixin_SET_DRAFT_COLLAPSED_CONTENT(null);
            this.$_ComposerInitMixin_SET_SAVED_INLINE_IMAGES([]);
            return message;
        },

        // case of a reply or forward message
        async initReplyOrForward(folder, creationMode, previousMessage) {
            const identity = this.getIdentityForReplyOrForward(previousMessage);

            const message = createReplyOrForward(previousMessage, folder, creationMode, identity);

            if (creationMode !== MessageCreationModes.FORWARD && this.$store.state.mail.mailThreadSetting === "true") {
                message.conversationRef = { ...previousMessage.conversationRef };
            }

            this.$_ComposerInitMixin_ADD_MESSAGES({ messages: [message] });

            await this.setFrom(identity, message);

            const parts = getPartsFromCapabilities(previousMessage, COMPOSER_CAPABILITIES);

            await this.$_ComposerInitMixin_FETCH_PART_DATA({
                messageKey: previousMessage.key,
                folderUid: previousMessage.folderRef.uid,
                imapUid: previousMessage.remoteRef.imapUid,
                parts: parts.filter(
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
            const collapsed = quotePreviousMessage(
                contentFromPreviousMessage,
                previousMessage,
                creationMode,
                this.userPrefTextOnly,
                inject("i18n")
            );

            this.$_ComposerInitMixin_SET_DRAFT_EDITOR_CONTENT(BmRichEditor.constants.NEW_LINE);
            this.$_ComposerInitMixin_SET_DRAFT_COLLAPSED_CONTENT(collapsed);
            this.$_ComposerInitMixin_SET_SAVED_INLINE_IMAGES([]);

            if (creationMode === MessageCreationModes.FORWARD) {
                this.copyAttachments(previousMessage, message);
            }

            this.$store.commit("mail/" + SET_MESSAGES_LOADING_STATUS, [
                { key: message.key, loading: LoadingStatus.LOADED }
            ]);

            return message;
        },

        async initEditAsNew(folder, related) {
            const message = createEmpty(folder);
            this.$_ComposerInitMixin_ADD_MESSAGES({ messages: [message] });
            const identity = this.getIdentityForNewMessage();
            await this.setFrom(identity, message);
            this.mergeRecipients(message, related);
            this.mergeSubject(message, related);
            await this.mergeBody(message, related);
            await this.mergeAttachments(message, related);
            this.$router.navigate({ name: "v:mail:message", params: { message: message } });
            return message;
        },

        async initForwardEml(related) {
            const message = createEmpty(this.$_ComposerInitMixin_MY_DRAFTS);
            this.$_ComposerInitMixin_ADD_MESSAGES({ messages: [message] });
            const identity = this.getIdentityForNewMessage();
            await this.setFrom(identity, message);
            const subject = computeSubject(MessageCreationModes.FORWARD, related);
            this.$store.commit(`mail/${SET_MESSAGE_SUBJECT}`, { messageKey: message.key, subject });
            try {
                const content = await apiMessages.fetchComplete(related);
                const file = new File(
                    [content],
                    messageUtils.createEmlName(related, this.$t("mail.viewer.no.subject")),
                    { type: "message/rfc822" }
                );
                await this.$execute("add-attachments", { files: [file], message, maxSize: this.maxSize });
            } catch {
                this.ERROR({
                    alert: { name: "mail.forward_eml.fetch", uid: "FWD_EML_UID" }
                });
                const conversation = this.$store.state.mail.conversations.conversationByKey[
                    related.conversationRef.key
                ];
                this.$router.navigate({ name: "v:mail:conversation", params: { conversation } });
                return;
            }
            return message;
        },

        async mergeBody(message, previousMessage) {
            const parts = getPartsFromCapabilities(previousMessage, COMPOSER_CAPABILITIES);

            await this.$_ComposerInitMixin_FETCH_PART_DATA({
                messageKey: previousMessage.key,
                folderUid: previousMessage.folderRef.uid,
                imapUid: previousMessage.remoteRef.imapUid,
                parts: parts.filter(
                    part => MimeType.isHtml(part) || MimeType.isText(part) || (MimeType.isImage(part) && part.contentId)
                )
            });
            let content = getEditorContent(
                this.userPrefTextOnly,
                parts,
                this.$_ComposerInitMixin_partsByMessageKey[previousMessage.key],
                this.$_ComposerInitMixin_lang
            );

            if (!this.userPrefTextOnly) {
                const partsWithCid = parts.filter(part => MimeType.isImage(part) && part.contentId);

                const result = await InlineImageHelper.insertAsBase64(
                    [content],
                    partsWithCid,
                    this.$_ComposerInitMixin_partsByMessageKey[previousMessage.key]
                );
                content = sanitizeHtml(result.contentsWithImageInserted[0]);
            }
            this.$_ComposerInitMixin_SET_DRAFT_EDITOR_CONTENT(content);
            this.$_ComposerInitMixin_SET_SAVED_INLINE_IMAGES([]);
        },

        async mergeAttachments(message, related) {
            const messageWithTmpAddresses = await apiMessages.getForUpdate(related);
            const { files, attachments } = AttachmentAdaptor.extractFiles(messageWithTmpAddresses.attachments, message);

            attachments.forEach(attachment =>
                this.$store.commit(`mail/${ADD_ATTACHMENT}`, { messageKey: message.key, attachment })
            );
            this.$store.commit(`mail/${ADD_FILES}`, { files });
        },

        async copyAttachments(sourceMessage, destinationMessage) {
            const messageWithTmpAddresses = await apiMessages.getForUpdate(sourceMessage);
            const { files, attachments } = AttachmentAdaptor.extractFiles(
                messageWithTmpAddresses.attachments,
                sourceMessage
            );
            this.$store.commit(`mail/${SET_ATTACHMENTS}`, { messageKey: destinationMessage.key, attachments });
            this.$store.commit(`mail/${ADD_FILES}`, { files });
        },

        async mergeSubject(message, related) {
            this.$store.commit(`mail/${SET_MESSAGE_SUBJECT}`, { messageKey: message.key, subject: related.subject });
        },

        async mergeRecipients(message, { to, cc, bcc }) {
            const rcpts = message.to;
            let recipients = message.to.concat(to.filter(to => rcpts.every(rcpt => to.address !== rcpt.address)));
            this.$store.commit(`mail/${SET_MESSAGE_TO}`, { messageKey: message.key, to: recipients });
            rcpts.push(...message.cc);
            recipients = message.cc.concat(cc.filter(cc => rcpts.every(rcpt => cc.address !== rcpt.address)));
            this.$store.commit(`mail/${SET_MESSAGE_CC}`, { messageKey: message.key, cc: recipients });
            rcpts.push(...message.bcc);
            recipients = message.bcc.concat(bcc.filter(bcc => rcpts.every(rcpt => bcc.address !== rcpt.address)));
            this.$store.commit(`mail/${SET_MESSAGE_BCC}`, { messageKey: message.key, bcc: recipients });
        }
    }
};

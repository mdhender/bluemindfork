import { mapActions, mapGetters, mapMutations, mapState } from "vuex";

import { InlineImageHelper, MimeType } from "@bluemind/email";
import { inject } from "@bluemind/inject";
import { sanitizeHtml } from "@bluemind/html-utils";
import { BmRichEditor } from "@bluemind/styleguide";
import { draft, message, loadingStatus, part } from "@bluemind/mail";

import { FETCH_PART_DATA, FETCH_MESSAGE_IF_NOT_LOADED } from "~/actions";
import { MY_DRAFTS } from "~/getters";
import {
    ADD_ATTACHMENT,
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

const { LoadingStatus } = loadingStatus;
const {
    addSeparator,
    COMPOSER_CAPABILITIES,
    createEmpty,
    createReplyOrForward,
    getEditorContent,
    handleSeparator
} = draft;
const { getPartsFromCapabilities } = part;

const { MessageCreationModes } = message;

/**
 * Manage different cases of composer initialization
 */
export default {
    data() {
        return {
            userPrefTextOnly: false // FIXME: https://forge.bluemind.net/jira/browse/FEATWEBML-88
        };
    },
    mixins: [ComposerFromMixin],
    computed: {
        ...mapGetters("mail", { $_ComposerInitMixin_MY_DRAFTS: MY_DRAFTS }),
        ...mapState("mail", { $_ComposerInitMixin_partsByMessageKey: ({ partsData }) => partsData.partsByMessageKey }),
        $_ComposerInitMixin_lang() {
            return this.$store.state.settings.lang;
        }
    },
    methods: {
        ...mapActions("mail", { $_ComposerInitMixin_FETCH_PART_DATA: FETCH_PART_DATA }),
        ...mapMutations("mail", {
            $_ComposerInitMixin_ADD_MESSAGES: ADD_MESSAGES,
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
                case MessageCreationModes.EDIT_AS_NEW:
                    try {
                        const previous = await this.$store.dispatch("mail/" + FETCH_MESSAGE_IF_NOT_LOADED, {
                            internalId: related.internalId,
                            folder: this.$store.state.mail.folders[related.folderKey]
                        });
                        return this.initEditAsNew(previous);
                    } catch {
                        return this.initNewMessage();
                    }
                default:
                    return this.initNewMessage();
            }
        },

        // case of a new message
        async initNewMessage() {
            const message = createEmpty(this.$_ComposerInitMixin_MY_DRAFTS);
            this.$_ComposerInitMixin_ADD_MESSAGES({ messages: [message] });
            const identity = this.getIdentityForNewMessage();
            await this.setFrom(identity, message);
            this.$_ComposerInitMixin_SET_DRAFT_EDITOR_CONTENT(BmRichEditor.constants.NEW_LINE);
            this.$_ComposerInitMixin_SET_DRAFT_COLLAPSED_CONTENT(null);
            this.$_ComposerInitMixin_SET_SAVED_INLINE_IMAGES([]);
            return message;
        },

        // case of a reply or forward message
        async initReplyOrForward(creationMode, previousMessage) {
            const identity = this.getIdentityForReplyOrForward(previousMessage);

            const message = createReplyOrForward(
                previousMessage,
                this.$_ComposerInitMixin_MY_DRAFTS,
                creationMode,
                identity
            );

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
            const collapsed = addSeparator(
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

        async initEditAsNew(related) {
            const message = createEmpty(this.$_ComposerInitMixin_MY_DRAFTS);
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

        async mergeAttachments({ key }, related) {
            const attachments = (await apiMessages.getForUpdate(related)).attachments;
            attachments.forEach(attachment =>
                this.$store.commit(`mail/${ADD_ATTACHMENT}`, { messageKey: key, attachment })
            );
        },

        async copyAttachments(sourceMessage, destinationMessage) {
            const attachments = (await apiMessages.getForUpdate(sourceMessage)).attachments;
            this.$store.commit(`mail/${SET_ATTACHMENTS}`, { messageKey: destinationMessage.key, attachments });
        },

        async mergeSubject({ key }, related) {
            this.$store.commit(`mail/${SET_MESSAGE_SUBJECT}`, { messageKey: key, subject: related.subject });
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

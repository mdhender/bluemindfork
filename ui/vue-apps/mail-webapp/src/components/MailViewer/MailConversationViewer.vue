<template>
    <div class="mail-conversation-viewer bg-surface pt-5" :class="{ darkened }">
        <mail-conversation-viewer-header
            :conversation="conversationMessages"
            :expanded="expanded"
            @do-show-middle-messages="showMiddleMessages = true"
            @expand="expandAll()"
            @collapse="collapseAll()"
        />
        <div class="container">
            <div v-for="(message, index) in conversationMessages" :key="message.key">
                <mail-conversation-viewer-hidden-items
                    v-if="!containsComposingMessageInTheMiddle && isHidden(index)"
                    :index="index"
                    :conversation="conversationMessages"
                    :count="hiddenItemsCount"
                    @do-show-middle-messages="showMiddleMessages = true"
                />
                <component
                    :is="
                        isDraft(index)
                            ? message.composing
                                ? 'mail-conversation-viewer-draft-editor'
                                : 'mail-conversation-viewer-draft'
                            : 'mail-conversation-viewer-message'
                    "
                    v-else
                    :class="{ expanded: expandedMessages[index] }"
                    :index="index"
                    :conversation="conversation"
                    :message="message"
                    :message-key="message.key"
                    :expanded-messages="expandedMessages"
                    :show-middle-messages="showMiddleMessages"
                    :is-last-before-draft="isLastBeforeDraft(index)"
                    :next-is-draft="nextIsDraft(index)"
                    :is-reply-or-forward="true"
                    :conversation-size="conversationMessages.length"
                    @expand="expand(index)"
                    @darken="darken"
                />
            </div>
        </div>
        <mail-conversation-viewer-footer v-if="noDraftOpened" :last-non-draft="lastNonDraft" />
    </div>
</template>
<script>
import Vue from "vue";
import MailConversationViewerDraft from "./MailConversationViewer/MailConversationViewerDraft";
import MailConversationViewerDraftEditor from "./MailConversationViewer/MailConversationViewerDraftEditor";
import MailConversationViewerFooter from "./MailConversationViewer/MailConversationViewerFooter";
import MailConversationViewerHeader from "./MailConversationViewer/MailConversationViewerHeader";
import MailConversationViewerHiddenItems from "./MailConversationViewer/MailConversationViewerHiddenItems";
import MailConversationViewerMessage from "./MailConversationViewer/MailConversationViewerMessage";
import { mapActions, mapGetters, mapMutations, mapState } from "vuex";
import { CONVERSATION_LIST_UNREAD_FILTER_ENABLED, CONVERSATION_MESSAGE_BY_KEY, MY_DRAFTS, MY_SENT } from "~/getters";
import { SET_MESSAGE_COMPOSING } from "~/mutations";
import { MARK_CONVERSATIONS_AS_READ } from "~/actions";
import { removeSentDuplicates, sortConversationMessages } from "~/model/conversations";

export default {
    name: "MailConversationViewer",
    components: {
        MailConversationViewerDraft,
        MailConversationViewerDraftEditor,
        MailConversationViewerFooter,
        MailConversationViewerHeader,
        MailConversationViewerHiddenItems,
        MailConversationViewerMessage
    },
    props: {
        conversation: {
            type: Object,
            required: true
        }
    },
    data() {
        return {
            darkened: false,
            showMiddleMessages: false,
            expandedMessages: []
        };
    },
    computed: {
        ...mapGetters("mail", {
            CONVERSATION_LIST_UNREAD_FILTER_ENABLED,
            CONVERSATION_MESSAGE_BY_KEY,
            MY_DRAFTS,
            MY_SENT
        }),
        ...mapState("mail", ["folders"]),
        ...mapState("mail", { messages: ({ conversations }) => conversations.messages }),
        conversationMessages() {
            return sortConversationMessages(
                removeSentDuplicates(this.CONVERSATION_MESSAGE_BY_KEY(this.conversation.key), this.MY_SENT),
                this.folders
            );
        },
        noDraftOpened() {
            return this.conversationMessages.every(message => !message.composing);
        },
        draftStates() {
            return this.conversationMessages.map(
                message => message.composing || this.MY_DRAFTS.key === message.folderRef.key
            );
        },
        hiddenItemsCount() {
            return this.conversationMessages.reduce(
                (total, message, index) => (this.isHidden(index) ? total + 1 : total),
                0
            );
        },
        lastNonDraft() {
            let lastNonDraft;
            for (let i = this.conversationMessages.length - 1; i >= 0 && !lastNonDraft; i--) {
                lastNonDraft = !this.isDraft(i) && this.conversationMessages[i];
            }
            return lastNonDraft;
        },
        expanded() {
            return this.conversationMessages.every((m, index) => this.expandedMessages[index]);
        },
        containsComposingMessageInTheMiddle() {
            return this.conversationMessages.some(
                (m, index) => index > 0 && index < this.conversationMessages.length - 1 && m.composing
            );
        }
    },
    watch: {
        "conversation.key": {
            handler: function (value, oldValue) {
                this.init();
                if (oldValue && oldValue !== value) {
                    this.resetComposingStatuses();
                }
            },
            immediate: true
        },
        conversationMessages(value) {
            // expand last message
            this.expandedMessages[value.length - 1] = true;
        }
    },
    created() {
        this.expandLastMessageAndTrailingDrafts();
    },
    destroyed() {
        this.resetComposingStatuses();
    },
    methods: {
        ...mapMutations("mail", { SET_MESSAGE_COMPOSING }),
        ...mapActions("mail", { MARK_CONVERSATIONS_AS_READ }),
        init() {
            this.darkened = false;
            this.showMiddleMessages = false;
            this.expandedMessages = [];
            this.collapseAll();
            this.markAsRead();
        },
        markAsRead() {
            if (
                !this.CONVERSATION_LIST_UNREAD_FILTER_ENABLED &&
                this.folders[this.conversation.folderRef.key].writable
            ) {
                this.MARK_CONVERSATIONS_AS_READ({ conversations: [this.conversation], noAlert: true });
            }
        },
        resetComposingStatuses() {
            this.conversationMessages.forEach(m => this.SET_MESSAGE_COMPOSING({ messageKey: m.key, composing: false }));
        },
        expand(index) {
            Vue.set(this.expandedMessages, index, true);
        },
        expandAll() {
            this.expandedMessages = this.conversationMessages.map(() => true);
        },
        collapseAll() {
            this.showMiddleMessages = false;
            this.expandedMessages.splice(0);
            this.expandLastMessageAndTrailingDrafts();
        },
        isHidden(index) {
            return (
                !this.showMiddleMessages &&
                this.isMiddle(index) &&
                !this.isLastBeforeDraft(index) &&
                !this.isTrailingDraft(index)
            );
        },
        isMiddle(index) {
            return this.conversationMessages.length > 3 && index > 0 && index < this.conversationMessages.length - 1;
        },
        darken(darkened) {
            this.darkened = darkened;
        },
        /** Expand the last non-draft message and following drafts. */
        expandLastMessageAndTrailingDrafts() {
            let lastNonDraftFound = false;
            for (let i = this.conversationMessages.length - 1; i >= 0 && !lastNonDraftFound; i--) {
                Vue.set(this.expandedMessages, i, true);
                lastNonDraftFound = !this.draftStates[i];
            }
        },
        isDraft(index) {
            return this.draftStates[index];
        },
        isLastBeforeDraft(index) {
            return !this.draftStates[index] && this.isTrailingDraft(index + 1);
        },
        isTrailingDraft(index) {
            return this.draftStates.slice(index).every(Boolean);
        },
        nextIsDraft(index) {
            return !!this.draftStates[index + 1];
        }
    }
};
</script>
<style lang="scss">
@import "@bluemind/styleguide/css/_variables.scss";

.mail-conversation-viewer {
    .vertical-line {
        width: 2em !important;
        background-image: linear-gradient($secondary, $secondary);
        background-size: 1px 100%;
        background-repeat: no-repeat;
        background-position: center center;
        &.first {
            background-size: 1px 50%;
            background-position: bottom center;
        }
        &.last {
            background-size: 1px 50%;
            background-position: top center;
        }
    }
    .draft .vertical-line,
    .last-before-draft .vertical-line.vertical-line-after-avatar {
        background-image: repeating-linear-gradient($secondary, $secondary 2px, $white 2px, $white 6px);
    }
    .vertical-line-transparent {
        background-image: linear-gradient($surface-bg, $surface-bg) !important;
    }
    .col-1 {
        flex: unset;
        width: unset;
    }
    .spacer {
        height: 0.5rem;
    }
    .mail-conversation-viewer-item:not(.expanded) .mail-conversation-viewer-item-body:hover {
        cursor: pointer;
        position: relative;
        &::before {
            content: "";
            position: absolute;
            top: 0;
            left: 0;
            bottom: 0;
            right: 0;
            background-color: $extra-light;
            margin: 0.375em 0.5625em 0.375em 0.5625em;
        }
    }
}
</style>

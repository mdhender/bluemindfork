<template>
    <div class="mail-conversation-viewer bg-surface pt-5" :class="{ darkened }">
        <mail-conversation-viewer-header
            :subject="conversationMessages[0].subject"
            :expanded="expanded"
            @do-show-hidden-messages="showHiddenMessages = conversationMessages.map(Boolean)"
            @expand="expandAll()"
            @collapse="collapseAll()"
        />
        <div class="container">
            <div v-for="(message, index) in conversationMessages" :key="message.key">
                <mail-conversation-viewer-hidden-items
                    v-if="isFirstOfHiddenGroup(index)"
                    :index="index"
                    :count="hiddenGroupSize(index)"
                    :conversation-size="conversationMessages.length"
                    @do-show-hidden-messages="doShowHiddenMessages(index)"
                />
                <mail-conversation-viewer-compo-switcher
                    v-else-if="!hiddenMessages[index]"
                    :class="{ expanded: expandedMessages[index] }"
                    :index="index"
                    :conversation="conversation"
                    :message="message"
                    :expanded-messages="expandedMessages"
                    :next-is-hidden="!!hiddenMessages[index + 1]"
                    :is-last-before-draft="isLastBeforeDraft(index)"
                    :next-is-draft="nextIsDraft(index)"
                    :is-reply-or-forward="true"
                    :conversation-size="conversationMessages.length"
                    :is-draft="isDraft(index)"
                    @expand="expand(index)"
                    @collapse="collapse(index)"
                    @darken="darken"
                    @remote-content="setBlockRemote"
                />
            </div>
        </div>
        <mail-conversation-viewer-footer
            v-if="noDraftOpened"
            :last-non-draft="lastNonDraft"
            :conversation-key="conversation.key"
        />
        <template-chooser />
    </div>
</template>
<script>
import Vue from "vue";
import { REMOVE, WARNING } from "@bluemind/alert.store";
import { mapActions, mapGetters, mapMutations, mapState } from "vuex";
import { conversationUtils } from "@bluemind/mail";

import MailConversationViewerCompoSwitcher from "./MailConversationViewerCompoSwitcher";
import MailConversationViewerFooter from "./MailConversationViewer/MailConversationViewerFooter";
import MailConversationViewerHeader from "./MailConversationViewer/MailConversationViewerHeader";
import MailConversationViewerHiddenItems from "./MailConversationViewer/MailConversationViewerHiddenItems";
import apiAddressbooks from "~/store/api/apiAddressbooks";
import {
    CONVERSATION_LIST_UNREAD_FILTER_ENABLED,
    CONVERSATION_MESSAGE_BY_KEY,
    CURRENT_MAILBOX,
    MY_DRAFTS
} from "~/getters";
import { SET_BLOCK_REMOTE_IMAGES, SET_MESSAGE_COMPOSING } from "~/mutations";
import { MARK_CONVERSATIONS_AS_READ } from "~/actions";
import { Flag } from "@bluemind/email";
import TemplateChooser from "~/components/TemplateChooser";

const { sortConversationMessages } = conversationUtils;

export default {
    name: "MailConversationViewer",
    components: {
        MailConversationViewerCompoSwitcher,
        MailConversationViewerFooter,
        MailConversationViewerHeader,
        MailConversationViewerHiddenItems,
        TemplateChooser
    },
    props: {
        conversation: {
            type: Object,
            required: true
        }
    },
    data() {
        return {
            alert: {
                alert: { name: "mail.BLOCK_REMOTE_CONTENT", uid: "BLOCK_REMOTE_CONTENT" },
                options: { area: "right-panel", renderer: "BlockedRemoteContent" }
            },
            darkened: false,
            showHiddenMessages: [],
            expandedMessages: []
        };
    },
    computed: {
        ...mapGetters("mail", { CONVERSATION_LIST_UNREAD_FILTER_ENABLED, CONVERSATION_MESSAGE_BY_KEY, MY_DRAFTS }),
        ...mapState("mail", ["folders"]),
        ...mapState("mail", { messages: ({ conversations }) => conversations.messages }),
        trustRemoteContent() {
            return this.$store.state.settings.trust_every_remote_content !== "false";
        },
        remoteBlocked() {
            return this.$store.state.mail.consultPanel.remoteImages.mustBeBlocked;
        },
        conversationMessages() {
            return sortConversationMessages(this.CONVERSATION_MESSAGE_BY_KEY(this.conversation.key), this.folders);
        },
        noDraftOpened() {
            return this.conversationMessages.every(message => !message.composing);
        },
        draftStates() {
            return this.conversationMessages.map(
                message => message.composing || this.MY_DRAFTS.key === message.folderRef.key
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
        hiddenMessages() {
            const hiddenCandidates = this.hiddenCandidates();
            return hiddenCandidates.map(
                (hc, index) => hc && (hiddenCandidates[index + 1] || hiddenCandidates[index - 1])
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
        "conversationMessages.length"() {
            this.expandUnreadOrLastAndTrailingDrafts();
        }
    },
    destroyed() {
        this.resetComposingStatuses();
        this.REMOVE(this.alert.alert);
    },
    methods: {
        ...mapMutations("mail", { SET_BLOCK_REMOTE_IMAGES, SET_MESSAGE_COMPOSING }),
        ...mapActions("mail", { MARK_CONVERSATIONS_AS_READ }),
        ...mapActions("alert", { REMOVE, WARNING }),
        init() {
            this.darkened = false;
            this.REMOVE(this.alert.alert);
            this.SET_BLOCK_REMOTE_IMAGES(!this.trustRemoteContent);
            this.collapseAll();
            this.markAsRead();
        },
        markAsRead() {
            if (
                !this.CONVERSATION_LIST_UNREAD_FILTER_ENABLED &&
                this.folders[this.conversation.folderRef.key].writable
            ) {
                this.MARK_CONVERSATIONS_AS_READ({
                    conversations: [this.conversation],
                    noAlert: true,
                    conversationsActivated: true,
                    mailbox: this.$store.getters[`mail/${CURRENT_MAILBOX}`]
                });
            }
        },
        resetComposingStatuses() {
            this.conversationMessages.forEach(m => this.SET_MESSAGE_COMPOSING({ messageKey: m.key, composing: false }));
        },
        expand(index) {
            Vue.set(this.expandedMessages, index, true);
        },
        expandAll() {
            this.expandedMessages = Array(this.conversationMessages.length).fill(true);
        },
        collapse(index) {
            Vue.set(this.expandedMessages, index, false);
        },
        /** Collapse all messages we can. Not the last one, trailing drafts and unread. */
        collapseAll() {
            this.showHiddenMessages = [];
            this.expandedMessages.splice(0);
            this.expandUnreadOrLastAndTrailingDrafts();
        },
        isHiddenCandidate(index) {
            return (
                !this.showHiddenMessages[index] &&
                index !== 0 &&
                !this.isTrailingDraft(index) && // TODO remove useless
                !this.expandedMessages[index] &&
                !this.isDraft(index)
            );
        },
        isUnread(index) {
            const message = this.conversationMessages[index];
            return message.flags && !message.flags.includes(Flag.SEEN);
        },
        isInConversationFolder(index) {
            return this.conversationMessages[index].folderRef.key === this.conversation.folderRef.key;
        },
        darken(darkened) {
            this.darkened = darkened;
        },
        /** Expand unread messages or the last non-draft message and trailing drafts. */
        expandUnreadOrLastAndTrailingDrafts() {
            let lastNonDraftFound = false;
            let atLeastOneUnread = false;
            const lastIndex = this.conversationMessages.length - 1;
            for (let i = lastIndex; i >= 0; i--) {
                const isUnread = this.isUnread(i) && this.isInConversationFolder(i);
                atLeastOneUnread = isUnread || atLeastOneUnread;
                if (!lastNonDraftFound || isUnread) {
                    Vue.set(this.expandedMessages, i, true);
                }
                lastNonDraftFound = lastNonDraftFound ? lastNonDraftFound : !this.draftStates[i];
            }

            const lastIsUnread = this.isUnread(lastIndex);
            if (atLeastOneUnread && !lastIsUnread) {
                // collapse the last one if we have an unread elsewhere
                Vue.set(this.expandedMessages, lastIndex, false);
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
        },
        isFirstOfHiddenGroup(index) {
            return this.hiddenMessages[index] && (index === 0 || !this.hiddenMessages[index - 1]);
        },
        hiddenGroupSize(index) {
            let currentIndex = index;
            while (this.hiddenMessages[currentIndex] && currentIndex < this.conversationMessages.length) {
                currentIndex++;
            }
            return currentIndex - index;
        },
        doShowHiddenMessages(index) {
            let currentIndex = index;
            do {
                Vue.set(this.showHiddenMessages, currentIndex++, true);
            } while (this.hiddenMessages[currentIndex]);
        },
        hiddenCandidates() {
            return this.conversationMessages.map((m, index) => this.isHiddenCandidate(index));
        },

        async setBlockRemote(message) {
            if (this.remoteBlocked) {
                const { total } = await apiAddressbooks.search(message.from.address);
                if (total === 0) {
                    const alert = {
                        alert: { ...this.alert.alert, payload: message },
                        options: this.alert.options
                    };
                    this.WARNING(alert);
                } else {
                    this.SET_BLOCK_REMOTE_IMAGES(false);
                }
            }
        }
    }
};
</script>
<style lang="scss">
@import "@bluemind/styleguide/css/_variables.scss";

.mail-conversation-viewer {
    .vertical-line {
        width: 2em !important;
        background-image: linear-gradient($neutral-fg, $neutral-fg);
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
        background-image: repeating-linear-gradient($neutral-fg, $neutral-fg 2px, $neutral-bg 2px, $neutral-bg 6px);
    }
    .vertical-line-transparent {
        background-image: linear-gradient($neutral-bg, $neutral-bg) !important;
    }
    .col-1 {
        flex: unset;
        width: unset;
    }
    .spacer {
        height: 0.5rem;
    }
    .mail-conversation-viewer-item:not(.expanded):not(.draft) .mail-conversation-viewer-item-body:hover {
        cursor: pointer;
        position: relative;
        &::before {
            content: "";
            position: absolute;
            top: 0;
            left: 0;
            bottom: 0;
            right: 0;
            background-color: $neutral-bg-lo1;
            margin: 0.375em 0.5625em 0.375em 0.5625em;
        }
    }
}
</style>

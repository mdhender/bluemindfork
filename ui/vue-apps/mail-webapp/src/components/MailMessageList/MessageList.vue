<template>
    <bm-list-group
        class="message-list bg-extra-light"
        tabindex="0"
        @scroll="onScroll"
        @keyup.shift.delete.exact.prevent="purge"
        @keyup.delete.exact.prevent="remove"
        @keyup.up.exact="goToByDiff(-1)"
        @keyup.down.exact="goToByDiff(+1)"
        @keyup.page-down.exact="goToByDiff(+PAGE)"
        @keyup.page-up.exact="goToByDiff(-PAGE)"
        @keyup.home.exact="goToByIndex(0)"
        @keyup.end.exact="goToByIndex(count - 1)"
        @keyup.space.exact="goToByKey(lastFocusedMessage)"
        @keyup.ctrl.exact.space="toggleSelect(lastFocusedMessage, true)"
        @keyup.ctrl.exact.65="toggleAll()"
        @keyup.ctrl.exact.up="focusByDiff(-1)"
        @keyup.ctrl.exact.down="focusByDiff(+1)"
        @keyup.ctrl.exact.home="focusByIndex(0)"
        @keyup.ctrl.exact.end="focusByIndex(count - 1)"
        @keyup.shift.exact.space="selectRange(lastFocusedMessage, true)"
        @keyup.shift.exact.up="selectRangeByDiff(-1, true)"
        @keyup.shift.exact.down="selectRangeByDiff(+1, true)"
        @keyup.shift.exact.home="selectRange(itemKeys[0], true)"
        @keyup.shift.exact.end="selectRange(itemKeys[count - 1], true)"
        @keyup.shift.ctrl.exact.space="selectRange(lastFocusedMessage)"
        @keyup.shift.ctrl.exact.up="selectRangeByDiff(-1)"
        @keyup.shift.ctrl.exact.down="selectRangeByDiff(+1)"
        @keyup.shift.ctrl.exact.home="selectRange(itemKeys[0])"
        @keyup.shift.ctrl.exact.end="selectRange(itemKeys[count - 1])"
    >
        <div v-for="(message, index) in _messages" :key="index">
            <message-list-separator v-if="message.hasSeparator" :text="$t(message.range.name)" />
            <message-list-item
                :ref="'message-' + message.key"
                :message="message"
                :is-muted="!!draggedMessage && isMessageSelected(draggedMessage) && isMessageSelected(message.key)"
                @toggleSelect="toggleSelect"
                @click.exact.native="unselectAllIfNeeded(message.key)"
                @click.ctrl.exact.native.capture.prevent.stop="toggleSelect(message.key)"
                @click.shift.exact.native.prevent.stop="selectRange(message.key, true)"
                @click.shift.exact.ctrl.exact.native.capture.prevent.stop="selectRange(message.key)"
                @dragstart="draggedMessage = message.key"
                @dragend="draggedMessage = null"
            />
        </div>
        <bm-list-group-item v-if="hasMore">Loading…</bm-list-group-item>
    </bm-list-group>
</template>

<script>
import { BmListGroup, BmListGroupItem } from "@bluemind/styleguide";
import { mapState, mapGetters, mapActions, mapMutations } from "vuex";
import { TOGGLE_SELECTION_ALL } from "../VueBusEventTypes";
import MessageListItem from "./MessageListItem";
import MessageListSeparator from "./MessageListSeparator";

const PAGE = 9;

export default {
    name: "MessageList",
    components: {
        BmListGroup,
        BmListGroupItem,
        MessageListItem,
        MessageListSeparator
    },
    data() {
        return {
            PAGE,
            length: 20,
            lastFocusedMessage: null,
            anchoredMessageForShift: null,
            draggedMessage: null
        };
    },
    computed: {
        ...mapGetters("mail-webapp", [
            "nextMessageKey",
            "areMessagesFiltered",
            "isMessageSelected",
            "areAllMessagesSelected"
        ]),
        ...mapState("mail-webapp/messages", ["itemKeys"]),
        ...mapGetters("mail-webapp/messages", ["messages", "count", "indexOf"]),
        ...mapGetters("mail-webapp/currentMessage", { currentMessage: "message" }),
        ...mapState("mail-webapp", ["messageFilter", "selectedMessageKeys"]),
        ...mapState("mail-webapp/currentMessage", { currentMessageKey: "key" }),
        ...mapGetters("mail", ["MY_TRASH"]),
        ...mapState("mail", ["activeFolder"]),
        _messages() {
            return this.messages.slice(0, this.length);
        },
        hasMore: function () {
            return this.length < this.count;
        },
        isSelectionMultiple() {
            return this.selectedMessageKeys.length > 1;
        }
    },
    watch: {
        currentMessageKey() {
            if (this.currentMessageKey) {
                this.focusByKey(this.currentMessageKey);
            }
        },
        activeFolder() {
            this.lastFocusedMessage = null;
            this.anchoredMessageForShift = null;
        }
    },
    created() {
        this.focusByKey(this.currentMessageKey);
    },
    bus: {
        [TOGGLE_SELECTION_ALL]: function () {
            this.toggleAll();
        }
    },
    methods: {
        ...mapActions("mail-webapp", ["loadRange"]),
        ...mapMutations("mail-webapp", [
            "addSelectedMessageKey",
            "deleteSelectedMessageKey",
            "addAllToSelectedMessages",
            "deleteAllSelectedMessages"
        ]),
        ...mapMutations("mail-webapp/currentMessage", { clearCurrentMessage: "clear" }),
        async loadMore() {
            if (this.hasMore) {
                const end = Math.min(this.length + 20, this.count);
                await this.loadRange({ start: this.length, end });
                this.length = end;
            }
        },
        onScroll(event) {
            const total = event.target.scrollHeight;
            const current = event.target.scrollTop + event.target.offsetHeight;
            if (current >= total) {
                this.loadMore();
            }
        },
        remove() {
            if (this.activeFolder === this.MY_TRASH.key) {
                this.purge();
            } else {
                // do this before followed async operations
                const nextMessageKey = this.nextMessageKey;
                this.$store.dispatch(
                    "mail-webapp/remove",
                    this.selectedMessageKeys.length ? this.selectedMessageKeys : this.currentMessageKey
                );
                if (!this.isSelectionMultiple) {
                    this.$router.navigate({ name: "v:mail:message", params: { message: nextMessageKey } });
                }
            }
        },
        async purge() {
            const confirm = await this.$bvModal.msgBoxConfirm(
                this.$tc("mail.actions.purge.modal.content", this.selectedMessageKeys.length || 1, {
                    subject: this.currentMessage && this.currentMessage.subject
                }),
                {
                    title: this.$tc("mail.actions.purge.modal.title", this.selectedMessageKeys.length || 1),
                    okTitle: this.$t("common.delete"),
                    cancelVariant: "outline-secondary",
                    cancelTitle: this.$t("common.cancel"),
                    centered: true,
                    hideHeaderClose: false
                }
            );
            if (confirm) {
                // do this before followed async operations
                const nextMessageKey = this.nextMessageKey;
                this.$store.dispatch(
                    "mail-webapp/purge",
                    this.selectedMessageKeys.length ? this.selectedMessageKeys : this.currentMessageKey
                );
                if (!this.isSelectionMultiple) {
                    this.$router.navigate({ name: "v:mail:message", params: { message: nextMessageKey } });
                }
            }
        },
        goToByDiff(diff) {
            this.goToByIndex(this.indexOf(this.lastFocusedMessage) + diff);
        },
        goToByIndex(index) {
            index = Math.min(Math.max(0, index), this.count - 1);
            this.goToByKey(this.messages[index].key);
        },
        goToByKey(key) {
            this.$router.navigate({ name: "v:mail:message", params: { message: key } });
            this.deleteAllSelectedMessages();
        },
        focusByKey(key) {
            this.focusByIndex(this.indexOf(key));
        },
        focusByDiff(diff) {
            this.focusByIndex(this.indexOf(this.lastFocusedMessage) + diff);
        },
        focusByIndex(index) {
            if (index !== -1 && this.messages[index]) {
                const messageKeyToFocus = this.messages[index].key;
                this.$nextTick(() => {
                    const htmlElement = this.$refs["message-" + messageKeyToFocus];
                    if (htmlElement[0] && htmlElement[0].$el) {
                        htmlElement[0].$el.focus();
                    } else {
                        console.log("not in DOM..");
                    }
                });
                this.lastFocusedMessage = messageKeyToFocus;
            }
        },
        selectRangeByDiff(diff, shouldReset = false) {
            const index = this.indexOf(this.lastFocusedMessage) + diff;
            const message = this.messages[index];
            if (message) {
                this.selectRange(message.key, shouldReset);
            }
        },
        selectRange(destinationMessageKey, shouldReset = false) {
            this.checkReset(shouldReset);
            this.initAnchored();
            if (this.anchoredMessageForShift && destinationMessageKey) {
                const startIndex = this.indexOf(this.anchoredMessageForShift);
                const endIndex = this.indexOf(destinationMessageKey);
                this.addMessageKeysBetween(startIndex, endIndex);
                this.focusByKey(destinationMessageKey);
                this.navigateAfterSelection();
            }
        },
        checkReset(shouldReset) {
            if (shouldReset) {
                this.deleteAllSelectedMessages();
            }
        },
        addMessageKeysBetween(start, end) {
            const realStart = start < end ? start : end;
            const realEnd = start < end ? end : start;
            return this.messages
                .slice(realStart, realEnd + 1)
                .filter(Boolean)
                .map(message => message.key)
                .forEach(key => this.addSelectedMessageKey(key));
        },
        initAnchored() {
            if (!this.anchoredMessageForShift) {
                this.anchoredMessageForShift =
                    this.lastFocusedMessage || this.currentMessageKey || (this.messages[0] && this.messages[0].key);
            }
        },
        toggleAll() {
            if (this.areAllMessagesSelected) {
                this.deleteAllSelectedMessages();
                this.clearCurrentMessage();
            } else {
                this.addAllToSelectedMessages(this.itemKeys);
            }
            this.navigateAfterSelection();
        },
        toggleSelect(messageKey, force = false) {
            if (this.isMessageSelected(messageKey)) {
                this.deleteSelectedMessageKey(messageKey);
                if (this.currentMessageKey === messageKey) {
                    this.clearCurrentMessage();
                }
            } else if (this.currentMessageKey === messageKey && !force) {
                this.clearCurrentMessage();
            } else {
                this.addSelectedMessageKey(messageKey);
                this.anchoredMessageForShift = messageKey;
            }
            this.focusByKey(messageKey);
            this.navigateAfterSelection();
        },
        navigateAfterSelection() {
            this.$router.navigate("mail:home");
        },
        unselectAllIfNeeded(messageKey) {
            if (this.selectedMessageKeys.length !== 1 || this.selectedMessageKeys[0] !== messageKey) {
                this.deleteAllSelectedMessages();
            }
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/variables";
.message-list {
    overflow-x: hidden;
    overflow-y: auto;
}
</style>

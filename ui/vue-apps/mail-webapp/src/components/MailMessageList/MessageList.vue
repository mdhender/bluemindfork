<template>
    <bm-list-group
        class="message-list bg-extra-light"
        tabindex="0"
        @scroll="onScroll"
        @keyup.shift.delete.exact.prevent="purge($event)"
        @keyup.delete.exact.prevent="remove($event)"
        @keyup.up.exact="goToByDiff(-1)"
        @keydown.up.prevent
        @keyup.down.exact="goToByDiff(+1)"
        @keydown.down.prevent
        @keyup.page-down.exact="goToByDiff(+PAGE)"
        @keyup.page-up.exact="goToByDiff(-PAGE)"
        @keyup.home.exact="goToByIndex(0)"
        @keyup.end.exact="goToByIndex(MESSAGE_LIST_COUNT - 1)"
        @keyup.space.exact="goToByKey(lastFocusedMessage)"
        @keyup.ctrl.exact.space="toggleSelect(lastFocusedMessage, true)"
        @keyup.ctrl.exact.65="toggleAll()"
        @keyup.ctrl.exact.up="focusByDiff(-1)"
        @keyup.ctrl.exact.down="focusByDiff(+1)"
        @keyup.ctrl.exact.home="focusByIndex(0)"
        @keyup.ctrl.exact.end="focusByIndex(MESSAGE_LIST_COUNT - 1)"
        @keyup.shift.exact.space="selectRange(lastFocusedMessage, true)"
        @keyup.shift.exact.up="selectRangeByDiff(-1, true)"
        @keyup.shift.exact.down="selectRangeByDiff(+1, true)"
        @keyup.shift.exact.home="selectRange(messageKeys[0], true)"
        @keyup.shift.exact.end="selectRange(messageKeys[MESSAGE_LIST_COUNT - 1], true)"
        @keyup.shift.ctrl.exact.space="selectRange(lastFocusedMessage)"
        @keyup.shift.ctrl.exact.up="selectRangeByDiff(-1)"
        @keyup.shift.ctrl.exact.down="selectRangeByDiff(+1)"
        @keyup.shift.ctrl.exact.home="selectRange(messageKeys[0])"
        @keyup.shift.ctrl.exact.end="selectRange(messageKeys[MESSAGE_LIST_COUNT - 1])"
    >
        <div v-for="(message, index) in messages" :key="index">
            <date-separator :message="message" :force="index === 0" />
            <draggable-message
                :ref="'message-' + message.key"
                :message="message"
                :is-muted="!!draggedMessage && isMessageSelected(draggedMessage) && isMessageSelected(message.key)"
                @toggle-select="toggleSelect"
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
import DraggableMessage from "./DraggableMessage";
import DateSeparator from "./DateSeparator";

const PAGE = 9;

export default {
    name: "MessageList",
    components: {
        BmListGroup,
        BmListGroupItem,
        DateSeparator,
        DraggableMessage
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
        ...mapGetters("mail-webapp", ["nextMessageKey", "isMessageSelected", "areAllMessagesSelected"]),
        ...mapState("mail-webapp", ["selectedMessageKeys"]),
        ...mapState("mail-webapp/currentMessage", { currentMessageKey: "key" }),
        ...mapGetters("mail", ["MY_TRASH", "MESSAGE_LIST_COUNT", "isLoaded"]),
        ...mapState("mail", {
            activeFolder: "activeFolder",
            messageKeys: state => state.messageList.messageKeys
        }),
        messages() {
            return this.messageKeys
                .slice(0, this.length)
                .map(key => this.$store.state.mail.messages[key])
                .filter(({ key }) => this.isLoaded(key));
        },
        currentMessage() {
            return this.messages[this.currentMessageKey];
        },
        hasMore: function () {
            return this.length < this.MESSAGE_LIST_COUNT;
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
    mounted() {
        this.onScroll();
    },
    updated() {
        this.onScroll();
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
                const end = Math.min(this.length + 20, this.MESSAGE_LIST_COUNT);
                await this.loadRange({ start: this.length, end });
                this.length = end;
            }
        },
        onScroll() {
            const total = this.$el.scrollHeight;
            const current = this.$el.scrollTop + this.$el.offsetHeight;
            if (current >= total) {
                this.loadMore();
            }
        },
        remove(event) {
            // '.delete' modifier captures both 'Delete' and 'Backspace' keys, we just want 'Delete'
            if (event.key === "Delete") {
                if (this.activeFolder === this.MY_TRASH.key) {
                    this.purge(event);
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
            }
        },
        async purge(event) {
            // '.delete' modifier captures both 'Delete' and 'Backspace' keys, we just want 'Delete'
            if (event.key === "Delete") {
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
            }
        },
        goToByDiff(diff) {
            this.goToByIndex(this.messageKeys.indexOf(this.lastFocusedMessage) + diff);
        },
        goToByIndex(index) {
            index = Math.min(Math.max(0, index), this.MESSAGE_LIST_COUNT - 1);
            this.goToByKey(this.messageKeys[index]);
        },
        goToByKey(key) {
            this.$router.navigate({ name: "v:mail:message", params: { message: key } });
            this.deleteAllSelectedMessages();
        },
        focusByKey(key) {
            if (key) {
                this.$nextTick(() => {
                    const htmlElement = this.$refs["message-" + key];
                    if (htmlElement && htmlElement[0] && htmlElement[0].$el) {
                        htmlElement[0].$el.focus();
                    } else {
                        console.log("not in DOM..");
                    }
                });
                this.lastFocusedMessage = key;
            }
        },
        focusByDiff(diff) {
            this.focusByIndex(this.messageKeys.indexOf(this.lastFocusedMessage) + diff);
        },
        focusByIndex(index) {
            if (index !== -1 && this.messageKeys[index]) {
                this.focusByKey(this.messageKeys[index]);
            }
        },
        selectRangeByDiff(diff, shouldReset = false) {
            const index = this.messageKeys.indexOf(this.lastFocusedMessage) + diff;
            if (this.messageKeys[index]) {
                this.selectRange(this.messageKeys[index].key, shouldReset);
            }
        },
        selectRange(destinationMessageKey, shouldReset = false) {
            this.checkReset(shouldReset);
            this.initAnchored();
            if (this.anchoredMessageForShift && destinationMessageKey) {
                const startIndex = this.messageKeys.indexOf(this.anchoredMessageForShift);
                const endIndex = this.messageKeys.indexOf(destinationMessageKey);
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
            return this.messageKeys.slice(realStart, realEnd + 1).forEach(key => this.addSelectedMessageKey(key));
        },
        initAnchored() {
            if (!this.anchoredMessageForShift) {
                this.anchoredMessageForShift = this.lastFocusedMessage || this.currentMessageKey || this.messageKeys[0];
            }
        },
        toggleAll() {
            if (this.areAllMessagesSelected) {
                this.deleteAllSelectedMessages();
                this.clearCurrentMessage();
            } else {
                this.addAllToSelectedMessages(this.messageKeys);
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

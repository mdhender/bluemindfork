<template>
    <bm-list-group
        class="message-list bg-extra-light"
        tabindex="0"
        @scroll="onScroll"
        @keyup.shift.delete.exact.prevent="REMOVE_MESSAGES(selected)"
        @keyup.delete.exact.prevent="MOVE_MESSAGES_TO_TRASH(selected)"
        @keyup.up.exact="goToByDiff(-1)"
        @keydown.up.prevent
        @keyup.down.exact="goToByDiff(+1)"
        @keydown.down.prevent
        @keyup.page-down.exact="goToByDiff(+PAGE)"
        @keydown.page-down.prevent
        @keyup.page-up.exact="goToByDiff(-PAGE)"
        @keydown.page-up.prevent
        @keyup.home.exact="goToByIndex(0)"
        @keyup.end.exact="goToByIndex(MESSAGE_LIST_COUNT - 1)"
        @keyup.space.exact="goToByKey(lastFocusedMessage)"
        @keydown.space.prevent
        @keyup.ctrl.exact.space="toggleInSelection(lastFocusedMessage)"
        @keyup.ctrl.exact.65="toggleAll()"
        @keydown.ctrl.exact.65.prevent
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
        <div v-for="(message, index) in _messages" :key="message.key">
            <date-separator v-if="MESSAGE_IS_LOADED(message.key)" :message="message" :index="index" />
            <draggable-message
                v-if="MESSAGE_IS_LOADED(message.key)"
                :ref="'message-' + message.key"
                :message="message"
                :is-muted="!!draggedMessage && MESSAGE_IS_SELECTED(draggedMessage) && MESSAGE_IS_SELECTED(message.key)"
                @toggle-select="toggleSelect"
                @click.exact.native="unselectAllIfNeeded(message.key)"
                @click.ctrl.exact.native.prevent.stop="toggleInSelection(message.key)"
                @click.shift.exact.native.prevent.stop="selectRange(message.key, true)"
                @click.ctrl.shift.exact.native.prevent.stop="selectRange(message.key)"
                @dragstart="draggedMessage = message.key"
                @dragend="draggedMessage = null"
            />
            <message-list-item-loading v-else-if="MESSAGE_IS_LOADING(message.key)" :message="message" />
        </div>
    </bm-list-group>
</template>

<script>
import { BmListGroup } from "@bluemind/styleguide";
import { mapState, mapGetters, mapActions, mapMutations } from "vuex";
import { TOGGLE_SELECTION_ALL } from "../VueBusEventTypes";
import DraggableMessage from "./DraggableMessage";
import DateSeparator from "./DateSeparator";
import MessageListItemLoading from "./MessageListItemLoading";
import {
    MULTIPLE_MESSAGE_SELECTED,
    SELECTION_IS_EMPTY,
    MESSAGE_IS_SELECTED,
    ALL_MESSAGES_ARE_SELECTED,
    MY_TRASH,
    MESSAGE_LIST_COUNT,
    MESSAGE_IS_LOADED,
    MESSAGE_IS_LOADING
} from "~getters";
import { SELECT_MESSAGE, UNSELECT_MESSAGE, SELECT_ALL_MESSAGES, UNSELECT_ALL_MESSAGES } from "~mutations";
import { RemoveMixin } from "~mixins";
import { MessageStatus } from "~model/message";

const PAGE = 9;

export default {
    name: "MessageList",
    components: {
        BmListGroup,
        DateSeparator,
        DraggableMessage,
        MessageListItemLoading
    },
    mixins: [RemoveMixin],
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
        ...mapState("mail-webapp/currentMessage", { currentMessageKey: "key" }),
        ...mapGetters("mail", {
            MULTIPLE_MESSAGE_SELECTED,
            SELECTION_IS_EMPTY,
            MESSAGE_IS_SELECTED,
            ALL_MESSAGES_ARE_SELECTED,
            MY_TRASH,
            MESSAGE_LIST_COUNT,
            MESSAGE_IS_LOADED,
            MESSAGE_IS_LOADING
        }),
        ...mapState("mail", ["activeFolder", "messages", "selection"]),
        ...mapState("mail", {
            messageKeys: state => state.messageList.messageKeys
        }),
        _messages() {
            return this.messageKeys
                .slice(0, this.length)
                .map(key => this.messages[key])
                .filter(({ status, loading }) => status !== MessageStatus.REMOVED);
        },
        currentMessage() {
            return this.messages[this.currentMessageKey];
        },
        hasMore() {
            return this.length < this.MESSAGE_LIST_COUNT;
        },
        selected() {
            return this.SELECTION_IS_EMPTY ? this.currentMessage : this.selection.map(key => this.messages[key]);
        }
    },
    watch: {
        currentMessageKey() {
            if (this.currentMessageKey) {
                this.focusByKey(this.currentMessageKey);
                this.anchoredMessageForShift = this.currentMessageKey;
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
        ...mapMutations("mail-webapp/currentMessage", { clearCurrentMessage: "clear" }),
        ...mapMutations("mail", { SELECT_MESSAGE, UNSELECT_MESSAGE, SELECT_ALL_MESSAGES, UNSELECT_ALL_MESSAGES }),

        loadMore() {
            if (this.hasMore) {
                const end = Math.min(this.length + 20, this.MESSAGE_LIST_COUNT);
                this.loadRange({ start: this.length, end });
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
        goToByDiff(diff) {
            this.goToByIndex(this.messageKeys.indexOf(this.lastFocusedMessage) + diff);
        },
        goToByIndex(index) {
            index = Math.min(Math.max(0, index), this.MESSAGE_LIST_COUNT - 1);
            this.goToByKey(this.messageKeys[index]);
        },
        goToByKey(key) {
            this.$router.navigate({ name: "v:mail:message", params: { message: key } });
            this.UNSELECT_ALL_MESSAGES();
        },
        async focusByKey(key) {
            if (key) {
                await this.$nextTick();
                const htmlElement = this.$refs["message-" + key];
                if (htmlElement && htmlElement[0] && htmlElement[0].$el) {
                    htmlElement[0].$el.focus();
                }
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
                this.selectRange(this.messageKeys[index], shouldReset);
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
                this.UNSELECT_ALL_MESSAGES();
            }
        },
        addMessageKeysBetween(start, end) {
            const realStart = start < end ? start : end;
            const realEnd = start < end ? end : start;
            return this.messageKeys.slice(realStart, realEnd + 1).forEach(key => this.SELECT_MESSAGE(key));
        },
        initAnchored() {
            if (!this.anchoredMessageForShift) {
                this.anchoredMessageForShift = this.lastFocusedMessage || this.currentMessageKey || this.messageKeys[0];
            }
        },
        toggleAll() {
            if (this.ALL_MESSAGES_ARE_SELECTED) {
                this.UNSELECT_ALL_MESSAGES();
                this.clearCurrentMessage();
            } else {
                this.SELECT_ALL_MESSAGES(this.messageKeys);
            }
            this.navigateAfterSelection();
        },
        toggleInSelection(messageKey) {
            if (this.SELECTION_IS_EMPTY && this.currentMessageKey && this.currentMessageKey !== messageKey) {
                this.SELECT_MESSAGE(this.currentMessageKey);
            }
            this.toggleSelect(messageKey);
        },
        toggleSelect(messageKey) {
            if (this.MESSAGE_IS_SELECTED(messageKey)) {
                this.UNSELECT_MESSAGE(messageKey);
                if (this.currentMessageKey === messageKey) {
                    this.clearCurrentMessage();
                }
            } else {
                this.SELECT_MESSAGE(messageKey);
                this.anchoredMessageForShift = messageKey;
            }
            this.focusByKey(messageKey);
            this.navigateAfterSelection();
        },
        navigateAfterSelection() {
            this.$router.navigate({ name: "v:mail:home" });
        },
        unselectAllIfNeeded(messageKey) {
            if (this.selection.length !== 1 || this.selection[0] !== messageKey) {
                this.UNSELECT_ALL_MESSAGES();
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

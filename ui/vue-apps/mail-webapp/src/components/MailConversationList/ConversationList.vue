<template>
    <bm-list-group
        class="conversation-list bg-extra-light"
        tabindex="0"
        @scroll="onScroll"
        @keyup.shift.delete.exact.prevent="REMOVE_CONVERSATIONS(selected)"
        @keyup.delete.exact.prevent="MOVE_CONVERSATIONS_TO_TRASH(selected)"
        @keyup.up.exact="goToByDiff(-1)"
        @keydown.up.prevent
        @keyup.down.exact="goToByDiff(+1)"
        @keydown.down.prevent
        @keyup.page-down.exact="goToByDiff(+PAGE)"
        @keydown.page-down.prevent
        @keyup.page-up.exact="goToByDiff(-PAGE)"
        @keydown.page-up.prevent
        @keyup.home.exact="goToByIndex(0)"
        @keyup.end.exact="goToByIndex(CONVERSATION_LIST_COUNT - 1)"
        @keyup.space.exact="goToByKey(lastFocusedConversation)"
        @keydown.space.prevent
        @keyup.ctrl.exact.space="toggleInSelection(lastFocusedConversation)"
        @keyup.ctrl.exact.65="toggleAll()"
        @keydown.ctrl.exact.65.prevent
        @keyup.ctrl.exact.up="focusByDiff(-1)"
        @keyup.ctrl.exact.down="focusByDiff(+1)"
        @keyup.ctrl.exact.home="focusByIndex(0)"
        @keyup.ctrl.exact.end="focusByIndex(CONVERSATION_LIST_COUNT - 1)"
        @keyup.shift.exact.space="selectRange(lastFocusedConversation, true)"
        @keyup.shift.exact.up="selectRangeByDiff(-1, true)"
        @keyup.shift.exact.down="selectRangeByDiff(+1, true)"
        @keyup.shift.exact.home="selectRange(CONVERSATION_LIST_ALL_KEYS[0], true)"
        @keyup.shift.exact.end="selectRange(CONVERSATION_LIST_ALL_KEYS[CONVERSATION_LIST_COUNT - 1], true)"
        @keyup.shift.ctrl.exact.space="selectRange(lastFocusedConversation)"
        @keyup.shift.ctrl.exact.up="selectRangeByDiff(-1)"
        @keyup.shift.ctrl.exact.down="selectRangeByDiff(+1)"
        @keyup.shift.ctrl.exact.home="selectRange(CONVERSATION_LIST_ALL_KEYS[0])"
        @keyup.shift.ctrl.exact.end="selectRange(CONVERSATION_LIST_ALL_KEYS[CONVERSATION_LIST_COUNT - 1])"
    >
        <div v-for="(conversation, index) in conversations" :key="conversation.key">
            <template v-if="CONVERSATION_IS_LOADED(conversation) && matchFilter(conversation, filter)">
                <date-separator :conversation="conversation" :index="index" />
                <draggable-conversation
                    :ref="'conversation-' + conversation.key"
                    :conversation="conversation"
                    :is-muted="
                        !!draggedConversation &&
                        CONVERSATION_IS_SELECTED(draggedConversation) &&
                        CONVERSATION_IS_SELECTED(conversation.key)
                    "
                    @toggle-select="toggleSelect"
                    @click.ctrl.exact.native.prevent.stop="toggleInSelection(conversation.key)"
                    @click.shift.exact.native.prevent.stop="selectRange(conversation.key, true)"
                    @click.ctrl.shift.exact.native.prevent.stop="selectRange(conversation.key)"
                    @dragstart="draggedConversation = conversation.key"
                    @dragend="draggedConversation = null"
                />
            </template>
            <conversation-list-item-loading
                v-else-if="!CONVERSATION_IS_LOADED(conversation)"
                :conversation="conversation"
            />
        </div>
    </bm-list-group>
</template>

<script>
import { BmListGroup } from "@bluemind/styleguide";
import { mapState, mapGetters, mapActions, mapMutations } from "vuex";
import { TOGGLE_SELECTION_ALL } from "../VueBusEventTypes";
import DraggableConversation from "./DraggableConversation";
import DateSeparator from "./DateSeparator";
import ConversationListItemLoading from "./ConversationListItemLoading";
import {
    ALL_CONVERSATIONS_ARE_SELECTED,
    CONVERSATION_IS_LOADED,
    CONVERSATION_IS_LOADING,
    CONVERSATION_METADATA,
    CONVERSATION_IS_SELECTED,
    CONVERSATION_LIST_KEYS,
    CONVERSATION_LIST_ALL_KEYS,
    CONVERSATION_LIST_COUNT,
    CONVERSATION_LIST_HAS_NEXT,
    CONVERSATION_LIST_CONVERSATIONS,
    SEVERAL_CONVERSATIONS_SELECTED,
    MY_TRASH,
    SELECTION,
    SELECTION_IS_EMPTY
} from "~/getters";
import {
    RESET_CONVERSATION_LIST_PAGE,
    SET_CURRENT_CONVERSATION,
    SELECT_ALL_CONVERSATIONS,
    SELECT_CONVERSATION,
    UNSELECT_ALL_CONVERSATIONS,
    UNSELECT_CONVERSATION
} from "~/mutations";
import { CONVERSATION_LIST_NEXT_PAGE, FETCH_MESSAGE_METADATA } from "~/actions";

import { RemoveMixin } from "~/mixins";
import { LoadingStatus } from "~/model/loading-status";
import { matchFilter } from "~/model/conversations";

const PAGE = 9;

export default {
    name: "FolderResultContent",
    components: {
        BmListGroup,
        ConversationListItemLoading,
        DateSeparator,
        DraggableConversation
    },
    mixins: [RemoveMixin],
    data() {
        return {
            PAGE,
            length: 20,
            lastFocusedConversation: null,
            anchoredConversationForShift: null,
            draggedConversation: null,
            matchFilter
        };
    },
    computed: {
        ...mapGetters("mail", {
            ALL_CONVERSATIONS_ARE_SELECTED,
            CONVERSATION_IS_LOADED,
            CONVERSATION_IS_LOADING,
            CONVERSATION_METADATA,
            CONVERSATION_IS_SELECTED,
            CONVERSATION_LIST_KEYS,
            CONVERSATION_LIST_ALL_KEYS,
            CONVERSATION_LIST_COUNT,
            CONVERSATION_LIST_HAS_NEXT,
            CONVERSATION_LIST_CONVERSATIONS,
            SEVERAL_CONVERSATIONS_SELECTED,
            MY_TRASH,
            SELECTION,
            SELECTION_IS_EMPTY
        }),
        ...mapState("mail", ["activeFolder"]),
        ...mapState("mail", {
            messages: ({ conversations }) => conversations.messages,
            conversationByKey: ({ conversations }) => conversations.conversationByKey,
            currentConversation: ({ conversations }) => conversations.currentConversation,
            filter: ({ conversationList }) => conversationList.filter
        }),
        hasMore() {
            return this.length < this.CONVERSATION_LIST_COUNT;
        },
        selected() {
            return this.SELECTION_IS_EMPTY ? this.currentConversation : this.SELECTION;
        },
        conversations() {
            return this.CONVERSATION_LIST_KEYS.map(key => this.CONVERSATION_METADATA(key)).filter(Boolean);
        }
    },
    watch: {
        currentConversation() {
            if (this.currentConversation) {
                this.focusByKey(this.currentConversation.key);
                this.anchoredConversationForShift = this.currentConversation.key;
            }
        },
        activeFolder() {
            this.lastFocusedConversation = null;
            this.anchoredConversationForShift = null;
        },
        CONVERSATION_LIST_KEYS() {
            const conversationsToLoad = this.conversations.filter(({ loading }) =>
                [LoadingStatus.NOT_LOADED, LoadingStatus.LOADING].includes(loading)
            );
            if (conversationsToLoad.length > 0) {
                const messagesToLoad = this.conversations.flatMap(conversation => conversation.messages);
                this.FETCH_MESSAGE_METADATA({
                    messages: messagesToLoad,
                    activeFolderKey: this.activeFolder.key
                });
            }
        }
    },
    created() {
        this.RESET_CONVERSATION_LIST_PAGE();
        this.focusByKey(this.currentConversation?.key);
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
        ...mapMutations("mail", {
            RESET_CONVERSATION_LIST_PAGE,
            SET_CURRENT_CONVERSATION,
            SELECT_ALL_CONVERSATIONS,
            SELECT_CONVERSATION,
            UNSELECT_ALL_CONVERSATIONS,
            UNSELECT_CONVERSATION
        }),
        ...mapActions("mail", { FETCH_MESSAGE_METADATA, CONVERSATION_LIST_NEXT_PAGE }),

        async loadMore() {
            if (this.CONVERSATION_LIST_HAS_NEXT) {
                this.CONVERSATION_LIST_NEXT_PAGE();
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
            this.goToByIndex(this.CONVERSATION_LIST_ALL_KEYS.indexOf(this.lastFocusedConversation) + diff);
        },
        goToByIndex(index) {
            index = Math.min(Math.max(0, index), this.CONVERSATION_LIST_COUNT - 1);
            this.goToByKey(this.CONVERSATION_LIST_ALL_KEYS[index]);
        },
        goToByKey(key) {
            const conversation = this.conversationByKey[key];
            this.SET_CURRENT_CONVERSATION(conversation);
            this.$router.navigate({
                name: "v:mail:conversation",
                params: { conversation }
            });
        },
        async focusByKey(key) {
            if (key) {
                await this.$nextTick();
                const htmlElement = this.$refs["conversation-" + key];
                if (htmlElement && htmlElement[0] && htmlElement[0].$el) {
                    htmlElement[0].$el.focus();
                }
                this.lastFocusedConversation = key;
            }
        },
        focusByDiff(diff) {
            this.focusByIndex(this.CONVERSATION_LIST_ALL_KEYS.indexOf(this.lastFocusedConversation) + diff);
        },
        focusByIndex(index) {
            if (index !== -1 && this.CONVERSATION_LIST_ALL_KEYS[index]) {
                this.focusByKey(this.CONVERSATION_LIST_ALL_KEYS[index]);
            }
        },
        selectRangeByDiff(diff, shouldReset = false) {
            const index = this.CONVERSATION_LIST_ALL_KEYS.indexOf(this.lastFocusedConversation) + diff;

            if (this.CONVERSATION_LIST_ALL_KEYS[index]) {
                this.selectRange(this.CONVERSATION_LIST_ALL_KEYS[index], shouldReset);
            }
        },
        selectRange(destinationConversationKey, shouldReset = false) {
            this.checkReset(shouldReset);
            this.initAnchored();
            if (this.anchoredConversationForShift && destinationConversationKey) {
                const startIndex = this.CONVERSATION_LIST_ALL_KEYS.indexOf(this.anchoredConversationForShift);
                const endIndex = this.CONVERSATION_LIST_ALL_KEYS.indexOf(destinationConversationKey);
                this.addConversationKeysBetween(startIndex, endIndex);
                this.focusByKey(destinationConversationKey);
                this.navigateAfterSelection();
            }
        },
        checkReset(shouldReset) {
            if (shouldReset) {
                this.UNSELECT_ALL_CONVERSATIONS();
            }
        },
        addConversationKeysBetween(start, end) {
            const realStart = start < end ? start : end;
            const realEnd = start < end ? end : start;
            return this.CONVERSATION_LIST_ALL_KEYS.slice(realStart, realEnd + 1).forEach(key =>
                this.SELECT_CONVERSATION(key)
            );
        },
        initAnchored() {
            if (!this.anchoredConversationForShift) {
                this.anchoredConversationForShift =
                    this.lastFocusedConversation || this.currentConversation?.key || this.CONVERSATION_LIST_ALL_KEYS[0];
            }
        },
        toggleAll() {
            if (this.ALL_CONVERSATIONS_ARE_SELECTED) {
                this.UNSELECT_ALL_CONVERSATIONS();
            } else {
                this.SELECT_ALL_CONVERSATIONS(this.CONVERSATION_LIST_ALL_KEYS);
            }
            this.navigateAfterSelection();
        },
        toggleInSelection(messageKey) {
            if (this.SELECTION_IS_EMPTY && this.currentConversation && this.currentConversation?.key !== messageKey) {
                this.SELECT_CONVERSATION(this.currentConversation.key);
            }
            this.toggleSelect(messageKey);
        },
        toggleSelect(messageKey) {
            this.SET_CURRENT_CONVERSATION(null);
            if (this.CONVERSATION_IS_SELECTED(messageKey)) {
                this.UNSELECT_CONVERSATION(messageKey);
            } else {
                this.SELECT_CONVERSATION(messageKey);
                this.anchoredConversationForShift = messageKey;
            }
            this.focusByKey(messageKey);
            this.navigateAfterSelection();
        },
        navigateAfterSelection() {
            this.$router.navigate({ name: "v:mail:home" });
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/variables";
.conversation-list {
    overflow-x: hidden;
    overflow-y: auto;
}
</style>

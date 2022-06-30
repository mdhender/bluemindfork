<template>
    <bm-list-group
        class="conversation-list"
        tabindex="0"
        @scroll="onScroll"
        @keyup.up.prevent="selectKey(byPosition(-1), $event)"
        @keydown.up.prevent
        @keyup.down.prevent="selectKey(byPosition(+1), $event)"
        @keydown.down.prevent
        @keyup.page-down.prevent="selectKey(byPosition(+PAGE), $event)"
        @keydown.page-down.prevent
        @keyup.page-up.prevent="selectKey(byPosition(-PAGE), $event)"
        @keydown.page-up.prevent
        @keyup.home="selectKey(byIndex(0), $event)"
        @keyup.end="selectKey(byIndex(allConversationKeys.length - 1), $event)"
        @keyup.space="selectKey(focused, $event)"
        @keydown.space.prevent
        @keyup.ctrl.exact.65="selectAll()"
        @keydown.ctrl.exact.65.prevent
    >
        <div
            v-for="conversation in conversations"
            :key="conversation.key"
            :set="(conversationIsSelected = isSelected(conversation.key))"
            class="bg-surface"
        >
            <template v-if="CONVERSATION_IS_LOADED(conversation)">
                <conversation-list-separator
                    v-if="dateRangeByKey[conversation.key]"
                    :text="dateRangeText(dateRangeByKey[conversation.key])"
                />
                <draggable-conversation
                    :ref="'conversation-' + conversation.key"
                    :conversation="conversation"
                    :draggable="draggable"
                    :is-muted="!!draggedConversation && isSelected(draggedConversation) && conversationIsSelected"
                    :is-selected="conversationIsSelected"
                    :multiple="multiple"
                    :selection-mode="selectionMode"
                    @click.native="selectKey(conversation.key, $event)"
                    @check="selectKey(conversation.key)"
                    @dragstart="draggedConversation = conversation.key"
                    @dragend="draggedConversation = null"
                >
                    <template v-slot:actions>
                        <slot name="actions" :conversation="conversation"></slot>
                    </template>
                </draggable-conversation>
            </template>
            <conversation-list-item-loading v-else :is-selected="isSelected(conversation.key)" />
        </div>
    </bm-list-group>
</template>

<script>
import { BmListGroup } from "@bluemind/styleguide";
import { mapState, mapGetters, mapActions } from "vuex";
import { loadingStatusUtils } from "@bluemind/mail";
import { CONVERSATIONS_ACTIVATED, CONVERSATION_IS_LOADED, CONVERSATION_METADATA } from "~/getters";
import { FETCH_CONVERSATIONS, FETCH_MESSAGE_METADATA } from "~/actions";
import ConversationListItemLoading from "./ConversationListItemLoading";
import ConversationListSeparator from "./ConversationListSeparator";
import DateRanges from "./DateRanges";
import DraggableConversation from "./DraggableConversation";

const { LoadingStatus } = loadingStatusUtils;

const PAGE = 9;
export const SELECTION_MODE = {
    MULTI: "MULTI",
    MONO: "MONO"
};

export default {
    name: "ConversationList",
    components: {
        BmListGroup,
        ConversationListItemLoading,
        ConversationListSeparator,
        DraggableConversation
    },
    props: {
        conversationKeys: {
            type: Array,
            required: true
        },
        allConversationKeys: {
            type: Array,
            required: true
        },
        multiple: {
            type: Boolean,
            required: false,
            default: true
        },
        selected: {
            type: [Number, String, Array],
            required: false,
            default: null
        },
        showSeparator: {
            type: Boolean,
            required: false,
            default: true
        },
        draggable: {
            type: Boolean,
            required: false,
            default: false
        }
    },
    data() {
        return {
            PAGE,
            length: 20,
            focused: null,
            anchored: null,
            draggedConversation: null,
            LoadingStatus
        };
    },
    computed: {
        ...mapGetters("mail", { CONVERSATIONS_ACTIVATED, CONVERSATION_IS_LOADED, CONVERSATION_METADATA }),
        ...mapState("mail", ["folders", "activeFolder"]),
        ...mapState("mail", {
            messages: ({ conversations }) => conversations.messages
        }),
        conversations() {
            return this.conversationKeys
                .map(key => this.CONVERSATION_METADATA(key))
                .filter(conversation => conversation.loading !== LoadingStatus.ERROR);
        },
        dateRangeByKey() {
            const dateRangeByKey = {};
            const allDateRanges = new DateRanges();
            const currentDateRanges = [];
            this.conversations.forEach(conversation => {
                if (conversation.date) {
                    const dateRange = getDateRange(conversation, allDateRanges, currentDateRanges);
                    if (dateRange) {
                        dateRangeByKey[conversation.key] = dateRange;
                    }
                }
            });
            return dateRangeByKey;
        },
        selectionMode() {
            return Array.isArray(this.selected) && this.selected.length > 0
                ? SELECTION_MODE.MULTI
                : SELECTION_MODE.MONO;
        }
    },
    watch: {
        selected() {
            if (this.selectionMode === SELECTION_MODE.MONO && this.selected) {
                this.focusByKey(this.selected);
                this.anchored = this.selected;
            }
        },
        conversationKeys: {
            async handler() {
                const conversationsToLoad = this.conversations.filter(
                    ({ loading }) => loading === LoadingStatus.NOT_LOADED
                );
                if (conversationsToLoad.length > 0) {
                    await this.FETCH_CONVERSATIONS({
                        conversations: conversationsToLoad,
                        folder: this.folders[this.activeFolder],
                        conversationsActivated: this.CONVERSATIONS_ACTIVATED
                    });
                }
                const messagesToLoad = this.conversations.flatMap(conversation => {
                    if (conversation.loading === LoadingStatus.LOADING) {
                        return conversation.messages.filter(key => this.messages[key].loading !== LoadingStatus.LOADED);
                    }
                    return [];
                });
                if (messagesToLoad.length > 0) {
                    this.FETCH_MESSAGE_METADATA({ messages: messagesToLoad });
                }
            },
            immediate: true
        }
    },
    created() {
        if (this.selected && !Array.isArray(this.selected)) {
            this.focusByKey(this.selected?.key);
        }
    },
    methods: {
        ...mapActions("mail", { FETCH_CONVERSATIONS, FETCH_MESSAGE_METADATA }),
        onScroll() {
            const total = this.$el.scrollHeight;
            const current = this.$el.scrollTop + this.$el.offsetHeight;
            if (total !== 0 && current + 1 >= total && this.conversationKeys.length < this.allConversationKeys.length) {
                this.$emit("next-page");
            }
        },
        async focusByKey(key) {
            if (key) {
                await this.$nextTick();
                const htmlElement = this.$refs["conversation-" + key];
                if (htmlElement && htmlElement[0] && htmlElement[0].$el) {
                    htmlElement[0].$el.focus();
                }
                this.focused = key;
            }
        },

        byPosition(position) {
            const index = this.allConversationKeys.indexOf(this.focused);
            return this.byIndex(index + position);
        },
        byIndex(index) {
            if (this.allConversationKeys[index]) {
                return this.allConversationKeys[index];
            }
        },
        range(key) {
            this.anchored = this.anchored || this.focused || this.selected?.key || this.allConversationKeys[0];
            const end = this.allConversationKeys.indexOf(key);
            const start = this.allConversationKeys.indexOf(this.anchored);
            return this.allConversationKeys.slice(Math.min(start, end), Math.max(start, end) + 1);
        },
        selectKey(key, event = {}) {
            if (key && isASelection(event)) {
                if (selectionMode(event, this.multiple) === SELECTION_MODE.MONO) {
                    this.$emit("set-selection", key, SELECTION_MODE.MONO);
                } else if (isARangeSelection(event)) {
                    const range = this.range(key);
                    const type = isASelectionMutation(event) ? "add-to-selection" : "set-selection";
                    this.$emit(type, range, SELECTION_MODE.MULTI);
                } else if (this.isSelected(key) && this.selectionMode === SELECTION_MODE.MULTI) {
                    this.$emit("remove-from-selection", [key]);
                } else {
                    const selection = selectionKeys(event, this.selected, key);
                    this.$emit("add-to-selection", selection);
                    this.anchored = key;
                }
            }
            this.focusByKey(key);
        },
        selectAll() {
            if (this.multiple) {
                this.$emit("set-selection", this.allConversationKeys, SELECTION_MODE.MULTI);
            }
        },
        isSelected(key) {
            return Array.isArray(this.selected) ? this.selected.indexOf(key) >= 0 : this.selected === key;
        },
        dateRangeText(dateRange) {
            return this.$t(dateRange.i18n, {
                date: this.$d(dateRange.date, dateRange.dateFormat)
            });
        }
    }
};

function isASelection({ shiftKey, ctrlKey, type, code }) {
    return type !== "keyup" || !ctrlKey || shiftKey || code === "Space";
}
function isARangeSelection({ shiftKey }) {
    return shiftKey;
}
function isASelectionMutation({ ctrlKey }) {
    return ctrlKey;
}
function selectionMode({ shiftKey, ctrlKey, type }, multipleEnabled) {
    return multipleEnabled && (shiftKey || ctrlKey || !type) ? SELECTION_MODE.MULTI : SELECTION_MODE.MONO;
}
function selectionKeys({ type }, selected, key) {
    if (!type || Array.isArray(selected) || !selected) {
        return [key];
    } else {
        return [selected, key];
    }
}
function getDateRange(conversation, allDateRanges, currentDateRanges) {
    const dateRange = allDateRanges.sortedArray.find(dateRange =>
        dateRange.contains(typeof conversation.date === "number" ? conversation.date : conversation.date.getTime())
    );
    if (!currentDateRanges.includes(dateRange)) {
        currentDateRanges.push(dateRange);
        return dateRange;
    }
}
</script>

<style lang="scss">
@import "@bluemind/styleguide/css/_variables.scss";

.conversation-list {
    overflow-x: hidden;
    overflow-y: auto;
}
</style>

<template>
    <div class="mail-conversation-list-filters desktop-only">
        <bm-check
            :checked="ALL_CONVERSATIONS_ARE_SELECTED"
            :indeterminate="!ALL_CONVERSATIONS_ARE_SELECTED && !SELECTION_IS_EMPTY"
            @change="toggleSelection"
        />
        <bm-choice-group
            ref="filterChoiceGroup"
            :options="filters"
            :selected="filters[filterIndex]"
            :aria-label="$t('mail.list.filter.aria')"
            :title="$t('mail.list.filter.tooltip')"
            class="flex-fill"
            :disabled="disabled"
        />
        <bm-dropdown
            variant="text"
            toggle-class="p-0 m-0"
            boundary="viewport"
            no-caret
            right
            no-flip
            :title="$t(`mail.list.sort.fields.${sort.field}.${sort.order}`)"
        >
            <template #button-content>
                <bm-sort-control :value="sort.order">
                    {{ $t(`mail.list.sort.fields.${sort.field}.label`) }}
                </bm-sort-control>
            </template>
            <bm-dropdown-header id="sort-header-label">
                {{ $t("mail.list.sort.title") }}
            </bm-dropdown-header>
            <bm-dropdown-divider />
            <bm-dropdown-item-button
                v-for="(option, index) in sorts"
                :key="index"
                :active="sort.field === option.field && sort.order === option.order"
                aria-describedby="sort-header-label"
                @click="sort = option"
            >
                {{ $t(`mail.list.sort.fields.${option.field}.${option.order}`) }}
            </bm-dropdown-item-button>
        </bm-dropdown>
    </div>
</template>

<script>
import {
    BmCheck,
    BmChoiceGroup,
    BmDropdown,
    BmDropdownDivider,
    BmDropdownHeader,
    BmDropdownItemButton,
    BmSortControl
} from "@bluemind/ui-components";
import { mapState, mapGetters, mapMutations } from "vuex";
import { UNSELECT_ALL_CONVERSATIONS, SET_SELECTION } from "~/mutations";
import { ALL_CONVERSATIONS_ARE_SELECTED, SELECTION_IS_EMPTY, CONVERSATION_LIST_ALL_KEYS } from "~/getters";
import { SortField, SortOrder } from "~/store/conversationList";
const FILTER_INDEXES = { all: 0, unread: 1, flagged: 2 };

export default {
    name: "MailConversationListHeader",
    components: {
        BmCheck,
        BmChoiceGroup,
        BmDropdown,
        BmDropdownDivider,
        BmDropdownHeader,
        BmDropdownItemButton,
        BmSortControl
    },
    props: {
        disabled: {
            type: Boolean,
            default: false
        }
    },
    data() {
        return {
            sorts: Object.values(SortField).flatMap(field => Object.values(SortOrder).map(order => ({ field, order })))
        };
    },
    computed: {
        ...mapState("mail", { filter: ({ conversationList }) => conversationList.filter }),
        ...mapGetters("mail", { ALL_CONVERSATIONS_ARE_SELECTED, SELECTION_IS_EMPTY, CONVERSATION_LIST_ALL_KEYS }),
        filters() {
            return [
                {
                    disabled: this.disabled,
                    text: this.$t("mail.list.filter.all"),
                    value: "all",
                    to: this.$router.relative({ name: "v:mail:home", params: { filter: null } }, this.$route)
                },
                {
                    disabled: this.disabled,
                    text: this.$t("mail.list.filter.unread"),
                    value: "unread",
                    to: this.$router.relative({ name: "v:mail:home", params: { filter: "unread" } }, this.$route)
                },
                {
                    disabled: this.disabled,
                    text: this.$t("mail.list.filter.flagged"),
                    value: "flagged",
                    to: this.$router.relative({ name: "v:mail:home", params: { filter: "flagged" } }, this.$route)
                }
            ];
        },
        filterIndex() {
            return FILTER_INDEXES[this.filter] || 0;
        },
        sort: {
            get() {
                return this.$store.state.mail.conversationList.sort;
            },
            set(value) {
                const path = this.$router.relative({ name: "v:mail:home", params: { sort: value } }, this.$route);
                this.$router.push(path);
            }
        }
    },
    watch: {
        filter() {
            this.UNSELECT_ALL_CONVERSATIONS();
        }
    },
    methods: {
        ...mapMutations("mail", { SET_SELECTION, UNSELECT_ALL_CONVERSATIONS }),
        toggleSelection() {
            if (!this.ALL_CONVERSATIONS_ARE_SELECTED) {
                this.SET_SELECTION(this.CONVERSATION_LIST_ALL_KEYS);
            } else {
                this.UNSELECT_ALL_CONVERSATIONS();
            }
            this.$router.navigate({ name: "v:mail:home" });
        }
    }
};
</script>

<style lang="scss">
@use "sass:math";
@import "~@bluemind/ui-components/src/css/utils/variables";
@import "../ConversationList/variables.scss";

.mail-conversation-list-filters {
    display: flex;
    height: $input-height-sm;
    .bm-check {
        $check-offset: math.div($avatar-width-sm - $custom-checkbox-size, 2);
        margin-left: calc(#{$not-seen-border-width} + #{$conversation-list-item-padding-left + $check-offset});
        margin-right: $sp-3;
        top: base-px-to-rem(7);
    }
}
</style>

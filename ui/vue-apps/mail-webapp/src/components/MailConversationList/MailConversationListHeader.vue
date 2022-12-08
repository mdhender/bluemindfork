<template>
    <div class="mail-conversation-list-header d-none d-lg-flex">
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
                <span class="text-overflow pr-3">{{ $t(`mail.list.sort.fields.${sort.field}.label`) }}</span>
                <div class="h-100" :class="sort.order">
                    <bm-icon
                        :stacked="[
                            { icon: 'caret-down', flip: 'vertical', transform: 'up-0.01', class: 'up' },
                            { icon: 'caret-down', transform: 'down-0.3', class: 'down' }
                        ]"
                    />
                </div>
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
    BmIcon
} from "@bluemind/ui-components";
import { mapState, mapGetters, mapMutations } from "vuex";
import { UNSELECT_ALL_CONVERSATIONS, SET_CONVERSATION_LIST_SORT, SET_SELECTION } from "~/mutations";
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
        BmIcon
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
                    text: this.$t("mail.list.filter.all"),
                    value: "all",
                    to: this.$router.relative({ name: "v:mail:home", params: { filter: null } }, this.$route)
                },
                {
                    text: this.$t("mail.list.filter.unread"),
                    value: "unread",
                    to: this.$router.relative({ name: "v:mail:home", params: { filter: "unread" } }, this.$route)
                },
                {
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
                this.$store.commit(`mail/${SET_CONVERSATION_LIST_SORT}`, value);
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
@use "sass:map";
@use "sass:math";
@import "~@bluemind/ui-components/src/css/mixins";
@import "~@bluemind/ui-components/src/css/variables";
@import "../ConversationList/_variables.scss";

.mail-conversation-list-header {
    background-color: $surface;
    border-bottom: 1px solid $neutral-fg-lo2;
    height: $input-height-sm;
}

.mail-conversation-list-header .bm-check {
    $check-offset: math.div($avatar-width-sm - $custom-checkbox-size, 2);
    margin-left: calc(#{$not-seen-border-width} + #{$conversation-list-item-padding-left + $check-offset});
    margin-right: $sp-3;
    top: base-px-to-rem(7);
}

.mail-conversation-list-header .dropdown-toggle {
    color: $primary-fg !important;
    outline-offset: 1px;

    &:hover {
        color: $primary-fg-hi1 !important;
    }

    .asc,
    .desc {
        .bm-icon {
            $size: map-get($icon-sizes, "xs");
            width: $size !important;
            height: $size !important;
        }
    }

    .asc .bm-icon {
        &.up {
            color: $primary-fg-hi1;
        }
        &.down {
            color: $primary-fg-lo1;
        }
    }
    .desc .bm-icon {
        &.up {
            color: $primary-fg-lo1;
        }
        &.down {
            color: $primary-fg-hi1;
        }
    }
}
</style>

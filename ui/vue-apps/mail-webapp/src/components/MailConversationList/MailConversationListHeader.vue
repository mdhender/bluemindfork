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
        />
    </div>
</template>

<script>
import { BmCheck, BmChoiceGroup } from "@bluemind/styleguide";
import { mapState, mapGetters, mapMutations } from "vuex";
import { UNSELECT_ALL_CONVERSATIONS, SET_SELECTION } from "~/mutations";
import { ALL_CONVERSATIONS_ARE_SELECTED, SELECTION_IS_EMPTY, CONVERSATION_LIST_ALL_KEYS } from "~/getters";
const FILTER_INDEXES = { all: 0, unread: 1, flagged: 2 };

export default {
    name: "MailConversationListHeader",
    components: { BmCheck, BmChoiceGroup },
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
@import "~@bluemind/styleguide/css/variables";
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

.fake-select {
    color: $primary-fg;
    font-weight: $font-weight-bold;
    cursor: pointer;
}
</style>

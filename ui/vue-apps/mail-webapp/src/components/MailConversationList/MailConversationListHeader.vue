<template>
    <div class="mail-conversation-list-header bg-surface pb-0 pt-1 d-none d-lg-block">
        <bm-row align-v="center" class="no-gutters">
            <bm-col cols="1">
                <bm-check
                    :checked="ALL_CONVERSATIONS_ARE_SELECTED"
                    :indeterminate="!ALL_CONVERSATIONS_ARE_SELECTED && !SELECTION_IS_EMPTY"
                    @change="toggleSelection"
                />
            </bm-col>
            <bm-col class="d-none d-lg-block">
                <bm-choice-group
                    ref="filterChoiceGroup"
                    :options="filters"
                    :selected="filters[filterIndex]"
                    :aria-label="$t('mail.list.filter.aria')"
                    :title="$t('mail.list.filter.tooltip')"
                />
            </bm-col>
        </bm-row>
    </div>
</template>

<script>
import { BmCheck, BmCol, BmRow, BmChoiceGroup } from "@bluemind/styleguide";
import { mapState, mapGetters, mapMutations } from "vuex";
import { UNSELECT_ALL_CONVERSATIONS, SET_SELECTION } from "~/mutations";
import { ALL_CONVERSATIONS_ARE_SELECTED, SELECTION_IS_EMPTY, CONVERSATION_LIST_ALL_KEYS } from "~/getters";
const FILTER_INDEXES = { all: 0, unread: 1, flagged: 2 };

export default {
    name: "MailConversationListHeader",
    components: { BmCheck, BmCol, BmRow, BmChoiceGroup },
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
@import "~@bluemind/styleguide/css/variables";
.mail-conversation-list-header {
    border-bottom: 1px solid $alternate-light;
    padding: 0.5rem;
}

.mail-conversation-list-header .bm-check {
    margin-left: 0.85em;
}

.fake-select {
    color: $info-dark;
    font-weight: $font-weight-bold;
    cursor: pointer;
}
</style>

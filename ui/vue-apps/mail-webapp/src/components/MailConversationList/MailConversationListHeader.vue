<template>
    <div class="mail-conversation-list-header bg-surface pb-0 pt-1 d-none d-lg-block">
        <bm-row align-v="center" class="no-gutters">
            <bm-col cols="1">
                <bm-check
                    :checked="ALL_CONVERSATIONS_ARE_SELECTED"
                    :indeterminate="!ALL_CONVERSATIONS_ARE_SELECTED && !SELECTION_IS_EMPTY"
                    @change="$bus.$emit(TOGGLE_SELECTION_ALL)"
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
import { RESET_ACTIVE_MESSAGE, UNSELECT_ALL_CONVERSATIONS, UNSET_CURRENT_CONVERSATION } from "~/mutations";
import { TOGGLE_SELECTION_ALL } from "../VueBusEventTypes";
import { ALL_CONVERSATIONS_ARE_SELECTED, SELECTION_IS_EMPTY } from "~/getters";
const FILTER_INDEXES = { all: 0, unread: 1, flagged: 2 };

export default {
    name: "MailConversationListHeader",
    components: {
        BmCheck,
        BmCol,
        BmRow,
        BmChoiceGroup
    },
    data() {
        return {
            TOGGLE_SELECTION_ALL
        };
    },
    computed: {
        ...mapState("mail", { filter: ({ conversationList }) => conversationList.filter }),
        ...mapGetters("mail", { ALL_CONVERSATIONS_ARE_SELECTED, SELECTION_IS_EMPTY }),
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
            this.RESET_ACTIVE_MESSAGE();
            this.UNSELECT_ALL_CONVERSATIONS();
            this.UNSET_CURRENT_CONVERSATION();
        }
    },
    methods: {
        ...mapMutations("mail", { RESET_ACTIVE_MESSAGE, UNSELECT_ALL_CONVERSATIONS, UNSET_CURRENT_CONVERSATION })
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

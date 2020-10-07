<template>
    <div class="mail-message-list-header bg-surface pb-0 pt-1 d-none d-lg-block">
        <bm-row align-v="center" class="no-gutters">
            <bm-col cols="1">
                <bm-check
                    :checked="areAllMessagesSelected"
                    :indeterminate="!areAllMessagesSelected && selectedMessageKeys.length > 0"
                    @change="$bus.$emit(TOGGLE_SELECTION_ALL)"
                />
            </bm-col>
            <bm-col class="d-none d-lg-block">
                <bm-choice-group
                    ref="filterChoiceGroup"
                    v-bm-tooltip
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
import { BmCheck, BmCol, BmRow, BmChoiceGroup, BmTooltip } from "@bluemind/styleguide";
import { mapState, mapGetters } from "vuex";
import { TOGGLE_SELECTION_ALL } from "../VueBusEventTypes";

const FILTER_INDEXES = { all: 0, unread: 1, flagged: 2 };

export default {
    name: "MailMessageListHeader",
    components: {
        BmCheck,
        BmCol,
        BmRow,
        BmChoiceGroup
    },
    directives: { BmTooltip },
    data() {
        return {
            TOGGLE_SELECTION_ALL
        };
    },
    computed: {
        ...mapState("mail", { filter: ({ messageList }) => messageList.filter }),
        ...mapState("mail-webapp", ["selectedMessageKeys"]),
        ...mapGetters("mail-webapp", ["areAllMessagesSelected"]),
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
    }
};
</script>
<style lang="scss">
@import "~@bluemind/styleguide/css/variables";
.mail-message-list-header {
    border-bottom: 1px solid $alternate-light;
    padding: 0.5rem;
}

.mail-message-list-header .bm-check {
    margin-left: 6px;
}

.fake-select {
    color: $info-dark;
    font-weight: $font-weight-bold;
    cursor: pointer;
}
</style>

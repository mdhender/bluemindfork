<template>
    <div class="mail-message-list-header bg-surface pb-0 pt-1 d-none d-lg-block">
        <bm-row align-v="center" class="no-gutters">
            <bm-col cols="1">
                <bm-check
                    :checked="areAllMessagesSelected"
                    :indeterminate="!areAllMessagesSelected && selectedMessageKeys.length > 0"
                    @change="$bus.$emit(TOGGLE_SELECTION_ALL)"
                />
                <!-- FIXME: toggleAll -->
            </bm-col>
            <bm-col class="d-none d-lg-block" cols="7">
                <bm-choice-group
                    ref="filterChoiceGroup"
                    v-bm-tooltip.ds500
                    :options="filters"
                    :selected="filters[filterIndex].value"
                    :aria-label="$t('mail.list.filter.aria')"
                    :title="$t('mail.list.filter.tooltip')"
                />
            </bm-col>
            <bm-col class="d-none d-sm-block d-md-none d-xl-block text-right" cols="4">
                <!-- hidden until the sort feature is really developed : https://forge.bluemind.net/jira/browse/FEATWEBML-573
                <span class="text-nowrap"> 
                    {{ $t("common.sort_by") }}
                    <span class="fake-select">{{ $t("common.date") }} <bm-icon icon="caret-down"/></span>
                </span> -->
            </bm-col>
        </bm-row>
    </div>
</template>

<script>
import { BmCheck, BmCol, BmRow, BmChoiceGroup, BmTooltip } from "@bluemind/styleguide";
import { mapState, mapGetters } from "vuex";
import { TOGGLE_SELECTION_ALL } from "../VueBusEventTypes";

const FILTER_INDEXES = { all: 0, unread: 1 };

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
        ...mapState("mail-webapp", ["currentFolderKey", "messageFilter", "selectedMessageKeys"]),
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
                }
            ];
        },
        filterIndex() {
            return FILTER_INDEXES[this.messageFilter] || 0;
        }
    },
    watch: {
        messageFilter() {
            this.$refs.filterChoiceGroup.select(this.filters[this.filterIndex]);
        }
    }
};
</script>
<style lang="scss">
@import "~@bluemind/styleguide/css/variables";
.mail-message-list-header {
    border-bottom: 1px solid $gray-300;
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

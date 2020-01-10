<template>
    <bm-list-group-item class="mail-message-list-header bg-surface pb-0 pt-1">
        <bm-row align-v="center" class="no-gutters">
            <bm-col cols="1">
                <bm-check />
            </bm-col>
            <bm-col class="d-none d-sm-block d-md-none d-xl-block" cols="7">
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
                <span class="text-nowrap">
                    {{ $t("common.sort_by") }}
                    <span class="fake-select">{{ $t("common.date") }} <bm-icon icon="caret-down" /></span>
                </span>
            </bm-col>
        </bm-row>
    </bm-list-group-item>
</template>

<script>
import { BmCheck, BmCol, BmListGroupItem, BmIcon, BmRow, BmChoiceGroup, BmTooltip } from "@bluemind/styleguide";
import { mapState } from "vuex";

const FILTER_INDEXES = { all: 0, unread: 1 };

export default {
    name: "MailMessageListHeader",
    components: {
        BmCheck,
        BmCol,
        BmListGroupItem,
        BmIcon,
        BmRow,
        BmChoiceGroup
    },
    directives: { BmTooltip },
    computed: {
        ...mapState("mail-webapp", ["currentFolderKey", "messageFilter"]),
        filters() {
            return [
                {
                    text: this.$t("mail.list.filter.all"),
                    value: "all",
                    to: { path: this.buildFilterRoutePath(), query: { filter: undefined } }
                },
                {
                    text: this.$t("mail.list.filter.unread"),
                    value: "unread",
                    to: { path: this.buildFilterRoutePath(), query: { filter: "unread" } }
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
    },
    methods: {
        /**
         * When changing the filter, unselect the current message (if any).
         * Remove the message-specific part of the route path.
         */
        buildFilterRoutePath() {
            const pathArray = this.$route.path.split("/");
            switch (pathArray[2]) {
                case "search":
                    return "/" + pathArray[1] + "/" + pathArray[2] + "/" + pathArray[3] + "/";
                default: {
                    if(!pathArray[2]) {
                        // add missing default folder
                        pathArray[2] = this.currentFolderKey;
                    }
                    return "/" + pathArray[1] + "/" + pathArray[2] + "/";
                }
            }
        }
    }
};
</script>
<style lang="scss">
@import "~@bluemind/styleguide/css/variables";

.mail-message-list-header .bm-check {
    margin-left: 6px;
}

.fake-select {
    color: $info-dark;
    font-weight: $font-weight-bold;
    cursor: pointer;
}
</style>

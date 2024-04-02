<template>
    <pref-filter-rule-action class="pref-filter-rule-action-deliver" :action="action">
        <bm-label-icon icon="folder" class="overflow-hidden">{{ action.folder }}</bm-label-icon>
    </pref-filter-rule-action>
</template>

<script>
import { BmLabelIcon } from "@bluemind/ui-components";
import PrefFilterRuleAction from "./PrefFilterRuleAction";

export default {
    name: "PrefFilterRuleActionDeliver",
    components: { BmLabelIcon, PrefFilterRuleAction },
    props: {
        action: {
            type: Object,
            required: true
        }
    }
};
</script>

<style lang="scss">
@import "@bluemind/ui-components/src/css/utils/text";

.pref-filter-rule-action-deliver {
    .bm-label-icon .bm-label-icon-label {
        @include text-overflow;
    }
}
</style>

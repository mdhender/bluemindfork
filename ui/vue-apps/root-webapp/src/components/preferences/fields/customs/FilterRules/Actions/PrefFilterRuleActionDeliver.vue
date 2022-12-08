<template>
    <pref-filter-rule-action class="pref-filter-rule-action-deliver" :action="action">
        <bm-label-icon icon="folder">{{ action.folder }}</bm-label-icon>
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

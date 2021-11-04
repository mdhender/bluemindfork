<template>
    <pref-filter-rule-criterion class="pref-filter-rule-text-criterion" :criterion="criterion" :negates="negates">
        <template v-if="criterion.value">
            :
            <strong class="ml-1">
                <em>"{{ criterion.value }}"</em>
            </strong>
        </template>
    </pref-filter-rule-criterion>
</template>

<script>
import PrefFilterRuleCriterion from "./PrefFilterRuleCriterion";
export default {
    name: "PrefFilterRuleTextCriterion",
    components: { PrefFilterRuleCriterion },
    props: {
        criterion: {
            type: Object,
            required: true
        },
        negates: {
            type: Boolean,
            default: false
        }
    }
};
</script>

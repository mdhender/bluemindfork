<template>
    <pref-filter-rule-criterion class="pref-filter-rule-contact-criterion" :criterion="criterion" :negates="negates">
        <bm-contact :email="criterion.value" />
    </pref-filter-rule-criterion>
</template>

<script>
import { BmContact } from "@bluemind/styleguide";
import PrefFilterRuleCriterion from "./PrefFilterRuleCriterion";
export default {
    name: "PrefFilterRuleContactCriterion",
    components: { BmContact, PrefFilterRuleCriterion },
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

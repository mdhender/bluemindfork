<template>
    <pref-filter-rule-criterion class="pref-filter-rule-contact-criterion" :criterion="criterion" :negates="negates">
        <contact :contact="{ address: criterion.value }" />
    </pref-filter-rule-criterion>
</template>

<script>
import { Contact } from "@bluemind/business-components";
import PrefFilterRuleCriterion from "./PrefFilterRuleCriterion";
export default {
    name: "PrefFilterRuleContactCriterion",
    components: { Contact, PrefFilterRuleCriterion },
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

<template>
    <pref-filter-rule-criterion class="pref-filter-rule-contact-criterion" :criterion="criterion" :negates="negates">
        <contact :contact="contact" class="text-truncate" />
    </pref-filter-rule-criterion>
</template>

<script>
import { Contact } from "@bluemind/business-components";
import { guessName } from "@bluemind/contact";

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
    },
    computed: {
        contact() {
            return { address: this.criterion.value, dn: guessName(this.criterion.value) };
        }
    }
};
</script>

<style lang="scss">
.pref-filter-rule-contact-criterion {
    .contact a {
        display: flex;
    }
}
</style>

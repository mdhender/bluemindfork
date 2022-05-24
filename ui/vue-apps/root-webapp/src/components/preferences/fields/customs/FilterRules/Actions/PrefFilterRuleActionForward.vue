<template>
    <pref-filter-rule-action class="pref-filter-rule-action-forward" :action="action">
        <bm-contact v-for="email in action.value.emails" :key="email" class="mr-1" :contact="{ address: email }" />
        <span v-if="action.value.localCopy">({{ $t("preferences.mail.filters.action.forward.keep_copy") }})</span>
    </pref-filter-rule-action>
</template>

<script>
import { BmContact } from "@bluemind/styleguide";
import PrefFilterRuleAction from "./PrefFilterRuleAction";

export default {
    name: "PrefFilterRuleActionForward",
    components: { BmContact, PrefFilterRuleAction },
    props: {
        action: {
            type: Object,
            required: true
        }
    }
};
</script>

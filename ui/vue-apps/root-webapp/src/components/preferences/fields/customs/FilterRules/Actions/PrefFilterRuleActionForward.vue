<template>
    <pref-filter-rule-action class="pref-filter-rule-action-forward" :action="action">
        <contact
            v-for="email in action.emails"
            :key="email"
            class="mr-1 overflow-hidden"
            :contact="{ address: email }"
        />
        <span v-if="action.keepCopy">({{ $t("preferences.mail.filters.action.REDIRECT.keep_copy") }})</span>
    </pref-filter-rule-action>
</template>

<script>
import { Contact } from "@bluemind/business-components";
import PrefFilterRuleAction from "./PrefFilterRuleAction";

export default {
    name: "PrefFilterRuleActionForward",
    components: { Contact, PrefFilterRuleAction },
    props: {
        action: {
            type: Object,
            required: true
        }
    }
};
</script>

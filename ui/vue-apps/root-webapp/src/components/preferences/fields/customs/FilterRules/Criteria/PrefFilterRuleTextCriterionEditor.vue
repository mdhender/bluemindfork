<template>
    <bm-form-input v-model="criterion.value" class="pref-filter-rule-text-criterion-editor" required />
</template>

<script>
import { BmFormInput } from "@bluemind/styleguide";

export default {
    name: "PrefFilterRuleTextCriterionEditor",
    components: { BmFormInput },
    props: {
        criterion: {
            type: Object,
            required: true
        }
    }
};
</script>

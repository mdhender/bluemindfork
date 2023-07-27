<template>
    <bm-form-input v-model="value" class="pref-filter-rule-text-criterion-editor" required />
</template>

<script>
import { BmFormInput } from "@bluemind/ui-components";

export default {
    name: "PrefFilterRuleTextCriterionEditor",
    components: { BmFormInput },
    props: {
        criterion: {
            type: Object,
            required: true
        }
    },
    computed: {
        value: {
            get() {
                return this.criterion.value;
            },
            set(value) {
                this.$emit("update:criterion", { ...this.criterion, value });
            }
        }
    }
};
</script>

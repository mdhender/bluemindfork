<template>
    <bm-form-group class="pref-field-check-group py-5">
        <pref-field-check v-for="(check, index) in checks" :key="index" v-bind="check" class="text-neutral" />
    </bm-form-group>
</template>

<script>
import { BmFormGroup } from "@bluemind/ui-components";
import PrefFieldCheck from "./PrefFieldCheck";

export default {
    name: "PrefFieldCheckGroup",
    components: { BmFormGroup, PrefFieldCheck },
    props: {
        label: { type: [Object, String], required: false, default: "" },
        checks: { type: Array, required: true }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables";

.pref-field-check-group {
    .pref-field-check:first-of-type {
        padding-top: $sp-3 !important;
    }
}
</style>

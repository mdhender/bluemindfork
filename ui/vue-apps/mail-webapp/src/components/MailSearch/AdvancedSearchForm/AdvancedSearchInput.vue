<template>
    <div class="advanced-search-input">
        <bm-form-input class="input d-none d-lg-flex" variant="underline" :value="value" @input="update" />
        <bm-form-input class="input d-lg-none" variant="outline" :value="value" @input="update" />
    </div>
</template>

<script>
import { BmFormInput } from "@bluemind/ui-components";

export default {
    name: "AdvancedSearchInput",
    components: { BmFormInput },
    props: {
        value: {
            type: String,
            default: ""
        }
    },
    methods: {
        update(value) {
            this.$emit("update:value", value);
        }
    }
};
</script>

<style lang="scss">
.advanced-search-input {
    width: 100%;
}
</style>

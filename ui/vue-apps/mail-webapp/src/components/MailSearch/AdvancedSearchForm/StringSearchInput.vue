<template>
    <div class="string-search-input">
        <bm-form-input class="input desktop-only" variant="underline" :value="value" @input="update" />
        <bm-form-input class="input mobile-only" variant="outline" :value="value" @input="update" />
    </div>
</template>

<script>
import { BmFormInput } from "@bluemind/ui-components";

export default {
    name: "StringSearchInput",
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
.string-search-input {
    width: 100%;
}
</style>

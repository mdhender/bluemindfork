<template>
    <bm-button class="search-button mr-1" variant="fill-accent" :disabled="disabled" @click="search">
        {{ $t("common.action.search") }}
    </bm-button>
</template>

<script>
import { BmButton } from "@bluemind/ui-components";

export default {
    name: "SearchButton",
    components: { BmButton },
    props: {
        disabled: {
            type: Boolean,
            default: false
        }
    },
    methods: {
        search() {
            this.$emit("search");
        }
    }
};
</script>
<style lang="scss">
.search-button {
    white-space: nowrap;
}
</style>

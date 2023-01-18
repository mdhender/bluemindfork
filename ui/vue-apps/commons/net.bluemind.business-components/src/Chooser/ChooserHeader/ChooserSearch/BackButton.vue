<template>
    <bm-button variant="outline" class="back-button" icon="arrow-back" @click="unsetSearchMode" />
</template>

<script>
import { BmButton } from "@bluemind/ui-components";
import { UNSET_SEARCH_PATTERN, UNSET_SEARCH_MODE } from "../../store/mutations";

export default {
    name: "BackButton",
    components: { BmButton },
    methods: {
        unsetSearchMode() {
            this.$store.commit(`chooser/${UNSET_SEARCH_PATTERN}`);
            this.$store.commit(`chooser/${UNSET_SEARCH_MODE}`);
            this.$emit("back");
        }
    }
};
</script>
<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables";

.back-button.btn-outline {
    background-color: $surface;
}
</style>

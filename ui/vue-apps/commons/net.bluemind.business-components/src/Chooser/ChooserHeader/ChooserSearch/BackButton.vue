<template>
    <bm-button variant="outline" class="back-button" @click="unsetSearchMode">
        <bm-icon icon="arrow-back" />
    </bm-button>
</template>

<script>
import { BmButton, BmIcon } from "@bluemind/styleguide";
import { UNSET_SEARCH_PATTERN, UNSET_SEARCH_MODE } from "../../store/mutations";

export default {
    name: "BackButton",
    components: { BmButton, BmIcon },
    methods: {
        unsetSearchMode() {
            this.$store.commit(`chooser/${UNSET_SEARCH_PATTERN}`);
            this.$store.commit(`chooser/${UNSET_SEARCH_MODE}`);
            this.$emit("back");
        }
    }
};
</script>

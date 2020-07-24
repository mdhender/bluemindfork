<template>
    <div class="h-100 bg-extra-light">
        <folder-result-content v-if="isResolved" />
        <folder-result-spinner v-if="isLoading" />
    </div>
</template>

<script>
import { mapGetters } from "vuex";
import FolderResultContent from "./FolderResultContent";
import FolderResultSpinner from "./FolderResultSpinner";

export default {
    name: "FolderResult",
    components: {
        FolderResultContent,
        FolderResultSpinner
    },
    computed: {
        ...mapGetters("mail-webapp", ["isResolved", "isLoading"])
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/variables";
</style>

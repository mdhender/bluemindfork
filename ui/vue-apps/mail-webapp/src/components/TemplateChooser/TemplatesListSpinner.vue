<template>
    <div class="templates-list-spinner">
        <bm-spinner />
    </div>
</template>

<script>
import { BmSpinner } from "@bluemind/ui-components";
export default {
    name: "TemplatesListSpinner",
    components: {
        BmSpinner
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/_variables.scss";

.templates-list-spinner {
    display: flex;
    justify-content: center;
    padding-top: $sp-8;

    .bm-spinner {
        flex: none;
    }
}
</style>

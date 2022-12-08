<template>
    <div class="chooser-header">
        <div class="header-top d-flex flex-fill mb-6">
            <chooser-breadcrumb :class="{ active: !HAS_VALID_PATTERN }" :path="path" class="mr-4" />
            <chooser-search :class="{ active: HAS_VALID_PATTERN }" />
        </div>

        <span v-if="!HAS_VALID_PATTERN" class="font-weight-bold">{{ currentDirectory }}</span>
    </div>
</template>

<script>
import { mapGetters, mapState } from "vuex";
import ChooserBreadcrumb from "./ChooserBreadcrumb/ChooserBreadcrumb";
import ChooserSearch from "./ChooserSearch/ChooserSearch";
import { HAS_VALID_PATTERN } from "../store/getters";

export default {
    name: "ChooserHeader",
    components: { ChooserBreadcrumb, ChooserSearch },

    computed: {
        ...mapState("chooser", ["path", "pattern", "isSearchMode"]),
        ...mapGetters("chooser", [HAS_VALID_PATTERN]),
        currentDirectory() {
            return this.path.split("/").filter(Boolean).pop();
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables";
@import "~@bluemind/ui-components/src/css/mixins/_responsiveness";

.chooser-header {
    width: 100%;
    background-color: $neutral-bg-lo1;
    padding-top: $sp-3;

    .header-top {
        max-width: 95%;
        @include until-lg {
            max-width: 100%;
        }
    }

    .chooser-breadcrumb {
        display: none;
        @include from-lg {
            &.active {
                display: flex;
                min-width: 75%;
            }
        }
    }
    .chooser-search {
        @include until-lg {
            display: none;
        }
    }
}
</style>

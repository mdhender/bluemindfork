<template>
    <div class="chooser-header px-3 pb-1">
        <div class="title mb-6">{{ $t("chooser.choose") }}</div>
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
@import "~@bluemind/styleguide/css/_variables";
@import "~@bluemind/styleguide/css/mixins/_responsiveness";

.chooser-header {
    width: 100%;
    .header-top {
        max-width: 95%;
        @include until-lg {
            max-width: 100%;
        }
    }
    .title {
        font-size: $font-size-lg;
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
}
</style>

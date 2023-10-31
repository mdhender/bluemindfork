<template>
    <bm-modal-header class="chooser-header" :title="$t('filehosting.chooser')" v-on="$listeners">
        <div class="d-flex flex-fill pl-4 mb-6">
            <chooser-breadcrumb :class="{ active: !HAS_VALID_PATTERN }" :path="path" class="mr-4" />
            <chooser-search :class="{ active: HAS_VALID_PATTERN }" />
        </div>
    </bm-modal-header>
</template>

<script>
import { mapGetters, mapState } from "vuex";
import { BmModalHeader } from "@bluemind/ui-components";
import ChooserBreadcrumb from "./ChooserBreadcrumb/ChooserBreadcrumb";
import ChooserSearch from "./ChooserSearch/ChooserSearch";
import { HAS_VALID_PATTERN } from "../store/getters";

export default {
    name: "ChooserHeader",
    components: { BmModalHeader, ChooserBreadcrumb, ChooserSearch },

    computed: {
        ...mapState("chooser", ["path", "pattern", "isSearchMode"]),
        ...mapGetters("chooser", [HAS_VALID_PATTERN])
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";
@import "~@bluemind/ui-components/src/css/utils/responsiveness";

.chooser-header {
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

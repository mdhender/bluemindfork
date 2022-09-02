<template>
    <chooser-files-table
        ref="search-result-table"
        class="search-result-files-table"
        :display-fields="fields"
        :items="items"
        :busy="loading"
    >
        <template v-if="error" #empty>
            <div class="text-center">
                {{ $t("common.search.error") }} <br />
                {{ $t("common.check.connection") }}
            </div>
        </template>
        <template v-else #empty>
            <search-no-results />
        </template>
    </chooser-files-table>
</template>

<script>
import { mapState } from "vuex";
import { inject } from "@bluemind/inject";
import ChooserMixin from "../mixins/ChooserMixin";
import ChooserFilesTable from "./ChooserFilesTable";
import SearchNoResults from "./SearchNoResults";

export default {
    name: "SearchResultFilesTable",
    components: { ChooserFilesTable, SearchNoResults },
    mixins: [ChooserMixin],
    data() {
        return {
            fields: ["selected", "name", "size", "path"]
        };
    },
    computed: {
        ...mapState("chooser", ["pattern"])
    },
    watch: {
        pattern: {
            async handler() {
                if (this.pattern && this.pattern.trim().length > 0) {
                    this.items = await this.getItems(this.searchItems);
                }
            },
            immediate: true
        }
    },
    methods: {
        async searchItems() {
            return inject("FileHostingPersistence").find(this.pattern);
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.search-result-files-table {
    .name-column {
        width: 40%;
    }
    .size-column {
        width: 15%;
    }
    .path-column {
        width: 40%;
    }
    .search-pattern {
        color: $primary-fg-hi1;
        font-weight: $font-weight-bold;
    }
}
</style>

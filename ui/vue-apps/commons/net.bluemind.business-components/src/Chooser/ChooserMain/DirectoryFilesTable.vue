<template>
    <chooser-files-table
        ref="directory-files-table"
        class="directory-files-table"
        :display-fields="fields"
        :items="items"
        :busy="loading"
    >
        <template v-if="error" #empty>
            <div class="text-center">
                {{ $t("common.check.connection") }}
            </div>
        </template>
        <template v-else #empty>
            <div class="text-center">
                <i18n path="common.folder.empty">
                    <template #folder>
                        <span class="empty-folder">{{ currentFolder }}</span>
                    </template>
                </i18n>
            </div>
        </template>
    </chooser-files-table>
</template>

<script>
import { mapState } from "vuex";
import { inject } from "@bluemind/inject";
import ChooserMixin from "../mixins/ChooserMixin";
import ChooserFilesTable from "./ChooserFilesTable";

export default {
    name: "DirectoryFilesTable",
    components: { ChooserFilesTable },
    mixins: [ChooserMixin],
    data() {
        return {
            fields: ["selected", "name", "size"]
        };
    },
    computed: {
        ...mapState("chooser", ["path"]),
        currentFolder() {
            return this.path.split("/").filter(Boolean).pop();
        }
    },
    watch: {
        path: {
            async handler() {
                this.items = await this.getItems(this.getItemsByPath);
            },
            immediate: true
        }
    },
    methods: {
        getItemsByPath() {
            return inject("FileHostingPersistence").list(this.path);
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.directory-files-table {
    .name-column {
        width: 55%;
    }
    .size-column {
        width: 45%;
    }

    .empty-folder {
        font-weight: $font-weight-bold;
    }
    .bm-icon.fa-folder-fill {
        color: $secondary-fg;
    }
}
</style>

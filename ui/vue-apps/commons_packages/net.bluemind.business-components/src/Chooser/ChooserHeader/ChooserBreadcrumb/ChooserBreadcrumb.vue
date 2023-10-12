<template>
    <div class="chooser-breadcrumb">
        <folder-up-button class="mr-4" @click.native="select(directories.length - 2)" />
        <bm-breadcrumb>
            <bm-breadcrumb-item class="home" icon="home" @click="select(0)" />
            <bm-breadcrumb-item
                v-for="(directory, index) in directories"
                :key="index"
                :active="index === directories.length - 1"
                :text="directory"
                @click="select(index + 1)"
            />
        </bm-breadcrumb>
    </div>
</template>

<script>
import { mapState } from "vuex";
import { BmBreadcrumb, BmBreadcrumbItem } from "@bluemind/ui-components";
import { RESET_PATH, SET_PATH } from "../../store/mutations";
import FolderUpButton from "./FolderUpButton";

export default {
    name: "ChooserBreadcrumb",
    components: { BmBreadcrumb, BmBreadcrumbItem, FolderUpButton },

    computed: {
        ...mapState("chooser", ["path", "rootPath"]),
        directories() {
            return this.path.split("/").filter(Boolean);
        }
    },
    methods: {
        select(index) {
            const isNotLastItem = index !== this.directories.length;
            if (index === 0) {
                this.$store.commit(`chooser/${RESET_PATH}`);
            } else if (isNotLastItem) {
                const folderPath = `/${this.directories.slice(0, index).join("/")}`;
                this.$store.commit(`chooser/${SET_PATH}`, folderPath);
            }
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.chooser-breadcrumb {
    .bm-breadcrumb {
        width: 100%;
        align-items: center;
        background-color: $surface;
        border: 1px solid $neutral-fg-lo1;
        padding-left: $sp-4;
    }
}
</style>

<template>
    <bm-button variant="outline" class="folder-up-button" icon="folder-up" @click="setPath" />
</template>

<script>
import { mapState } from "vuex";
import { BmButton } from "@bluemind/ui-components";
import { SET_PATH } from "../../store/mutations";

export default {
    name: "FolderUpButton",
    components: { BmButton },
    props: {
        disabled: {
            type: Boolean,
            default: false
        }
    },
    computed: {
        ...mapState("chooser", ["path", "rootPath"]),
        isRootPath() {
            return this.path === this.rootPath;
        },
        folderUpPath() {
            const hierarchy = this.path.split("/");
            return hierarchy.length > 2 ? hierarchy.slice(hierarchy.length - 2).join("/") : this.rootPath;
        },
        isDisabled() {
            return this.disabled || this.isRootPath;
        }
    },
    methods: {
        setPath() {
            this.$store.commit(`chooser/${SET_PATH}`, this.folderUpPath);
        }
    }
};
</script>
<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.folder-up-button.btn-outline {
    background-color: $surface-bg;
}
</style>

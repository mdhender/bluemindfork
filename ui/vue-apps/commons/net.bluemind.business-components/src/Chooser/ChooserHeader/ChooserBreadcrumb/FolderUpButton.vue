<template>
    <bm-button variant="outline" class="back-button" @click="setPath">
        <bm-icon icon="folder-up" />
    </bm-button>
</template>

<script>
import { mapState } from "vuex";
import { BmButton, BmIcon } from "@bluemind/ui-components";
import { SET_PATH } from "../../store/mutations";

export default {
    name: "FolderUpButton",
    components: { BmButton, BmIcon },
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

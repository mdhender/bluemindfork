import { MY_MAILBOX_KEY } from "~/getters";
import { SET_COLLAPSED_TREE } from "~/mutations";

export default {
    data() {
        return { collapsed: true };
    },
    computed: {
        collapsedTrees() {
            return this.$store.state.mail.folderList.collapsedTrees;
        }
    },
    watch: {
        collapsedTrees: {
            handler() {
                const index = this.collapsedTrees.findIndex(({ key }) => key === this.treeKey);
                if (index === -1) {
                    const areExpandedByDefault = [this.$store.getters["mail/" + MY_MAILBOX_KEY]];
                    this.collapsed = !areExpandedByDefault.includes(this.treeKey);
                } else {
                    this.collapsed = this.collapsedTrees[index].collapsed;
                }
            },
            immediate: true
        }
    },
    methods: {
        toggleTree() {
            this.collapsed = !this.collapsed;
            this.$store.commit("mail/" + SET_COLLAPSED_TREE, { key: this.treeKey, collapsed: this.collapsed });
        }
    }
};

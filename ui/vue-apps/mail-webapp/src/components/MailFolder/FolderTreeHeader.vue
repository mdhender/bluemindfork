<template>
    <div class="folder-tree-header">
        <mail-mailbox-icon :mailbox="mailbox" class="folder-tree-avatar" />
        <div class="folder-tree-name">{{ name }}</div>
    </div>
</template>

<script>
import MailMailboxIcon from "../MailMailboxIcon";

export default {
    name: "FolderTreeHeader",
    components: { MailMailboxIcon },
    props: {
        mailbox: {
            type: Object,
            required: true
        },
        name: {
            type: String,
            default: function () {
                return this.mailbox.name;
            }
        }
    }
};
</script>
<style lang="scss">
@import "@bluemind/ui-components/src/css/variables";
@import "@bluemind/ui-components/src/css/_type.scss";

.folder-tree-header {
    display: flex;
    align-items: center;
    gap: $sp-3;

    .folder-tree-avatar {
        flex: none;
    }
    .folder-tree-name {
        flex: 1;
        min-width: 0;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        color: $primary-fg-hi1;

        @extend %bold;
        letter-spacing: -0.04em;
    }
}
</style>

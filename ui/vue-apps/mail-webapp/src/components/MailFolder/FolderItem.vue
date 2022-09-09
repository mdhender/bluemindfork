<template>
    <bm-dropzone
        :states="{ active: false }"
        :accept="['conversation']"
        :value="folder"
        class="folder-item"
        :class="{ active: isActive, 'read-only': !folder.writable, 'show-menu-btn': showMenuBtn }"
    >
        <slot />
        <div v-if="!folder.writable" :title="$t('mail.folder.access.limited')" class="instead-of-menu">
            <bm-icon icon="info-circle" />
        </div>
        <div v-if="isUnread && !showMenuBtn" class="instead-of-menu">
            <bm-counter-badge
                :count="folder.unread"
                :active="isActive"
                :aria-label="$t('mail.folder.unread', { count: folder.unread })"
            />
        </div>
        <mail-folder-item-menu
            v-if="folder.writable"
            :folder="folder"
            class="mx-1"
            @edit="$emit('edit')"
            @create="$emit('create')"
            @shown="showMenuBtn = true"
            @hidden="showMenuBtn = false"
        />
    </bm-dropzone>
</template>

<script>
import { mapState } from "vuex";
import { BmCounterBadge, BmDropzone, BmIcon } from "@bluemind/styleguide";
import MailFolderItemMenu from "./MailFolderItemMenu";

export default {
    name: "FolderItem",
    components: {
        BmCounterBadge,
        BmDropzone,
        BmIcon,
        MailFolderItemMenu
    },
    props: {
        folder: {
            type: Object,
            required: true
        }
    },
    data() {
        return { showMenuBtn: false };
    },
    computed: {
        ...mapState("mail", ["activeFolder"]),
        isActive() {
            return this.folder.key === this.activeFolder;
        },
        isUnread() {
            return this.folder.unread > 0;
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/mixins/_responsiveness";
@import "~@bluemind/styleguide/css/_variables";

.folder-item {
    display: flex;
    min-width: 0;
    align-items: center;
    height: 100%;
    padding-bottom: $sp-3;

    border-bottom: 1px solid $neutral-fg-lo3;
    cursor: pointer;
    &:hover {
        background-color: $neutral-bg-lo1;
        color: $neutral-fg-hi1;
    }
    &.active {
        background-color: $secondary-bg-lo1;
        color: $neutral-fg-hi1;
        &:hover {
            background-color: $secondary-bg;
        }
    }

    .instead-of-menu,
    .mail-folder-item-menu {
        width: base-px-to-rem(28);
    }

    .instead-of-menu {
        display: flex;
        align-items: center;
        justify-content: center;
    }

    .mail-folder-item-menu {
        display: none !important;

        .dropdown-toggle {
            padding: 0;
        }
    }
}

@include from-lg {
    .bm-tree-node-content:focus .folder-item:not(.read-only),
    .bm-tree-node-content:focus-within .folder-item:not(.read-only),
    .folder-item:not(.read-only):hover,
    .folder-item:not(.read-only).show-menu-btn {
        .mail-folder-item-menu {
            display: flex !important;
        }
        .instead-of-menu {
            display: none !important;
        }
    }
}
</style>

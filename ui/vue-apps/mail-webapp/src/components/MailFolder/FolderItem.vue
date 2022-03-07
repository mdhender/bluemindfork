<template>
    <bm-dropzone
        :states="{ active: false }"
        :accept="['conversation']"
        :value="folder"
        :class="{ active: isActive }"
        class="folder-item align-items-center d-inline-flex border-bottom border-ligth pl-2"
    >
        <slot />
        <div v-if="!folder.writable" :title="$t('mail.folder.access.limited')" :class="isUnread ? 'pr-1' : 'pr-2'">
            <bm-icon icon="info-circle" />
        </div>
        <mail-folder-item-menu
            v-if="folder.writable"
            :folder="folder"
            class="mx-1"
            @edit="$emit('edit')"
            @create="$emit('create')"
            @shown="menuIsShown = true"
            @hidden="menuIsShown = false"
        />
        <bm-counter-badge
            v-if="isUnread && !menuIsShown"
            :value="folder.unread"
            :variant="isActive ? 'secondary' : 'primary'"
            class="mx-1 d-block"
            :class="{ 'read-only': !folder.writable }"
            :aria-label="$t('mail.folder.unread', { count: folder.unread })"
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
        return { menuIsShown: false };
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
@import "~@bluemind/styleguide/css/_variables";

.folder-item {
    cursor: pointer;
    &:hover {
        background-color: $extra-light;
        color: $dark;
    }
    &.active {
        background-color: $component-active-bg;
        color: $dark;
        &:hover {
            background-color: $component-active-bg-darken;
        }
    }
    .mail-folder-item-menu,
    .bm-counter-badge {
        min-width: 1.4rem;
    }

    .mail-folder-item-menu {
        visibility: hidden;

        .dropdown-toggle {
            padding: 0;
        }
    }
}

@include media-breakpoint-up(lg) {
    .folder-item:hover {
        .mail-folder-item-menu {
            display: flex !important;
            visibility: visible;
        }
        .bm-counter-badge:not(.read-only) {
            display: none !important;
        }
    }
}
</style>

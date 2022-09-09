<template>
    <div class="mail-folder-sidebar-header bg-surface">
        <folder-sidebar-mobile-header />
        <folder-search-input />
    </div>
</template>

<script>
import FolderSearchInput from "./FolderSearchInput";
import FolderSidebarMobileHeader from "./FolderSidebarMobileHeader";

export default {
    name: "MailFolderSidebarHeader",
    components: { FolderSearchInput, FolderSidebarMobileHeader }
};
</script>

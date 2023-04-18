<template>
    <bm-icon-button
        class="mail-search-advanced-button"
        :size="size"
        icon="settings"
        :variant="variant"
        @click="openAdvancedSearchModal"
    />
</template>

<script>
import { BmIconButton } from "@bluemind/ui-components";

export default {
    name: "MailSearchAdvancedButton",
    components: { BmIconButton },
    props: {
        variant: {
            type: String,
            default: "compact"
        },
        size: {
            type: String,
            default: "md"
        }
    },
    methods: {
        openAdvancedSearchModal() {
            this.$bvModal.show("advanced-search-modal");
        }
    }
};
</script>

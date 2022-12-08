<template>
    <bm-icon-dropdown
        no-caret
        variant="compact"
        size="sm"
        class="other-button"
        icon="3dots-v"
        :title="$t('mail.actions.other')"
    >
        <bm-extension id="webapp.mail" type="list" :file="file" path="file.actions" :message="message" />
    </bm-icon-dropdown>
</template>

<script>
import { BmIconDropdown } from "@bluemind/ui-components";
import { BmExtension } from "@bluemind/extensions.vue";

export default {
    name: "OtherButton",
    components: { BmIconDropdown, BmExtension },
    props: {
        file: {
            type: Object,
            required: true
        },
        message: {
            type: Object,
            required: true
        }
    }
};
</script>

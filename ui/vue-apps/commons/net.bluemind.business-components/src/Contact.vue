<template>
    <bm-extension
        v-if="extension"
        id="webapp"
        v-slot="context"
        :path="`contact.chip.${extension}`"
        type="renderless"
        v-bind="{ ...$attrs }"
    >
        <contact-internal v-bind="context" v-on="$listeners">
            <template #email="slotProps">
                <slot name="email" :email="slotProps.email" />
            </template>
            <template #actions="slotProps">
                <slot name="actions" :contact="slotProps.contact" />
            </template>
        </contact-internal>
    </bm-extension>
    <contact-internal v-else v-bind="{ ...$attrs }" v-on="$listeners">
        <template #email="slotProps">
            <slot name="email" :email="slotProps.email" />
        </template>
        <template #actions="slotProps">
            <slot name="actions" :contact="slotProps.contact" />
        </template>
    </contact-internal>
</template>

<script>
import { BmExtension } from "@bluemind/extensions.vue";
import ContactInternal from "./ContactInternal";

export default {
    name: "Contact",
    components: { ContactInternal, BmExtension },
    props: {
        extension: { type: String, default: undefined }
    }
};
</script>

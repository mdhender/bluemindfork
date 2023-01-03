<template>
    <bm-extension
        v-if="extension"
        id="webapp"
        v-slot="context"
        :path="`contact.chip.${extension}`"
        type="renderless"
        v-bind="{ ...$attrs }"
    >
        <bm-contact-internal v-bind="context" v-on="$listeners" />
    </bm-extension>
    <bm-contact-internal v-else v-bind="{ ...$attrs }" v-on="$listeners" />
</template>

<script>
import { BmExtension } from "@bluemind/extensions.vue";
import BmContactInternal from "./BmContactInternal";

export default {
    name: "BmContact",
    components: { BmContactInternal, BmExtension },
    props: {
        extension: {
            type: String,
            default: undefined
        }
    }
};
</script>

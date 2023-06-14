<template>
    <div>
        <bm-button v-key-nav-group:recipient-button variant="text-accent" @click="$bvModal.show('recipient-picker')">
            {{ recipientType }}
        </bm-button>
    </div>
</template>

<script>
import { BmButton, KeyNavGroup } from "@bluemind/ui-components";

export default {
    components: { BmButton },
    directives: { KeyNavGroup },
    props: {
        recipientType: { type: String, required: true }
    }
};
</script>

<template>
    <bm-toolbar-icon-button
        v-if="!fhFile && !isReadOnly"
        class="detach-item"
        :disabled="isToolarge"
        icon="cloud-arrow-up"
        @click.stop="detach"
    >
        {{ $t("filehosting.detach") }}
    </bm-toolbar-icon-button>
</template>

<script>
import { BmToolbarIconButton } from "@bluemind/ui-components";
import { DETACH_ATTACHMENT } from "~/store/types/actions";
import OtherActionsMixin from "~/mixins/OtherActionsMixin";

export default {
    name: "DetachItem",
    components: { BmToolbarIconButton },
    mixins: [OtherActionsMixin],
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    methods: {
        async detach() {
            await this.$store.dispatch(`mail/${DETACH_ATTACHMENT}`, {
                file: this.file,
                message: this.message,
                vm: this
            });
        }
    }
};
</script>

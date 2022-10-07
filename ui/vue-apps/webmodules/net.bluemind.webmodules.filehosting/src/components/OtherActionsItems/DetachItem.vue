<template>
    <bm-dropdown-item v-if="!fhFile" class="detach-item" :disabled="isToolarge" icon="cloud-up" @click.stop="detach">
        {{ $t("filehosting.detach") }}
    </bm-dropdown-item>
</template>

<script>
import { BmDropdownItem } from "@bluemind/styleguide";
import { DETACH_ATTACHMENT } from "~/store/types/actions";
import OtherActionsMixin from "~/mixins/OtherActionsMixin";

export default {
    name: "DetachItem",
    components: { BmDropdownItem },
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

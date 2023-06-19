<template>
    <div class="mail-composer-recipient-button">
        <bm-button v-key-nav-group:recipient-button variant="text-accent" class="mx-3" @click="show = true">
            {{ recipientType }}
        </bm-button>
        <bm-modal id="recipient-picker" v-model="show" centered hide-footer />
    </div>
</template>

<script>
import { BmButton, BmModal, KeyNavGroup } from "@bluemind/ui-components";

export default {
    components: {
        BmButton,
        BmModal
    },
    directives: { KeyNavGroup },
    props: {
        recipientType: {
            type: String,
            required: true
        }
    },
    data() {
        return {
            show: false
        };
    }
};
</script>

<style lang="scss">
.mail-composer-recipient-button {
    flex: none;
}
</style>

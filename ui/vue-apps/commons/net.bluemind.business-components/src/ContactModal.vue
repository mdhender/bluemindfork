<template>
    <bm-modal :id="uniqueId" centered hide-footer hide-header @hidden="$emit('update:show', false)">
        <contact-card v-focus-out :contact="contact" @focusout="$emit('update:show', false)">
            <template #email="slotProps">
                <slot name="email" :email="slotProps.email" />
            </template>
            <template #actions="slotProps">
                <slot name="actions" :contact="slotProps.contact" />
            </template>
        </contact-card>
    </bm-modal>
</template>

<script>
import { BmModal } from "@bluemind/ui-components";
import ContactCard from "./ContactCard";
import FocusOut from "./FocusOut";

export default {
    name: "ContactModal",
    components: { BmModal, ContactCard },
    directives: { FocusOut },
    props: {
        contact: { type: Object, default: undefined },
        show: { type: Boolean, default: false }
    },
    computed: {
        uniqueId() {
            return `contact-card-modal-${this._uid}`;
        }
    },
    watch: {
        show(value) {
            value ? this.$bvModal.show(this.uniqueId) : this.$bvModal.hide(this.uniqueId);
        }
    }
};
</script>

<style lang="scss">
@import "@bluemind/ui-components/src/css/utils/responsiveness";
@import "@bluemind/ui-components/src/css/utils/typography";
@import "@bluemind/ui-components/src/css/utils/variables";

[id^="contact-card-modal-"] {
    @include from-lg {
        display: none;
    }

    .modal-body {
        padding: $sp-5;
        .contact-card-body {
            li {
                padding-bottom: $sp-4;
            }
        }
        div:focus,
        .contact-card-body a:focus {
            outline-width: 1px !important;
            outline-style: dashed !important;
            outline-color: var(--neutral-fg);
        }
        .bm-icon-button-copy {
            display: none !important;
        }
    }
}
</style>

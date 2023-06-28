<template>
    <div class="bm-modal-header">
        <bm-navbar class="modal-navbar-header">
            <bm-navbar-back @click="$emit('close')" />
            <bm-navbar-title :title="title" />
            <slot />
        </bm-navbar>
        <div class="title-and-close">
            <p class="modal-title title default-title">{{ title }}</p>
            <bm-button-close class="modal-close" size="lg" @click="$emit('close')" />
        </div>
        <div class="additional-content">
            <slot />
        </div>
    </div>
</template>

<script>
import BmButtonClose from "../buttons/BmButtonClose";
import BmNavbar from "../navbar/BmNavbar";
import BmNavbarBack from "../navbar/BmNavbarBack";
import BmNavbarTitle from "../navbar/BmNavbarTitle";

export default {
    name: "BmModalHeader",
    components: { BmButtonClose, BmNavbar, BmNavbarBack, BmNavbarTitle },
    props: {
        title: {
            type: String,
            required: true
        }
    }
};
</script>

<style lang="scss">
@import "../../css/utils/responsiveness";
@import "../../css/utils/variables";

.bm-modal-header {
    width: 100%;

    .title-and-close {
        display: flex;
        gap: $sp-5;

        padding: base-px-to-rem(14) base-px-to-rem(12) base-px-to-rem(14) base-px-to-rem(20);
        @include from-lg {
            padding: base-px-to-rem(20) base-px-to-rem(14) base-px-to-rem(20) base-px-to-rem(30);
        }

        .modal-title {
            flex: 1;
        }
    }

    .additional-content {
        padding: 0 $sp-6;
    }
}

// Manage full-screen modals with navbar header on mobile:
.bm-modal-header .modal-navbar-header {
    display: none;
}

@include until-lg {
    .modal-dialog:not(.modal-sm) {
        .bm-modal-header {
            padding-bottom: 0;
            .modal-navbar-header {
                display: flex;
                flex: 1;
                min-width: 0;
            }
            .title-and-close,
            .additional-content {
                display: none;
            }
        }
    }
}
</style>

<template>
    <div class="bm-alert-group">
        <transition-group v-if="alerts.length <= maxSize || !stackable" name="grow-y">
            <template>
                <bm-alert v-for="alert in _alerts" :key="alert.uid" :variant="variant(alert)" show class="bm-alert">
                    <bm-alert-icon :alert="alert" />
                    <slot :alert="alert" />
                    <span v-if="alert.dismissible" class="ml-auto pl-5">
                        <bm-button-close @click="$emit('remove', alert)" />
                    </span>
                </bm-alert>
            </template>
        </transition-group>
        <bm-alert-stack v-else-if="stackable" :alerts="alerts" @remove="alerts => $emit('remove', alerts)">
            <template v-slot="context">
                <slot :alert="context.alert">{{ context.alert.message }}</slot>
            </template>
        </bm-alert-stack>
    </div>
</template>

<script>
import AlertMixin from "./mixin";
import BmAlert from "../BmAlert";
import BmAlertIcon from "./BmAlertIcon";
import BmAlertStack from "./BmAlertStack";
import BmButtonClose from "../buttons/BmButtonClose";

export default {
    name: "BmAlertGroup",

    components: {
        BmAlert,
        BmAlertIcon,
        BmAlertStack,
        BmButtonClose
    },
    mixins: [AlertMixin],
    props: {
        alerts: {
            type: Array,
            required: true
        },
        stackable: {
            type: Boolean,
            default: true
        },
        maxSize: {
            type: Number,
            default: 2
        }
    },
    computed: {
        _alerts() {
            return this.alerts.slice(-this.maxSize);
        }
    }
};
</script>
<style lang="scss">
@import "../../css/_variables.scss";
.bm-alert-group {
    position: relative;

    .bm-alert {
        padding: $sp-5 + $sp-2 $sp-5 + $sp-3 + $sp-2;
        color: $neutral-fg-hi1 !important;
        display: flex !important;
        align-items: center !important;
        border: none;
        transition: all 0.25s linear;
    }

    .bm-alert-icon {
        margin-right: $sp-5 + $sp-3 + $sp-2;
    }

    .grow-y-leave-active {
        position: absolute;
        width: max-content;
        min-width: 100%;
    }
    .grow-y-enter,
    .grow-y-leave-to {
        opacity: 0;
        transform: scaleY(0);
    }
}
</style>

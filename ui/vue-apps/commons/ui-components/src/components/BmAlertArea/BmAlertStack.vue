<template>
    <bm-alert show :variant="variant({ type })" class="px-0 bm-alert-stack" :class="{ collapsed: !expanded }">
        <div class="bm-alert py-0 d-flex align-items-center align-self-stretch">
            <bm-alert-icon :alert="{ type }" class="" />
            {{ $tc("styleguide.alert.alerts", alerts.length, { n: alerts.length }) }}
            <bm-button v-if="expanded" variant="text" class="ml-6" @click="expanded = false">
                {{ $t("styleguide.alert.hide") }}
            </bm-button>
            <bm-button v-else variant="text" class="ml-6" @click="expanded = true">
                {{ $t("styleguide.alert.show") }}
            </bm-button>
            <span v-if="isDismissible" class="ml-auto pl-3">
                <bm-button-close @click="$emit('remove', dismissibles)" />
            </span>
        </div>
        <div v-show="expanded" class="px-6 scroller-y w-100">
            <hr />
            <div v-for="alert in alerts" :key="alert.uid">
                <slot :alert="alert" />
                <hr />
            </div>
        </div>
        <hr v-show="!expanded" />
        <hr v-show="!expanded" />
    </bm-alert>
</template>

<script>
import AlertMixin from "./mixin";
import BmAlert from "../BmAlert";
import BmButtonClose from "../buttons/BmButtonClose";
import BmButton from "../buttons/BmButton";
import BmAlertIcon from "./BmAlertIcon";

export default {
    name: "BmAlertStack",
    components: {
        BmAlert,
        BmButtonClose,
        BmButton,
        BmAlertIcon
    },
    mixins: [AlertMixin],
    props: {
        alerts: {
            type: Array,
            default: () => []
        }
    },
    data() {
        return {
            expanded: false
        };
    },
    computed: {
        type() {
            return (this.alerts[0] && this.alerts[0].type) || "";
        },
        dismissibles() {
            return this.alerts.filter(({ dismissible }) => dismissible);
        },
        isDismissible() {
            return this.dismissibles.length > 0;
        }
    },
    methods: {
        removeDissmissbles() {}
    }
};
</script>

<style lang="scss">
@import "../../css/_variables.scss";

.bm-alert-stack {
    &.alert {
        flex-direction: column !important;
    }
    hr {
        border-top-color: $lowest !important;
        opacity: 1;
        margin-top: $sp-5;
        margin-bottom: $sp-5;
    }

    &.collapsed hr {
        border-width: 5px;
        margin-top: 5px;
        margin-bottom: 0px;
        position: relative;
        top: 5px;
        width: 100%;
    }

    .scroller-y {
        max-height: 10rem;
    }
}
</style>

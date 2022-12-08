<template>
    <div class="bm-alert-area flex-column" :class="floating ? 'd-flex z-index-250 position-absolute' : 'd-inline-flex'">
        <transition-group>
            <bm-alert-group
                v-for="(_alerts, type) in byType"
                :key="type + ''"
                :alerts="_alerts"
                :stackable="isStackable(type)"
                :max-size="size(type)"
                @remove="alerts => $emit('remove', alerts)"
            >
                <template v-slot="context">
                    <slot :alert="context.alert">{{ context.alert.message }}</slot>
                </template>
            </bm-alert-group>
        </transition-group>
    </div>
</template>

<script>
import { AlertTypes } from "@bluemind/alert.store";
import BmAlertGroup from "./BmAlertGroup";

export default {
    name: "BmAlertArea",
    components: {
        BmAlertGroup
    },
    props: {
        alerts: {
            type: Array,
            default: () => []
        },
        floating: {
            type: Boolean,
            default: false
        }
    },
    computed: {
        byType() {
            return Object.values(AlertTypes).reduce(
                (byType, type) => ({ ...byType, [type]: this.alerts.filter(alert => alert.type === type) }),
                {}
            );
        }
    },
    methods: {
        isStackable(type) {
            switch (type) {
                case AlertTypes.ERROR:
                case AlertTypes.WARNING:
                    return true;
                default:
                    return false;
            }
        },
        size(type) {
            switch (type) {
                case AlertTypes.WARNING:
                case AlertTypes.ERROR:
                    return 1;
                case AlertTypes.SUCCESS:
                    return 1;
                default:
                    return 5;
            }
        }
    }
};
</script>

<style lang="scss">
@import "../../css/_variables.scss";

.bm-alert-area {
    margin-bottom: 0;

    .alert {
        margin-bottom: $sp-1;
    }

    &.position-absolute {
        .alert {
            margin-top: $sp-2;
            margin-bottom: 0;
        }

        .bm-alert-group {
            transition: top, bottom 0.25s linear;
        }
    }
}
</style>

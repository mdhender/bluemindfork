<script>
import { mapMutations } from "vuex";
import { RESET_ACTIVE_MESSAGE, UNSET_CURRENT_CONVERSATION } from "~/mutations";

export default {
    name: "ChainOfResponsibility",
    props: {
        isResponsible: {
            type: Boolean,
            required: true
        }
    },
    created() {
        this.RESET_ACTIVE_MESSAGE();
        this.UNSET_CURRENT_CONVERSATION();
    },
    methods: {
        ...mapMutations("mail", { RESET_ACTIVE_MESSAGE, UNSET_CURRENT_CONVERSATION })
    },
    render() {
        if (this.isResponsible) {
            return this.$scopedSlots.default();
        }
        return this.$parent.$scopedSlots.default();
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/mixins/responsiveness";
@import "~@bluemind/ui-components/src/css/type";
@import "~@bluemind/ui-components/src/css/variables";

.mail-home-screen {
    display: flex;
    @include until-lg {
        display: none;
    }

    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: max-content;
    min-height: 100%;
    width: 100%;
    padding: $sp-6 0 $sp-5 $scroll-width;

    .starter-text-and-actions {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: $sp-7;

        .starter-text {
            display: flex;
            flex-direction: column;
            align-items: center;
            text-align: center;
            gap: $sp-7;
            margin-bottom: $sp-3;

            @extend %regular-high;

            h1 {
                margin: 0;
            }

            .description {
                width: 60%;
                min-width: base-px-to-rem(240);
                color: $neutral-fg;
            }
        }

        &.compact .starter-text {
            gap: $sp-5;
            margin-bottom: 0;
        }

        .description {
            width: 60%;
            min-width: base-px-to-rem(240);
            min-height: base-px-to-rem(100);
            display: flex;
            align-items: center;
            color: $neutral-fg;
        }
    }

    .starter-links {
        display: flex;
        flex-wrap: wrap;
        justify-content: center;
        gap: $sp-5 $sp-7;
        margin: $sp-5 0;

        .starter-link {
            display: flex;
            gap: $sp-4;
            .bm-icon {
                color: $neutral-fg;
            }
        }
    }
}
</style>

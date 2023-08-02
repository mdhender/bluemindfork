<template>
    <section class="anonymous-screen" aria-labelledby="text-1">
        <div class="anonymous-screen-content">
            <h2 id="text-1">{{ $t("root.anonymous_screen.not_connected") }}</h2>
            <div class="anonymous-screen-body">
                <bm-illustration value="user-question" size-lg="lg" over-background :over-background-lg="false" />
                <div class="continue-as-user-or-guest">
                    <div class="continue_as_user">
                        <i18n path="root.anonymous_screen.continue_as_user" tag="span">
                            <template #domain>
                                <br /><strong>{{ domain }}</strong>
                            </template>
                        </i18n>
                        <br />
                        <br />
                        <bm-icon icon="chevron-right" />
                        <bm-button variant="text-accent" @click="$emit('login')">{{ $t("common.login") }}</bm-button>
                    </div>
                    <bm-button variant="text" class="continue-as-guest" @click="$emit('continue')">{{
                        $t("root.anonymous_screen.continue_as_guest")
                    }}</bm-button>
                </div>
            </div>
        </div>
    </section>
</template>

<script>
import { BmButton, BmIcon, BmIllustration } from "@bluemind/ui-components";

export default {
    name: "AnonymousScreen",
    components: { BmButton, BmIcon, BmIllustration },
    computed: {
        domain() {
            return new URL(window.location).hostname;
        }
    }
};
</script>
<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";
@import "~@bluemind/ui-components/src/css/utils/responsiveness";

.anonymous-screen {
    flex: 1 1 auto;
    align-items: center;
    justify-content: center;
    display: flex;
    flex-direction: column;
    padding: $sp-6 0;
    .anonymous-screen-content {
        display: flex;
        text-align: center;
        flex-direction: column;
        justify-content: space-around;
        flex: 1 1 auto;
        width: 100%;
        padding: 0 $sp-6;
        @include from-lg {
            background: $surface;
            max-height: 600px;
            width: map-get($grid-breakpoints, "lg") - 1px;
        }
        .anonymous-screen-body {
            display: flex;
            @include until-lg {
                flex-direction: column;
            }
            align-items: center;
            justify-content: space-around;
            .continue-as-user-or-guest {
                max-width: 300px;
                .continue_as_user {
                    background: $surface;
                    @include from-lg {
                        background: $backdrop;
                    }
                    padding: $sp-6;
                    margin-bottom: $sp-7;
                    .bm-icon {
                        vertical-align: middle;
                    }
                }
            }
        }
    }
}
</style>

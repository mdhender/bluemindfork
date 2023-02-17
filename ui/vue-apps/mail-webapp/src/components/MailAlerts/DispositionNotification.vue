<template>
    <div class="disposition-notification d-flex flex-fill justify-content-between">
        <div>
            <i18n path="alert.mail.disposition_notification.info" :title="EmailExtractor.extractEmail(payload.to)">
                <template #sender>
                    <strong class="text-neutral">{{
                        EmailExtractor.extractDN(payload.to) || EmailExtractor.extractEmail(payload.to)
                    }}</strong>
                </template>
            </i18n>
            <br />
            <span>{{ $t("alert.mail.disposition_notification.info.action") }}</span>
            <router-link
                class="d-none d-lg-inline-block"
                :to="`${$route.path}${prefPath}`"
                @click.native="$store.commit('preferences/TOGGLE_PREFERENCES')"
            >
                {{ $t("alert.mail.disposition_notification.info.pref") }}
            </router-link>
        </div>
        <div class="d-flex align-items-center justify-content-around">
            <bm-button variant="text" class="mx-4">{{ $t("common.send") }}</bm-button>
            <bm-button variant="text" class="mx-4">{{ $t("common.ignore") }}</bm-button>
            <bm-icon-button
                class="d-lg-none"
                icon="preferences"
                variant="compact"
                @click.native="
                    $router.push(`${$route.path}${prefPath}`);
                    $store.commit('preferences/TOGGLE_PREFERENCES');
                "
            />
        </div>
    </div>
</template>
<script>
import { AlertMixin } from "@bluemind/alert.store";
import { EmailExtractor } from "@bluemind/email";
import { BmButton, BmIconButton } from "@bluemind/ui-components";

export default {
    name: "DispositionNotification",
    components: { BmButton, BmIconButton },
    mixins: [AlertMixin],
    data() {
        return { EmailExtractor, prefPath: "#preferences-mail-main" };
    }
};
</script>

<style lang="scss">
@import "@bluemind/ui-components/src/css/mixins/responsiveness";

.disposition-notification {
    @include until-lg {
        flex-direction: column;
    }
}
</style>

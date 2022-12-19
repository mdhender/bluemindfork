<template>
    <bm-button
        class="contact-action-show"
        variant="text-accent"
        :href="link"
        target="_blank"
        :title="$t('contact.card.show_contact.tooltip')"
    >
        {{ $t("contact.card.show_contact") }}
    </bm-button>
</template>

<script>
import { mapExtensions } from "@bluemind/extensions";
import { inject } from "@bluemind/inject";
import { BmButton } from "@bluemind/ui-components";

export default {
    name: "ContactActionShow",
    components: { BmButton },
    props: {
        contact: { type: Object, default: undefined }
    },
    computed: {
        link() {
            const { protocol, hostname } = new URL(document.URL);
            const { uid, containerUid } = this.contact;
            return `${protocol}//${hostname}${this.contactApp.href}#/individual/consult/?uid=${uid}&container=${containerUid}`;
        },
        contactApp() {
            const session = inject("UserSession");
            const applications = mapExtensions("webapp.banner", { apps: "application" }).apps.filter(({ role }) =>
                session.roles.includes(role)
            );
            return applications.find(a => a.$id === "net.bluemind.webmodules.contact" && a.$loaded.status);
        }
    }
};
</script>

<template>
    <div class="contact-card-footer ml-3">
        <bm-button variant="text-accent" :href="link" target="_blank" :title="$t('contact.card.show_contact.tooltip')">
            {{ $t("contact.card.show_contact") }}
        </bm-button>
    </div>
</template>

<script>
import { mapExtensions } from "@bluemind/extensions";
import { inject } from "@bluemind/inject";
import { BmButton } from "@bluemind/styleguide";

export default {
    name: "ContactCardFooter",
    components: { BmButton },
    props: {
        contact: { type: Object, required: true }
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

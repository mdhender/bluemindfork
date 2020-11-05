<template>
    <default-alert v-if="alert.type === AlertTypes.LOADING" :alert="alert" :options="{ subject }" />
    <default-alert v-else-if="alert.type === AlertTypes.ERROR" :alert="alert" :options="{ subject }" />
    <i18n v-else-if="alert.type === AlertTypes.SUCCESS" :path="path" tag="span">
        <template #subject>
            <router-link :to="link">{{ subject }}</router-link>
        </template>
    </i18n>
</template>
<script>
import DefaultAlert from "./DefaultAlert";
import { AlertTypes } from "@bluemind/alert.store";
import { mapState } from "vuex";
import ItemUri from "@bluemind/item-uri";

export default {
    name: "SendMessage",
    components: { DefaultAlert },
    props: {
        alert: {
            type: Object,
            required: true
        }
    },
    data() {
        return { AlertTypes };
    },
    computed: {
        ...mapState("mail", ["messages"]),
        subject() {
            return this.messages[this.alert.payload.draftKey].subject;
        },
        link() {
            //FIXME : Dans l'idée :
            // - Soit send devrait renvoyer un message déjà formatté (pas de raison de gérer des données au format server);
            // - Soit send devrait renvoyer uniquement la clé du message
            // - Soit (et ça me parait limite le plus coherent) on devrait juste afficher un lien vers le dossier Sent...
            console.log(this.alert);
            const message = ItemUri.encode(this.alert.result.internalId, this.alert.payload.sentFolder.remoteRef.uid);
            return {
                name: "v:mail:message",
                params: { message, folder: this.alert.payload.sentFolder.path }
            };
        },
        path() {
            const { name, type } = this.alert;
            return "alert." + name.toLowerCase() + "." + type.toLowerCase();
        }
    }
};
</script>

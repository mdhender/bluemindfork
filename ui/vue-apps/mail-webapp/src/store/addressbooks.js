import { AddressbookAdaptor } from "@bluemind/contact";

import { SET_MY_CONTACTS_ADDRESSBOOK } from "~mutations";

export default {
    mutations: {
        [SET_MY_CONTACTS_ADDRESSBOOK]: (state, { subscriptions, userUid }) => {
            const remoteAddressbook = subscriptions.find(
                subscription =>
                    subscription.value.containerType === "addressbook" &&
                    subscription.value.containerUid === "book:Contacts_" + userUid
            );
            state.myContacts = AddressbookAdaptor.fromContainerSubscriptionModel(remoteAddressbook.value);
        }
    },

    state: {
        myContacts: {}
    }
};

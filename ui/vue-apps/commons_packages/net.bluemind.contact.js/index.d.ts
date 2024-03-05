import VCardAdaptor from "./VCardAdaptor";

declare module "@bluemind/contact" {
    type Contact = {
        uid: string | undefined;
        address: string | undefined;
        dn: string | undefined;
        kind: VCard.Kind | undefined;
        photo: boolean;
        urn: string;
        isInternal: boolean;
        members: Partial<VCard.Organizational.Member>[];
        memberCount?: number;
    };

    type Field =
        | "value.identification.formatedName.value"
        | "value.communications.emails.value"
        | "value.organizational.org.company";

    type SearchOption = {
        size?: number;
        noGroup?: boolean;
        addressBook?: string;
        fields?: Field[];
        from?: number;
    };

    function searchVCardsHelper(pattern: string, options?: SearchOption);
}

export { VCardAdaptor };

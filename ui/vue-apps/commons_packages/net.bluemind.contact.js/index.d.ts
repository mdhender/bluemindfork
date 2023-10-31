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
    function searchVCardsHelper(pattern: string, size = 5, noGroup = false);
}

export { VCardAdaptor };

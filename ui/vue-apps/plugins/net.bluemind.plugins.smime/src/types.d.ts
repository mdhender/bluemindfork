export type CheckOptions = {
    date?: Date;
    expectedAddress?: string;
    smimeUsage?: SMIME_CERT_USAGE;
};
export type DecryptResult = {
    item: ItemValue<MailboxItem>;
    content: string;
};

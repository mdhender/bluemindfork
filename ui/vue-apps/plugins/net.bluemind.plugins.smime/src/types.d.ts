export type CheckOptions = {
    date?: Date;
    expectedAddress?: string;
    smimeUsage?: SMIME_CERT_USAGE;
};
export type DecryptResult = {
    body: MessageBody;
    content: string;
};

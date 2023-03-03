import { MessageBody } from "@bluemind/backend.mail.api";

export default interface Visitor {
    results: MessageBody.Part[];
    visit(part: MessageBody.Part, ancestors?: MessageBody.Part[]): void;
    result(): MessageBody.Part[];
}

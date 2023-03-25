import { MessageBody } from "@bluemind/backend.mail.api";
import { fetchRequest, dispatchFetch } from "@bluemind/service-worker-utils";
import session from "../../environnment/session";

export default function (imapUid: number, folderUid: string) {
    return async (p: MessageBody.Part): Promise<string | Uint8Array> => {
        const sid = await session.sid;
        const request = fetchRequest(sid, folderUid, imapUid, p.address!, p.encoding!, p.mime!, p.charset!, p.fileName);
        const data = await dispatchFetch(request);
        return new Uint8Array(await data.arrayBuffer());
    };
}

import { PartKey } from "../PartKey";

export function getPartContent(state) {
    return (messageKey, address) => state.partContents[PartKey.encode(address, messageKey)];
}

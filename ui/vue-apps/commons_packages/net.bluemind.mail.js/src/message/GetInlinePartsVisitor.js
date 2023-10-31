import findLast from "lodash.findlast";

import { MimeType } from "@bluemind/email";
import { isAttachment } from "../attachment";

/**
 * Pass this and a body.structure to TreeWalker to build an array of maps of inline parts keyed by capabilities.
 * For each possible display possibilities due to multipart/alternative parts, there is an entry in the result.
 * When there is only one possibility then 'capabilities' is kept empty.
 *
 * @example // having this structure:
 *          //
 *          // multipart/alternative
 *          //  |
 *          //  ---text/plain
 *          //  |
 *          //  ---multipart/related
 *          //      |
 *          //      ---text/html
 *          //      |
 *          //      ---image/png
 *          //
 *          const visitor = new GetInlinePartsVisitor();
 *          const walker = new TreeWalker(rootPart, visitor);
 *          walker.walk();
 *          return visitor.result(); // => [ { "capabilities" : ["text/plain"], "parts" : [ <MyPlainPartObject> ] },
 *                                   //      { "capabilities" : ["text/html"],
 *                                   //        "parts" : [ <MyHtmlPartObject>, <MyImagePartObject> ] } ]
 *
 * @example // just one possibility:
 *          //
 *          // multipart/mixed
 *          //  |
 *          //  ---text/html
 *          //  |
 *          //  ---text/plain
 *          //  |
 *          //  ---image/png
 *          //
 *          const visitor = new GetInlinePartsVisitor();
 *          const walker = new TreeWalker(rootPart, visitor);
 *          walker.walk();
 *          return visitor.result(); // => [ { "capabilities" : [], "parts" : [
 *                                   //            <MyHtmlPartObject>, <MyPlainPartObject, <MyImagePartObject> ] } ]
 * @see TreeWalker
 */
export default class GetInlinePartsVisitor {
    constructor() {
        this.results = [{ parts: [], capabilities: [], lastForkAddress: "" }];
    }

    visit(part, ancestors) {
        this.root = ancestors[0] || part;
        if (this.isFork(part)) {
            this.forkResults(part, ancestors);
        } else if (this.isLeaf(part) && this.isInline(part)) {
            this.add(part, ancestors);
        }
    }

    /** Split the results in function of the given Alternative part. */
    forkResults(part, ancestors) {
        const root = this.getBranchRoot(part, ancestors);

        this.getChildrenResults(root).forEach(sibling => {
            let results = part.children.sort(this.isPreferedPart).map(part => {
                return {
                    lastForkAddress: part.address || "",
                    capabilities: sibling.capabilities.slice(0),
                    parts: sibling.parts.slice(0)
                };
            });
            this.results.splice(this.results.indexOf(sibling), 1, ...results);
        });
    }

    isPreferedPart(part1, part2) {
        const preferedHeader = "X-BM-Prefered-Part";
        if (part1.headers?.some(({ name }) => name === preferedHeader)) {
            return 1;
        }
        if (part2.headers?.some(({ name }) => name === preferedHeader)) {
            return -1;
        }
        return 0;
    }

    /** @return the closest ancestor which is an Alternative child */
    getBranchRoot(part, ancestors) {
        let child = part;
        for (let i = ancestors.length - 1; i >= 0; i--) {
            if (MimeType.isAlternative(ancestors[i])) {
                return child;
            }
            child = ancestors[i];
        }
        return child;
    }

    /** @return the already computed results of the given Alternative part */
    getChildrenResults(part) {
        const address = part.address === this.root.address ? "" : part.address;
        return this.results.filter(result => result.lastForkAddress.startsWith(address));
    }

    getPartNeededCapability(part, ancestors) {
        let child = part;
        for (let i = ancestors.length - 1; i >= 0; i--) {
            if (MimeType.isAlternative(ancestors[i])) {
                return part.mime;
            }
            if (MimeType.isRelated(ancestors[i]) && this.isNotFirstChild(child, ancestors[i])) {
                // we do not want to add the capability of a non-first Related child
                return null;
            }
            child = ancestors[i];
        }
        return null;
    }

    /** Build and add a result entry. */
    add(part, ancestors) {
        const capability = this.getPartNeededCapability(part, ancestors);
        const root = this.getBranchRoot(part, ancestors);
        this.getChildrenResults(root).forEach(result => {
            result.parts.push(part);
            if (capability !== null && !result.capabilities.includes(capability)) {
                result.capabilities.push(capability);
            }
        });
    }
    isFork(part) {
        return MimeType.isAlternative(part) && part.children && part.children.length > 0;
    }

    isInline(part) {
        return !isAttachment(part);
    }

    isLeaf(part) {
        return (!part.children || part.children.length === 0) && !MimeType.isMultipart(part);
    }

    isNotFirstChild(child, parent) {
        return (
            !!parent && !!parent.children && parent.children.length > 0 && parent.children[0].address !== child.address
        );
    }

    /** @return the sorted results */
    result() {
        return this.results
            .filter(result => {
                const prior = findLast(this.results, reverse =>
                    reverse.capabilities.every(capability => result.capabilities.includes(capability))
                );
                return result === prior;
            })
            .map(result => {
                return {
                    capabilities: result.capabilities,
                    parts: result.parts
                };
            })
            .reverse();
    }
}

type MatchOptions = {
    /**  Enables case sensitivity */
    caseSensitive?: boolean;
    /**  Enables strict accents mode, a letter with accent will only match the same letter and accent */
    accentsSensitive?: boolean;
    /**  The target must start with the pattern. By default, the target only need to include the pattern to match */
    startsWith?: boolean;
};

/**
 * Check if a pattern is included in a target (a string or an array of string)
 * Available options:
 * - caseSensitive: Enables case sensitivity
 * - accentsSensitive: Enables strict accents mode, a letter with accent will only match the same letter and accent
 * - startsWith: The target must start with the pattern. By default, the target only need to include the pattern to match
 */
export function matchPattern(pattern: string, target?: string | (string | null | undefined)[], options?: MatchOptions) {
    const matcher = (text?: string | null) =>
        text && normalize(text, options)[options?.startsWith ? "startsWith" : "includes"](normalize(pattern, options));
    return Array.isArray(target) ? target.some(matcher) : matcher(target);
}

export function normalize(text: string, options?: MatchOptions) {
    let normalizedText = text;
    if (!options?.caseSensitive) {
        normalizedText = normalizedText.toLowerCase();
    }
    if (!options?.accentsSensitive) {
        normalizedText = normalizedText.normalize("NFD").replace(/\p{Diacritic}/gu, "");
    }
    return normalizedText;
}

export default { matchPattern, normalize };

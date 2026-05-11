# Refactor Test Point Extraction Logic

## Background
The current LLM text parsing logic in `RequirementService` splits the LLM output strictly by newline characters (`\R`). Every single non-empty line (that passes a primitive `looksLikeNoise` check) is treated as an independent `TestPoint`, and only the `name` is assigned. 
This is fundamentally incompatible with structured Markdown outputs from the LLM, such as tables or multi-line descriptions. For example, if the LLM outputs a markdown table of an API interface, the current system will parse every row of the table (and delimiters like `| --- |`) as a separate, duplicate, or nonsensical test point.

## Proposed Changes

### Backend Component

#### [MODIFY] RequirementService.java
We will refactor the `RequirementService.java` to replace the naive line-by-line parsing with a "block-based" parsing strategy:

1. **Remove Line-by-Line Parsing**: Remove the `extractCandidateLines` logic that strictly loops over `split("\\R")`.
2. **Implement Block Parsing (`extractTestPointBlocks`)**: 
    - Parse the LLM output using heuristics to detect **blocks**. 
    - We will identify the start of a new test point block by looking for Markdown headings (e.g., `### ` or `## ` or `# `) or numbered lists (e.g., `1. `, `2. `).
    - Group the lines following a heading/list item into a single block.
3. **Map to `TestPoint` Entities**:
    - For each block, extract the clean title (e.g., stripping `### ` and extra symbols) to populate `TestPoint.setName()`.
    - Join the block's content (the entire chunk including tables/descriptions) and populate `TestPoint.setDescription()`.
    - Ensure `looksLikeNoise` is adapted or removed so we don't accidentally ignore valid description content.

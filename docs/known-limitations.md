# Source limitations and evidence boundaries

- Nutrition data is fictional workshop content.
- Original recipe/nutrition prompts receive names without nutrient facts or weights. Their parser checks a JSON object but does not fully enforce its business schema.
- Product totals require confirmed products and total grams. Generated wording still needs evaluation.
- Grounded citation/numeric checks do not establish semantic or unit correctness.
- Selected App Service/authentication/feature settings were inspected read-only; see `azure-observed-state.md`. No exact deployed artifact match, new learner sign-in, live model/Search call, PostgreSQL contents, or complete search infrastructure was verified.
- Platform access is limited to explicit identities; grounded chat has a separate single-owner restriction. Downloading the source grants neither permission.
- Cart data is shared. Chat/activity use process-local sessions. Local data is ephemeral.
- HTTP stubs test application contracts, not live model quality, cloud access, or provider compatibility.
- Live RAG requires the assets listed in `azure-setup.md`; assessment can proceed with gaps recorded.
- Source dependency versions are preserved. Dependency review is a separate assessment activity.
- An earlier-release rollback test depended on an absent JAR. The learner suite retains current-version restart/persistence checks and makes no historical rollback claim.

Distinguish source evidence, local checks, scenario assumptions, and verified deployment configuration.

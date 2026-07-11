---
name: creating-pull-requests
description: >-
  Create a well-formed pull request from the current branch — handles branch
  naming, base-branch detection (including stacked PRs), a clear title and
  description, pushing, and opening the PR with the gh CLI. Use this whenever the
  user wants to open, create, raise, or submit a PR / pull request / merge
  request, or asks to "put this up for review", "ship this branch", or "make a PR"
  — even if they don't spell out every step. Prefer this skill over ad-hoc git
  pushes so PRs stay consistent and reviewable.
---

# Creating Pull Requests

A pull request is read by a human who wasn't there when you wrote the code. The
goal of this skill is a PR whose title and description let a reviewer understand
*what changed and why* in under a minute, targeting the right base branch, without
you having to remember every step each time.

Work through the steps below in order. Most are quick; the ones that matter most
are getting the **base branch** right and writing a description that explains the
**why**, not just the what.

## Step 1: Decide whether to branch first

Look at the current branch (`git branch --show-current`).

- If it's the repo's default branch (`main`, `master`, or `develop`), or the user
  explicitly asked to "create a branch and a PR", **create a new branch first** —
  you should not open a PR directly from a shared trunk.
- If you're already on a working branch with commits, use it as-is.

When creating a branch, follow the repo's existing convention if there is one
(scan `git branch -a` for the prevailing style). Absent a convention, a
type-prefixed, kebab-case name reads well and sorts sensibly:

| Prefix      | When                                   | Example                        |
|-------------|----------------------------------------|--------------------------------|
| `feature/`  | New functionality                      | `feature/infinite-deck`        |
| `fix/`      | Bug fix                                | `fix/cart-crash-on-empty-promo`|
| `refactor/` | Restructuring without behavior change  | `refactor/extract-deck-state`  |
| `chore/`    | Tooling, deps, build, CI               | `chore/bump-compose-bom`       |
| `docs/`     | Documentation only                     | `docs/api-migration-notes`     |
| `test/`     | Adding or updating tests               | `test/reconciler-edge-cases`   |

For a large feature or epic, add a segment to express the hierarchy — e.g.
`feature/gifting/add-send-page`. This also feeds stacked-PR detection in Step 3.

## Step 2: Understand what the branch does

Read the actual change, don't guess: the commit subjects (`git log --oneline
<base>..HEAD`), the file stat (`git diff --stat <base>..HEAD`), and skim the diff
for anything surprising. You need a real grasp of the change to write a title and
description that are true to it.

If you can't reconstruct the **why** — the problem being solved or the goal —
from the commits and diff, **ask the user** rather than inventing a motivation. A
confident-but-wrong rationale is worse than a question.

## Step 3: Determine the base branch

The PR targets the branch it was forked from. Getting this wrong sends dozens of
unrelated commits into the wrong review, so resolve it deliberately in this order:

1. **User-specified** — if the user named a base branch, use it.

2. **Stacked / cascading PRs** — if the current branch name has 3+ segments
   (e.g. `feature/gifting/add-send-page`), strip the last segment and check whether
   that parent branch exists on the remote:
   ```bash
   git ls-remote --heads origin "feature/gifting"
   ```
   If it exists, it's almost certainly the intended base — each branch in a stack
   targets its parent, not the trunk.

3. **Repo default branch** — otherwise, target the repository's default branch.
   Detect it rather than assuming `main`:
   ```bash
   gh repo view --json defaultBranchRef --jq .defaultBranchRef.name 2>/dev/null \
     || git symbolic-ref --short refs/remotes/origin/HEAD 2>/dev/null | sed 's|^origin/||' \
     || echo main
   ```

4. **Confirm** — show the resolved base branch to the user before creating the PR,
   especially for a stacked PR where the base isn't the trunk. A one-line "Base:
   `feature/gifting` — correct?" is enough.

## Step 4: Push the branch

Make sure the branch is on the remote (`git push -u origin <branch>` if it isn't).
The PR can't be opened otherwise.

## Step 5: Compose the title

A good title is a single scannable line that says what the change accomplishes.
Match the repo's existing PR/commit-title convention if it has one (check recent
merged PRs or `git log`). Absent a convention, this default reads cleanly:

**Format:** `[Type] <Topic>. <Imperative summary>` — `Topic` is optional; include
it only when there's a meaningful feature/epic area (often the middle segment of a
hierarchical branch name).

| Type        | When                          |
|-------------|-------------------------------|
| `[Feature]` | New functionality             |
| `[Fix]`     | Bug fix                       |
| `[Refactor]`| Restructuring, no behavior change |
| `[Chore]`   | Tooling, deps, build, CI      |
| `[Docs]`    | Documentation                 |
| `[Test]`    | Tests                         |

**Examples:**

Input: branch `feature/gifting/add-send-page`, adds a gift-sending screen
Output: `[Feature] Gifting. Add gift-sending page`

Input: branch `fix/cart-crash-on-empty-promo`, fixes a null deref on empty promo
Output: `[Fix] Cart. Fix crash on empty promo state`

Input: branch `refactor/migrate-di-to-ksp`, swaps annotation processing backend
Output: `[Refactor] Migrate DI from kapt to KSP`

Keep the summary imperative ("Add", "Fix", "Migrate"), not past tense.

## Step 6: Compose the description

Write for the reviewer, in plain, direct English — the tone of a good teammate
explaining their change, not a formal report.

**If the repo has a PR template** (`.github/pull_request_template.md` or
`.github/PULL_REQUEST_TEMPLATE/`), use it and fill in each section. Teams rely on
their template; don't replace it with your own structure.

**Otherwise, use this structure:**

```markdown
## What & why

<1–2 sentences on the problem or goal, then what you did at a high level.>

## Changes

- <High-level, reviewer-facing bullets. Group by intent, not by file.>

## Testing

<How you verified it: commands run and their result, or manual steps. "N/A" if
genuinely nothing to run.>

## Screenshots / video

<For any UI change. Otherwise omit this section or write "N/A".>
```

**Good example:**

```markdown
## What & why

Users couldn't find caffeine-free drinks, so they kept asking support. Added a
"Caffeine-free" filter to the menu.

## Changes

- Added the filter checkbox to the menu filter sheet
- Threaded the new filter through MenuFeature's state and query
- Covered the filtering logic with unit tests

## Testing

`./gradlew :menu:test` — all green. Verified on device that toggling the filter
narrows the list and clearing it restores everything.

## Screenshots / video

_N/A_
```

**Avoid these:**

- Listing every changed file — that's what the diff is for. Describe intent.
- Formal filler ("In the scope of this task, the following was accomplished…").
  Say it plainly.
- Skipping the *why*. The motivation is the one thing the diff can't show, so it's
  the most valuable thing you can add.
- Padding a small change into a long template. Match the length to the change.

## Step 7: Confirm, then create

Show the user the resolved base branch, the title, and the description, and wait
for approval or edits. This is cheap and prevents a wrong-base or wrong-framing PR
that then needs to be edited or reopened.

Once approved, prefer the `gh` CLI over any GitHub MCP tool — check it's ready with
`gh auth status`. Create the PR, passing the body via a here-doc so formatting
survives:

```bash
gh pr create --base <base-branch> --title "<title>" --body-file - <<'EOF'
<body>
EOF
```

Add `--draft` if the user wants a draft. If `gh` isn't installed or authenticated,
fall back to the GitHub MCP `create_pull_request` tool.

Print the PR URL when it's created. If the change touches UI and you couldn't
attach screenshots, remind the user to add them.

## Quick rules

- Never open a PR from a shared trunk (`main`/`master`/`develop`) — branch first.
- Resolve the base branch deliberately; support stacked PRs; confirm before creating.
- Explain the *why* before the *what*; keep the length proportional to the change.
- Use the repo's PR template and title convention when they exist.
- Push before creating; confirm title, description, and base with the user first.

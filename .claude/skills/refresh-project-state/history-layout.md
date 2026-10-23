# History Layout

```text
workflow/project-history/
  README.md                      index of iterations (newest first)
  iteration-1/
    PROJECT_STATE.md             the old file, with an archive banner, links rebased
    done-ledger.md               the old ledger, links rebased
    branch-states/               the folded branch states (from branch-states/archive/), if any
    REFRESH.md                   the disposition record for the refresh that closed it
```

Iterations are numbered from 1 and never renumbered. Specs and plans stay in `workflow/`.

## Archive banner

Insert as the first lines of the moved `PROJECT_STATE.md` and `done-ledger.md`:

```markdown
> **ARCHIVED — iteration <n>, closed YYYY-MM-DD. Not the source of truth.** The current state is
> [`PROJECT_STATE.md`](<relative path to the root file>). Open this file only to learn why something
> was decided. Statuses, "Now" text and links inside describe the project as it was then.
```

## `REFRESH.md`

```markdown
# Refresh — iteration <n> closed

- **Closed:** YYYY-MM-DD by <model> on <INT | refresh/<n>> · last commit `<sha>`
- **Approved by:** <owner> YYYY-MM-DD (disposition table)
- **Developer guide at closure:** last full update YYYY-MM-DD, commit `<sha>`
- **ID high-water:** W<n>, C<n>, D<n>, Q<n>, S<n>
- **Size:** PROJECT_STATE.md <before> → <after> lines; ledger <k> entries archived
- **Open workstreams carried:** <ids, or none>

## Disposition
<the table from disposition-rules.md>
```

## `README.md` index

```markdown
# Project History

Archived records of earlier iterations. **Not read at session start.** The current state is
[`PROJECT_STATE.md`](../../PROJECT_STATE.md); the handover brief is
[`DeveloperGuide.md`](../DeveloperGuide.md).

| Iteration | Closed | Covers | Contents |
|---|---|---|---|
| [1](iteration-1/REFRESH.md) | YYYY-MM-DD | W1–W11: <one line on what was built> | state, ledger (<k> entries), branch states |
```

Create it on the first refresh; add a row at each later one.

## Link check

After the moves, every relative link in the moved files must resolve:

```bash
node -e "const fs=require('fs'),p=require('path');for(const f of process.argv.slice(1)){const d=p.dirname(f);for(const m of fs.readFileSync(f,'utf8').matchAll(/\]\(([^)#\s]+)/g)){if(/^([a-z]+:|\/)/i.test(m[1]))continue;if(!fs.existsSync(p.join(d,m[1])))console.log(f,'->',m[1])}}" workflow/project-history/iteration-<n>/*.md PROJECT_STATE.md
```

The check is crude: it also reports link-shaped text inside code spans and fences. Judge each hit.
Links to gitignored specs and plans resolve on the owner's disk but not in a fresh clone; that is
expected here and not a failure.

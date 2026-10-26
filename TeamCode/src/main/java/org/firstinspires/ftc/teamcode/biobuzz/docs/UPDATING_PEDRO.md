# Updating Pedro Pathing (merging a new Quickstart)

> **For mentors.** Students: you do not need this file to get started. Begin with the
> student guide, `biobuzz/README.md` ("Start here").

Our repository is a copy of the Pedro Pathing Quickstart with our `biobuzz/` package added.
New Pedro versions arrive by merging the Quickstart's `master` branch.

**When:** once a month in the off-season and before each competition. **Never in the 7 days
before an event.**

## Steps

1. Check the remote. In this repository it is named **`pedro`**
   (`git remote -v` shows `pedro https://github.com/Pedro-Pathing/Quickstart.git`).
   The Requirements call it `quickstart`; either name works, the commands below use `pedro`.
2. Make a branch: `git checkout -b update-quickstart-2026-11-01` (today's date).
3. `git fetch pedro`, then `git merge pedro/master`.
4. **If there is a conflict:**
   - `pedro/Constants.java` or `pedro/Tuning.java`: **keep our values and our tuner list, take
     the Quickstart's new structure.** Open the file, look at each `<<<<<<<` block: the part
     marked HEAD is ours, the other is theirs. Keep their new lines (new config names, new
     imports) and put our numbers into them. Delete the markers.
   - `build.dependencies.gradle`: take the Quickstart's Pedro and SDK versions; keep our block
     at the end (`// ---- BioBuzz additions ----`).
   - Anything under `biobuzz/` should never conflict (the Quickstart doesn't have it).
5. Build (**Build → Make Project**). Fix any compile errors: a renamed Pedro method shows up in
   `drive/Drive.java` or `drive/FieldPaths.java` (the only team files that use Pedro).
6. Test: the Quickstart's `Tests` OpModes, then **Field Checks 01 to 09 at home**, compared with
   the baseline in `FieldCheckConfig`.
7. Only if everything passes: merge the branch to the main branch, and record in
   `UPSTREAM.md` (repository root) the Quickstart commit (`git log -1 pedro/master`) and the
   test results.
8. If something fails and can't be fixed quickly: `git merge --abort` (during the merge) or
   just switch back to the main branch and delete the update branch. Nothing is lost.

## What we changed in Quickstart files

Every change to a Quickstart-owned file is listed in `UPSTREAM.md`. Today: the FTC Dashboard
block at the end of `build.dependencies.gradle` and `.gitignore` lines. At drive tuning,
`Constants.java` and `Tuning.java` get their one-time fill-in (`Tuning.java` from our
[Pedro tuning kit](https://github.com/smrc-pune/pedropathing/tree/main/pedro-tuning-kit)).

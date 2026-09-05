# Module 0 — Setup, Self-Audit, and DSA Habit Bootstrap

| Field | Value |
|---|---|
| Phase | 0 (Foundation prep) |
| Status | Core — do not skip |
| Weekly Budget | 16 hrs/week |
| Module Duration | ~1.5 weeks (~14 hrs total module work + DSA hours) |
| Output | Audit doc, LeetCode public profile, GitHub solutions repo, weekly schedule pinned |

---

## 1. What this module actually is — and why it can't be skipped

Module 0 is not "learn a thing." Module 0 is "set the conditions for the next 9 months to compound."

You will:

- Map your real Java / Spring / SQL level — honestly, with a 70-question checklist
- Set up the study infrastructure (LeetCode, GitHub solutions repo, notes vault, timer)
- Establish the DSA daily habit with 7 problems in 7 days
- Audit wrenchy.in against your own SRS to find pending real work

**Why it matters.** Habits compound. Modules don't compound. If you skip Module 0 and jump to Module 2, you'll have to backfill it in week 4 anyway — but by then the bad habits (irregular DSA, scattered notes, no honest baseline) will already be set.

**Brutally honest.** Most students who fail at "I'll learn Java this semester" fail in week 2, not week 12. They lacked Module 0.

---

## 2. The four parts of this module

| Part | What | Time |
|---|---|---|
| A | Self-audit — the 70-question honest checklist | 4 hrs |
| B | Study environment setup | 3 hrs |
| C | DSA habit bootstrap (7 problems, 7 days) | 7 hrs (1 hr/day) |
| D | wrenchy.in pending-work audit | 3 hrs |

Total: ~17 hrs (DSA hours overlap with the daily track). Spread over 10 days.

---

## 3. Part A — The Self-Audit (4 hrs)

### 3.1 Scoring scale

For each question, score yourself 0–3:

- **0** — Never heard of it / cannot explain at all
- **1** — Seen it / used it once / vague idea
- **2** — Can use in code but cannot fully explain *why* or how it works internally
- **3** — Can use, explain, and teach someone else

**Be honest.** Inflating your score now wastes the next 9 months. A score of 1 you mark as 1 is more useful than a 3 you cannot defend.

### 3.2 Java audit (30 questions)

| # | Question |
|---|---|
| 1 | Primitive vs reference types — what happens to a `Person p1 = p2;` assignment? |
| 2 | Why is `String` immutable? What's the String pool? |
| 3 | Difference between `==`, `.equals()`, and `Objects.equals()`. |
| 4 | The `equals()` / `hashCode()` contract — what breaks if you violate it? |
| 5 | `final` on a variable, on a method, on a class — three different meanings. |
| 6 | `static` vs instance — when is the field initialised? |
| 7 | Method overriding rules and the `@Override` annotation. |
| 8 | Abstract class vs interface in Java 8+ (default methods). When choose which? |
| 9 | Checked vs unchecked exceptions — which to throw, which to catch. |
| 10 | `try-with-resources` — what makes a class eligible? |
| 11 | `ArrayList` vs `LinkedList` — when do you actually pick LinkedList? |
| 12 | `HashMap` internals — what happens on a hash collision? When does it become a tree? |
| 13 | `HashSet` vs `TreeSet` — order, complexity, when to use which. |
| 14 | Generics — what does `<? extends Number>` vs `<? super Number>` mean? PECS rule. |
| 15 | Streams — `map`, `filter`, `reduce`, `collect`. Lazy vs eager. Stateful vs stateless. |
| 16 | `Optional` — when to use, when *not* to use (rule: never as a field). |
| 17 | Lambdas — what's a functional interface? Can you write `Function<String, Integer>`? |
| 18 | Method references — the four kinds. |
| 19 | Varargs — how does it desugar? |
| 20 | `record` (Java 14+) — what does the compiler generate? |
| 21 | Pattern matching for `instanceof` (Java 16+). |
| 22 | Sealed classes (Java 17+) — what problem do they solve? |
| 23 | Virtual threads (Java 21) — what makes them "virtual"? Carrier threads. |
| 24 | JVM memory: stack vs heap vs metaspace. Where does a `String s = "hi"` live? |
| 25 | Garbage collection — generational hypothesis, why young gen exists. |
| 26 | `Thread` vs `Runnable` — why prefer Runnable. |
| 27 | `synchronized` — what does it lock on for an instance method? For a static method? |
| 28 | `volatile` — what guarantees does it give? What does it *not* give? |
| 29 | Producer-consumer with `BlockingQueue` — sketch it. |
| 30 | `CompletableFuture` — `thenApply` vs `thenCompose` vs `thenCombine`. |

### 3.3 Spring audit (25 questions)

| # | Question |
|---|---|
| 1 | IoC — explain in plain English. Why does it matter? |
| 2 | DI by constructor vs by field — which does Spring recommend now, why? |
| 3 | `@Component`, `@Service`, `@Repository`, `@Controller` — actually different or just labels? |
| 4 | Bean scopes — singleton vs prototype vs request vs session. |
| 5 | `@Configuration` vs `@Component` — why does it matter for `@Bean` methods? (Hint: CGLIB proxying.) |
| 6 | `@Value` vs `@ConfigurationProperties` — when each? |
| 7 | Profiles — how do you activate one? What's `application-prod.yml`? |
| 8 | `ApplicationContext` vs `BeanFactory`. |
| 9 | Bean lifecycle — `@PostConstruct`, `InitializingBean`, `@PreDestroy`. |
| 10 | `@RestController` vs `@Controller` — what's the difference? |
| 11 | `@GetMapping` vs `@RequestMapping(method = GET)` — same thing. Why prefer the former? |
| 12 | `@PathVariable`, `@RequestParam`, `@RequestBody` — three different things. |
| 13 | `ResponseEntity<T>` — why use it over returning `T` directly? |
| 14 | `@Valid` and Bean Validation — `@NotNull`, `@Size`, custom constraints. |
| 15 | `@ControllerAdvice` — what does it do? Why is it useful? |
| 16 | Spring Data JPA derived queries — `findByCustomerPhoneOrderByCreatedAtDesc(...)`. |
| 17 | `@Transactional` propagation — `REQUIRED` vs `REQUIRES_NEW`. When use the latter? |
| 18 | LAZY vs EAGER fetch — what's the default for `@ManyToOne`? For `@OneToMany`? |
| 19 | The N+1 query problem — describe it. How do you detect and fix it? |
| 20 | `@Query` JPQL vs native — when prefer which? |
| 21 | `SecurityFilterChain` — what's the modern bean-based pattern (Spring Security 6+)? |
| 22 | `SecurityContextHolder` — what does it hold, and where? |
| 23 | A custom JWT filter — describe at the level of `OncePerRequestFilter`. |
| 24 | `@PreAuthorize("hasRole('ADMIN')")` — what does it require to work? |
| 25 | Spring Boot autoconfiguration — how does `DataSourceAutoConfiguration` know whether to create a bean? |

### 3.4 SQL audit (15 questions)

| # | Question |
|---|---|
| 1 | INNER vs LEFT vs RIGHT vs FULL OUTER JOIN — sketch a Venn diagram. |
| 2 | `GROUP BY` — what columns can appear in `SELECT`? Why? |
| 3 | `HAVING` vs `WHERE`. |
| 4 | Correlated subquery vs uncorrelated — give one of each from wrenchy.in. |
| 5 | CTE — what is a `WITH RECURSIVE`? |
| 6 | Window functions — `ROW_NUMBER()` vs `RANK()` vs `DENSE_RANK()`. |
| 7 | `EXISTS` vs `IN` — when does each perform better? |
| 8 | B-tree index — when does it help? When does it *not* help (e.g., `LIKE '%foo'`)? |
| 9 | Composite index — column order matters. Why? |
| 10 | `EXPLAIN ANALYZE` — what's the difference between `cost` and `actual time`? |
| 11 | ACID — define each letter without looking. |
| 12 | Transaction isolation levels — READ COMMITTED vs REPEATABLE READ. What anomaly does each prevent? |
| 13 | Deadlock — what's the simplest scenario that causes one? |
| 14 | Primary key vs unique constraint — both enforce uniqueness, what's the difference? |
| 15 | `ON DELETE CASCADE` vs `ON DELETE RESTRICT` vs `ON DELETE SET NULL`. |

### 3.5 Scoring and interpretation

After scoring all 70 questions:

- **Total ≥ 175 (avg 2.5+)** — you have strong fundamentals. Phase 1 modules can move faster.
- **Total 105–175 (avg 1.5–2.5)** — typical 3rd-year level. Plan stays as-is.
- **Total < 105 (avg < 1.5)** — slower start; spend 1.5 × the budgeted time on Modules 2–8.

**[FACT]** Most 3rd-year B.Tech CSE students score 80–140. Don't panic if you score 90. The score's purpose is to **find the holes**, not to grade you.

### 3.6 Output of Part A

A private markdown file `docs/modules/audit-baseline.md` with:

- Date of audit
- Score per question
- Total + average
- Top 5 weakest areas (the 1s and 0s grouped)
- One sentence per weak area: "I will close this in Module X"

---

## 4. Part B — Study Environment Setup (3 hrs)

### 4.1 LeetCode account + public profile

- Sign up at leetcode.com with your professional email
- Add a real photo + bio mentioning wrenchy.in
- Make your problem-solving stats public
- Bookmark your profile URL — it's an asset for the resume

### 4.2 GitHub solutions repo

Create a new public repo: `dsa-pattern-solutions` (or similar). Structure:

```
dsa-pattern-solutions/
├── README.md                  ← pattern index, problem count, current streak
├── 01-arrays-two-pointers/
│   ├── README.md              ← the pattern card (see Part C)
│   ├── two-sum.java
│   ├── two-sum.md             ← notes on this specific problem
│   └── ...
├── 02-sliding-window/
└── ...
```

**README.md should display:**

```markdown
# DSA Pattern Solutions

| Pattern | Problems Solved | Status |
|---|---|---|
| Arrays + Two Pointers | 1 / 12 | In progress |
| Sliding Window | 0 / 10 | Pending |
| ...

Current streak: 7 days
Total solved: 7
LeetCode profile: leetcode.com/u/itsAdityaG/
```

Update this table after every problem. The discipline of updating it IS half the value.

### 4.3 Notes vault

A `wrenchy-study/` folder (markdown vault). Recommended structure:

```
wrenchy-study/
├── modules/
│   ├── module-0-setup.md
│   ├── module-2-java-core.md
│   └── ...
├── interview-questions/
│   ├── java-core.md
│   ├── spring-internals.md
│   └── ...
├── revision-pile.md            ← what to revisit this week
└── daily-log.md                ← 2-line daily entry
```

Use Obsidian, Notion, or just `nano` — tool doesn't matter; the discipline of writing notes after every study session matters.

### 4.4 Timer / Pomodoro

Use any timer (phone, browser). Suggested:

- DSA daily: 1× 50-minute focused block + 10-minute notes-writing
- Module sessions: 2× 50-minute focused blocks + 10-minute review

**No phone during the focused block.** This is the single most-violated rule.

### 4.5 Weekly review cadence

Every Sunday evening (or whichever day you commit to):

- 30 minutes of review
- Update the README in the DSA repo
- Update `revision-pile.md` with anything still fuzzy
- Pick 2 problems from earlier weeks to re-solve cold

**This compounds.** Skip it, and the rest of the roadmap loses ~30% effectiveness.

### 4.6 Output of Part B

- Public LeetCode profile URL
- Public GitHub solutions repo URL (empty for now is fine; will fill in Part C)
- Notes vault initialised
- Weekly review day committed to (write it in the daily log)

---

## 5. Part C — DSA Habit Bootstrap (7 hrs, one per day for 7 days)

### 5.1 The 7 problems, in order

| Day | Pattern | Problem | LeetCode # |
|---|---|---|---|
| 1 | Arrays + Hashing | Two Sum | 1 |
| 2 | Strings + Two Pointers | Valid Palindrome | 125 |
| 3 | Sliding Window | Best Time to Buy and Sell Stock | 121 |
| 4 | Linked List + Fast/Slow Pointer | Linked List Cycle | 141 |
| 5 | Stack | Valid Parentheses | 20 |
| 6 | Trees + DFS | Binary Tree Inorder Traversal | 94 |
| 7 | Bit Manipulation | Reverse Bits | 190 |

All 7 are Easy. The point is **not** difficulty. The point is:

- Establishing the daily streak
- Touching seven different patterns to seed the mental map
- Practicing the post-problem ritual (writing the pattern card)

### 5.2 The post-problem ritual (every problem, no skipping)

After each problem, write a one-page markdown "pattern card" in the relevant folder:

```markdown
# Pattern: Arrays + Hashing

## When to recognize this pattern
- Need to find pairs / count occurrences in O(n)
- Asked "exists" or "frequency" of something

## Template
HashMap<Integer, Integer> map = new HashMap<>();
for (int i = 0; i < nums.length; i++) {
    int complement = target - nums[i];
    if (map.containsKey(complement)) return new int[]{map.get(complement), i};
    map.put(nums[i], i);
}

## Problem I solved
LeetCode 1 — Two Sum.
My approach: single-pass hashmap of seen-numbers.
Time: O(n). Space: O(n).

## What I missed on first attempt
- I initially wrote a nested loop (O(n²)) before refactoring.
- Forgot to handle the case where the same element is used twice.

## Re-solve checklist
[ ] Can I re-solve this cold in 10 minutes?
[ ] Can I explain the trick out loud in 60 seconds?
```

**Why pattern cards.** When you see a new problem 4 months from now, you don't need to remember the problem — you need to remember the *pattern* it matches. Cards reinforce the pattern, not the puzzle.

### 5.3 Daily log

Add a one-line entry per day in `daily-log.md`:

```
2026-06-01 — D1. Two Sum. 18 min. Pattern card written. Notes: forgot edge case until test 2.
2026-06-02 — D2. Valid Palindrome. 22 min. Two-pointer template internalised.
```

**This log is the proof of streak.** Going public with it (committing the log to GitHub) is optional but recommended — public commitment compounds.

### 5.4 Output of Part C

- 7 problems solved, code committed to GitHub
- 7 pattern cards written
- 7 daily-log entries
- 7-day GitHub commit graph showing green squares

---

## 6. Part D — wrenchy.in Pending-Work Audit (3 hrs)

### 6.1 Cross-reference your real code against your own docs

Open these in tabs:

- `docs/SRS.md` — what the system *should* do
- `docs/SYSTEM_DESIGN.md` — how it's designed
- `docs/DATABASE_DESIGN.md` — the schema
- `docs/API_DOCUMENTATION.md` — the API surface
- Your actual codebase

For every `[ASSUMPTION]` and `[ADD:]` and `[VALIDATE]` tag in those four documents, ask: **is this still pending, or has it been done?**

### 6.2 The pending-work tracker

Create `docs/modules/wrenchy-pending.md`:

```markdown
# wrenchy.in Pending Work — Captured Module 0

| # | Item | Source Doc | Module That Closes It | Status |
|---|---|---|---|---|
| 1 | systemd auto-restart unit files | SD §7.3 | Module 15 | Pending |
| 2 | 2Factor.in OTP delivery | SRS §FR-AUTH-02 | Module 8 | Pending |
| 3 | Gateway rate limiting | SRS §NFR-SEC-04 | Module 11 | Pending |
| 4 | X-Request-Id propagation | SRS §NFR-OBSERV-03 | Module 16 | Pending |
| 5 | Automated test suite | SRS §NFR-MAINT-02 | Module 14 | Pending |
| 6 | Fast2SMS wallet recharge | (ops) | Anytime | ₹100 pending |
| 7 | Razorpay live keys | (ops) | Module 8 | Test mode only |
| ... | ... | ... | ... | ... |
```

Goal: **15–25 items.** If you find fewer than 15, you haven't read the docs carefully enough. If you find more than 30, scope-cut.

### 6.3 Output of Part D

- `docs/modules/wrenchy-pending.md` with at least 15 numbered items
- Each item tagged with the module that closes it
- The tracker becomes the project's source of truth for what's "still to do"

---

## 7. Wrenchy.in connection summary

Module 0 has no code-level connection — it sets up the conditions. The connection appears in:

- The audit reveals the real Java/Spring gaps that you'll close *via* wrenchy.in code in Modules 2–8
- The pending-work tracker is the canonical to-do list for the project over the next 9 months
- The DSA habit, once established, runs in parallel and never derails project work

---

## 8. Likely interview questions about Module 0 stuff

Yes, even setup has interview ROI:

- **"How do you decide what to learn next?"** Answer: I audit honestly, find my weakest 5 areas, and plan modules that close them while connecting to a real project. (You'll be one of the 5% of freshers with a real answer.)
- **"Walk me through your study process."** Answer: daily DSA, weekly modules tied to a production project, weekly review with a revision pile, post-problem pattern cards.
- **"How do you keep your fundamentals sharp?"** Answer: the revision-pile pattern + weekly re-solves of older problems.
- **"What's your strongest area? Your weakest?"** Now answerable from the audit.

These aren't trick questions — they're vetting whether you can think about your own learning. Most freshers can't.

---

## 9. The week-by-week task breakdown

### Days 1–3 (Part A — Audit, 4 hrs)

- Day 1 evening: Java audit (2 hrs)
- Day 2 evening: Spring audit + SQL audit (2 hrs)
- Day 3 evening: write `audit-baseline.md` — totals + 5 weakest

### Days 1–2 (Part B — Environment, in parallel with audit)

- Day 1: LeetCode profile + GitHub repo init
- Day 2: Notes vault + timer + weekly review day committed

### Days 1–7 (Part C — DSA daily, 1 hr each)

- One problem per day, in the listed order
- Pattern card after each
- Daily log entry committed

### Days 4–6 (Part D — wrenchy.in pending audit, 3 hrs)

- Day 4 evening: read SRS + SD with the pending-tracker open (1 hr)
- Day 5 evening: read DB Design + API doc (1 hr)
- Day 6 evening: finalise the tracker, tag each item with closing module (1 hr)

### Day 7 (Module 0 wrap)

- The 7th DSA problem
- A 30-minute self-review: did you complete all four parts honestly?
- A 1-page "Module 0 complete" note in the notes vault, with links to: audit baseline, public LeetCode profile, public DSA repo, pending tracker.

### Days 8–10 (slack / catch-up)

- Honest reality: if you missed any DSA day in week 1, days 8–10 are when you make up.
- Don't roll forward incomplete output. **A messy Module 0 = a messy Module 2.**

---

## 10. The re-explain test (how you know Module 0 is truly done)

Without looking at notes, you should be able to answer all of these:

1. **What are your 5 weakest areas in Java/Spring/SQL?** (Just name them. No defense needed.)
2. **What's the next module, and why does it come first?** ("Module 2 — Java core — because two of my 5 weak areas are equals/hashCode and immutability.")
3. **What's your daily DSA schedule?** ("1 hour every day, 50-min focused + 10-min notes, in the morning before college.")
4. **How will you measure progress over the next 9 months?** ("LeetCode count + completed modules + pending-tracker items closed.")
5. **What's currently the single most pending thing in wrenchy.in?** (Whatever's at the top of your tracker. Probably the test suite or systemd.)

If you fail any of these, Module 0 is not done. Loop back.

---

## 11. Output checklist (commit / share when ready)

- [ ] `audit-baseline.md` written and totalled
- [ ] LeetCode profile public, photo + bio added
- [ ] GitHub solutions repo public, README table populated
- [ ] 7 problems solved + 7 pattern cards in the repo
- [ ] `daily-log.md` with 7 entries, committed
- [ ] `wrenchy-pending.md` with 15+ items
- [ ] Weekly review day committed in the daily log
- [ ] "Module 0 complete" note in the vault

Reply with "Module 0 done" and a one-line summary of your audit total + top 3 weak areas, plus a link to your DSA solutions repo. Then I produce **Module 2 — Java Core Refresh and OOP Depth** (~3.3 weeks at 9 hrs/week module budget).

---

## 12. Going forward — progress tracking conventions

Each time we resume the roadmap, the assistant will state:

1. **Current module:** the one you're in
2. **Completed modules:** counted
3. **Pending artifacts:** anything you said you'd ship but haven't (caught from the daily log and tracker)
4. **Revision-pile surface:** one concept from a previous module worth revisiting this week

You answer with: what you actually did, what tripped you up, and what you want to do next. **Honesty over performance.** I'd rather you say "I missed 3 DSA days and only finished the audit" than fabricate a perfect week.

---

*End of Module 0. No teaching of Module 2 happens until you reply with "Module 0 done" plus your audit total + DSA repo link.*

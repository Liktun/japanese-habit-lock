# Three front-end directions — spec

All three render the SAME `DailyChecklist` data so the comparison is fair.
Each is self-contained under `ui/styles/<name>/` and does not touch the
existing screens, so nothing that already works can break.

## Shared contract

Every style package exposes exactly one entry point:

```kotlin
@Composable
fun <Style>Screen(
  checklist: DailyChecklist,
  onToggleTask: (String, Boolean) -> Unit,
  onOpenTask: (RoadmapTask) -> Unit,
  modifier: Modifier = Modifier,
)
```

`DailyChecklist` exposes: `day: StudyDay` (`.date: LocalDate`), `weekNumber: Int`,
`phase: Phase` (`.label`, `.summary`), `blockingTasks` / `optionalTasks:
List<RoadmapTask>`, `isDone(task)`, `blockingDone`, `blockingTotal`,
`isUnlocked: Boolean`, `progress: Float` (0f..1f), `focus: String`,
`checkpoints: List<String>`, `phasePrompt: String?`.

`RoadmapTask` exposes: `id`, `title`, `detail`, `launch: TaskLaunch?`.

Every screen must show: phase + week + date, locked/unlocked state with
progress, the blocking tasks with checkboxes, the optional tasks marked as
never-blocking, and the weekly checkpoints.

---

## Style 1 — SUMI-E 墨絵 (ink & paper)

Traditional ink wash. Monochrome discipline with one red accent, like a hanko
seal on rice paper. Calm, spare, lots of negative space (間 *ma*).

| Token | Hex |
|---|---|
| washi paper | `#F4F1EA` |
| paper shade | `#E8E3D8` |
| sumi ink | `#1C1A17` |
| ink soft | `#4A453D` |
| ink wash | `#8B857A` |
| vermilion 朱色 | `#C8452F` |
| gold | `#B8925A` |

- `FontFamily.Serif`, generous letter spacing, thin rules instead of cards.
- Progress = a brush stroke that grows, not a bar.
- Completed task = struck through with a hand-drawn ink line.
- Unlock = a vermilion hanko seal stamps down (scale + rotate + settle).
- No rounded cards. Hairline dividers. Vertical katakana column as a margin motif.

## Style 2 — NEON YOKOCHO ネオン横丁 (night alley)

Shinjuku back-alley at 2am. Dark, wet, glowing signage. High energy, the
opposite of style 1.

| Token | Hex |
|---|---|
| night | `#0A0812` |
| panel | `#14101F` |
| neon magenta | `#FF2D95` |
| neon cyan | `#00E5FF` |
| lantern amber | `#FFB020` |
| electric violet | `#9D4EDD` |
| text | `#F0EBFF` |

- Monospace, uppercase, tight tracking. Katakana labels.
- Glow via layered translucent borders and shadow.
- Locked = magenta, unlocked = cyan. Both pulse slowly.
- Neon flicker on the title (irregular, not a clean sine).
- Progress = a charging energy bar with a bright leading edge.

## Style 3 — WA-MODERN 和モダン (seasonal light)

Contemporary Japanese design: cream paper, sakura, matcha, gold leaf, kumiko
geometry. Warm and inviting where style 1 is austere and style 2 is loud.

| Token | Hex |
|---|---|
| kinari cream | `#FBF7F0` |
| surface | `#FFFFFF` |
| sakura | `#F2A8B8` |
| sakura deep | `#D9647E` |
| matcha | `#7A9A6B` |
| matcha deep | `#4F6B45` |
| indigo 藍色 | `#2E4057` |
| gold leaf | `#C9A227` |
| warm gray | `#8A8078` |

- Soft rounded cards, layered shadows, generous padding.
- Sakura petals drifting down behind the header (slow, looping, subtle).
- Progress = a ring that fills, matcha green.
- Task completion = a spring bounce + colour wash.
- Kumiko-style geometric pattern as a faint section divider.

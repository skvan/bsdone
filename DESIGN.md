---
version: alpha
name: BSD Portal Design System
description: Warm paper, deep navy and muted gold personal clubhouse language for the BSD baseball portal. Light and dark themes are fixed; the approved V3 player almanac is the reference implementation.
colors:
  primary: "#C9A45B"
  on-primary: "#201507"
  primary-hover: "#D8B775"
  accent-ink: "#7A5A1F"
  gold-soft: "#F6EEDC"
  canvas: "#F2EFE9"
  canvas-soft: "#F7F4EE"
  surface: "#FFFFFF"
  surface-soft: "#FBF9F4"
  surface-elevated: "#F6F1E7"
  ink: "#071A33"
  text: "#1F2937"
  muted: "#5B6472"
  faint: "#8A919C"
  hairline: "#E8E2D7"
  hairline-soft: "#F0ECE4"
  success: "#1F7A5A"
  danger: "#B5122E"
  hero-start: "#0A2442"
  hero-mid: "#071A33"
  hero-end: "#041020"
  on-hero: "#FFFFFF"
typography:
  display-xl:
    fontFamily: "Songti SC, STSong, Noto Serif SC, Georgia, serif"
    fontSize: 64px
    fontWeight: 700
    lineHeight: 1
    letterSpacing: 0px
  display-lg:
    fontFamily: "Songti SC, STSong, Noto Serif SC, Georgia, serif"
    fontSize: 48px
    fontWeight: 700
    lineHeight: 1.05
    letterSpacing: 0px
  heading-lg:
    fontFamily: "Songti SC, STSong, Noto Serif SC, Georgia, serif"
    fontSize: 28px
    fontWeight: 700
    lineHeight: 1.2
    letterSpacing: 0px
  heading-md:
    fontFamily: "Songti SC, STSong, Noto Serif SC, Georgia, serif"
    fontSize: 22px
    fontWeight: 700
    lineHeight: 1.25
    letterSpacing: 0px
  heading-sm:
    fontFamily: "PingFang SC, Microsoft YaHei, Noto Sans SC, system-ui, sans-serif"
    fontSize: 16px
    fontWeight: 700
    lineHeight: 1.3
    letterSpacing: 0px
  body-lg:
    fontFamily: "PingFang SC, Microsoft YaHei, Noto Sans SC, system-ui, sans-serif"
    fontSize: 16px
    fontWeight: 400
    lineHeight: 1.7
    letterSpacing: 0px
  body-md:
    fontFamily: "PingFang SC, Microsoft YaHei, Noto Sans SC, system-ui, sans-serif"
    fontSize: 14px
    fontWeight: 400
    lineHeight: 1.65
    letterSpacing: 0px
  body-sm:
    fontFamily: "PingFang SC, Microsoft YaHei, Noto Sans SC, system-ui, sans-serif"
    fontSize: 12px
    fontWeight: 400
    lineHeight: 1.6
    letterSpacing: 0px
  caption:
    fontFamily: "PingFang SC, Microsoft YaHei, Noto Sans SC, system-ui, sans-serif"
    fontSize: 11px
    fontWeight: 500
    lineHeight: 1.5
    letterSpacing: 0px
  label:
    fontFamily: "PingFang SC, Microsoft YaHei, Noto Sans SC, system-ui, sans-serif"
    fontSize: 10px
    fontWeight: 700
    lineHeight: 1.4
    letterSpacing: 1.2px
  stat-xl:
    fontFamily: "Bahnschrift, DIN Alternate, Arial Narrow, sans-serif"
    fontSize: 48px
    fontWeight: 700
    lineHeight: 1
    letterSpacing: -1px
  stat-lg:
    fontFamily: "Bahnschrift, DIN Alternate, Arial Narrow, sans-serif"
    fontSize: 32px
    fontWeight: 700
    lineHeight: 1
    letterSpacing: -0.5px
  stat-md:
    fontFamily: "Bahnschrift, DIN Alternate, Arial Narrow, sans-serif"
    fontSize: 22px
    fontWeight: 700
    lineHeight: 1
    letterSpacing: 0px
  button-md:
    fontFamily: "PingFang SC, Microsoft YaHei, Noto Sans SC, system-ui, sans-serif"
    fontSize: 13px
    fontWeight: 600
    lineHeight: 1
    letterSpacing: 0px
rounded:
  none: 0px
  xs: 4px
  sm: 6px
  md: 8px
  lg: 12px
  xl: 18px
  2xl: 24px
  full: 9999px
spacing:
  xxs: 4px
  xs: 8px
  sm: 12px
  md: 16px
  lg: 20px
  xl: 24px
  2xl: 32px
  3xl: 40px
  section: 56px
components:
  site-header:
    backgroundColor: "{colors.canvas}"
    textColor: "{colors.ink}"
    typography: "{typography.body-sm}"
    height: 68px
  user-chip:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.ink}"
    typography: "{typography.caption}"
    rounded: "{rounded.full}"
    height: 40px
  page-title:
    textColor: "{colors.ink}"
    typography: "{typography.heading-lg}"
  hero:
    backgroundColor: "{colors.hero-mid}"
    textColor: "{colors.on-hero}"
    rounded: "{rounded.2xl}"
    padding: "30px 34px"
  player-card:
    backgroundColor: "{colors.hero-start}"
    textColor: "{colors.on-hero}"
    rounded: "{rounded.lg}"
    padding: "18px"
  avatar:
    backgroundColor: "{colors.hero-start}"
    textColor: "{colors.on-hero}"
    rounded: "{rounded.full}"
    size: 48px
  panel:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.text}"
    rounded: "{rounded.xl}"
    padding: "20px"
  rank-band:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.ink}"
    rounded: "{rounded.xl}"
    padding: "18px 22px"
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
    typography: "{typography.button-md}"
    rounded: "{rounded.sm}"
    padding: "0 16px"
    height: 36px
  button-primary-hover:
    backgroundColor: "{colors.primary-hover}"
    textColor: "{colors.on-primary}"
    typography: "{typography.button-md}"
    rounded: "{rounded.sm}"
    height: 36px
  button-ghost:
    backgroundColor: "transparent"
    textColor: "{colors.on-hero}"
    typography: "{typography.button-md}"
    rounded: "{rounded.sm}"
    height: 36px
  scope-pill:
    backgroundColor: "{colors.surface-soft}"
    textColor: "{colors.ink}"
    typography: "{typography.caption}"
    rounded: "{rounded.sm}"
    height: 30px
  event-tag-league:
    backgroundColor: "{colors.gold-soft}"
    textColor: "{colors.accent-ink}"
    typography: "{typography.caption}"
    rounded: "{rounded.xs}"
    padding: "3px 6px"
  stat-value:
    textColor: "{colors.ink}"
    typography: "{typography.stat-lg}"
  data-table:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.text}"
    typography: "{typography.body-sm}"
    rounded: "{rounded.none}"
  chart:
    backgroundColor: "transparent"
    textColor: "{colors.primary}"
    typography: "{typography.caption}"
  theme-toggle:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.muted}"
    rounded: "{rounded.full}"
    size: 32px
  footer:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.on-hero}"
    typography: "{typography.caption}"
    height: 64px
---

# BSD Portal Design System

## Overview

This is the site-level visual language for the public baseball portal and the player's personal clubhouse. It comes from the approved **V3 player almanac** exploration and replaces the one-off visual choices in the earlier A/B mockups.

The personality is **warm paper + deep navy + muted gold**: editorial enough to feel like a personal baseball card, but structured enough for data, rankings and schedules. The hero is always deep navy; the page canvas is warm paper in light mode and near-black navy in dark mode.

Status: **colors and light/dark pairing are owner-approved (2026-10-21)**. Component specs below are proposed and must be verified in the real Vue implementation before being treated as production truth.

### 中文速览

- 参考实现：V3 球星年鉴；浅色 / 深色两套色号已固定为网站设计语言。
- 浅色：纸色 `#F2EFE9`、卡片 `#FFFFFF`、墨色 `#071A33`、正文 `#1F2937`、金色 `#C9A45B`；金色小字必须用 `#7A5A1F`。
- 深色：底色 `#0B1018`、卡片 `#131B26`、标题 `#F3F6FA`、正文 `#D5DDE8`、金色 `#D4AF6A`。
- 深色主题以现有 `html.dark` / `data-portal-theme` 为准；管理台是否切换另行决定。
- 后续任何前端改动先对照本文件：门户和个人驾驶舱必须遵循；其他页面评估后再改。

## Sources Inspected

| Source | Status | Confidence | Notes |
|---|---|---:|---|
| V3 player almanac mockup (local design artifact, not committed) | Approved reference | confirmed | Owner selected V3; visual source of truth for the dashboard. The HTML/CSS artifact lives in the design workspace `_design/my-home-mockup/`, not in this repo. |
| Shared exploration tokens (local `common.css`, not committed) | Reference implementation | confirmed | Light/dark values used by the three explorations; values are owner-approved. |
| Existing portal theme store (`bsball-frontend/src/stores/portalTheme.js`) | Production evidence | confirmed | Supports light / dark / auto, applies `html.dark` and `data-portal-theme` |
| Existing global styles (`bsball-frontend/src/styles/index.css`) | Production evidence | conflict | Legacy `--bsball-*` values differ from the approved palette; see conflicts |
| Frontend component library | Production evidence | likely | Element Plus; primary blue is still the default in admin surfaces |

## Colors

### Light theme

| Token | Value | Intended use |
|---|---|---|
| `{colors.canvas}` | `#F2EFE9` | Page canvas, warm paper |
| `{colors.canvas-soft}` | `#F7F4EE` | Subtle alternate bands |
| `{colors.surface}` | `#FFFFFF` | Cards, panels, header user chip |
| `{colors.surface-soft}` | `#FBF9F4` | Pills, inset rows, secondary cards |
| `{colors.ink}` | `#071A33` | Display headings, key numerals, footer background |
| `{colors.text}` | `#1F2937` | Body copy on light surfaces |
| `{colors.muted}` | `#5B6472` | Secondary body text (accessible replacement for mockup `#6B7280`) |
| `{colors.faint}` | `#8A919C` | Decorative captions, metadata; never essential copy |
| `{colors.hairline}` | `#E8E2D7` | Strong 1px dividers |
| `{colors.hairline-soft}` | `#F0ECE4` | Default card and table dividers |
| `{colors.primary}` | `#C9A45B` | Gold fills, chart lines, decorative rules |
| `{colors.primary-hover}` | `#D8B775` | Hover state of gold fills |
| `{colors.on-primary}` | `#201507` | Text/icons on gold fills |
| `{colors.accent-ink}` | `#7A5A1F` | Gold-toned text on light surfaces; accessible gold text |
| `{colors.gold-soft}` | `#F6EEDC` | Event/rank tag background |
| `{colors.success}` | `#1F7A5A` | Wins, positive trends |
| `{colors.danger}` | `#B5122E` | Losses, negative trends, destructive actions |
| Hero start | `#0A2442` | Hero and player-card gradient start |
| Hero middle | `#071A33` | Hero gradient middle |
| Hero end | `#041020` | Hero gradient end |
| On hero | `#FFFFFF` | Text on the hero |

### Dark theme

Dark theme uses the same semantic token names, overridden under `html.dark` / `html[data-portal-theme="dark"]`. The table lists the override values.

| Token | Value | Intended use |
|---|---|---|
| `{colors.canvas}` dark | `#0B1018` | Page canvas |
| `{colors.canvas-soft}` dark | `#0E141D` | Alternate bands |
| `{colors.surface}` dark | `#131B26` | Cards, panels |
| `{colors.surface-soft}` dark | `#101823` | Pills, inset rows |
| `{colors.surface-elevated}` dark | `#182231` | Elevated rows / hover surfaces |
| `{colors.ink}` dark | `#F3F6FA` | Display headings, key numerals |
| `{colors.text}` dark | `#D5DDE8` | Body copy |
| `{colors.muted}` dark | `#8D99A9` | Secondary text |
| `{colors.faint}` dark | `#5F6B7B` | Decorative metadata only |
| `{colors.hairline}` dark | `#243041` | Strong dividers |
| `{colors.hairline-soft}` dark | `#1B2533` | Default dividers |
| `{colors.primary}` dark | `#D4AF6A` | Gold fills, chart lines |
| `{colors.on-primary}` dark | `#201507` | Text on gold |
| `{colors.accent-ink}` dark | `#E8CE97` | Gold-toned text on dark |
| `{colors.gold-soft}` dark | `rgba(212, 175, 106, .12)` | Tag background |
| `{colors.success}` dark | `#5FD3A5` | Wins, positive trends |
| `{colors.danger}` dark | `#F0758B` | Losses, negative trends |
| Hero start | `#0A1B30` | Hero gradient start |
| Hero middle | `#06101D` | Hero gradient middle |
| Hero end | `#030810` | Hero gradient end |
| Header background | `rgba(11, 16, 24, .92)` | Sticky header, blurred |
| Header border | `rgba(255, 255, 255, .08)` | Header hairline |
| Chart grid | `rgba(255, 255, 255, .08)` | Chart grid |
| Chart fill | `rgba(212, 175, 106, .13)` | Area fill under the line |

### Accessibility rules for gold

`#C9A45B` on `#F2EFE9` is not accessible for small text. Use it for fills, borders, chart strokes and large decorative numerals only. For gold-toned text on light surfaces use `{colors.accent-ink}` (`#7A5A1F`). On dark surfaces `#D4AF6A` / `#E8CE97` may be used for text with normal text sizes.

Never use `#9CA3AF`-style faint gray for essential copy. The canonical light secondary text is `{colors.muted}` (`#5B6472`); the faint token is for timestamps and decorative metadata.

### Data visualization palette

When a chart needs more than one series, use these six tones in order. Do not introduce arbitrary chart colors.

| Series | Light | Dark | Typical use |
|---|---|---|---|
| `chart-1` | `#C9A45B` | `#D4AF6A` | Primary metric, gold series |
| `chart-2` | `#3D7FB5` | `#89B9DE` | Secondary series / cup event |
| `chart-3` | `#2F7F7A` | `#7FD1C8` | Tertiary series / invitational event |
| `chart-4` | `#B87A4B` | `#E0B28C` | Fourth series / bronze rank |
| `chart-5` | `#7C8A9C` | `#AEB8C4` | Neutral, “other”, reference |
| `chart-6` | `#12365F` | `#5E86A8` | Deep navy comparison series |

Chart rules:

- Maximum 6 series. Aggregate the rest into “其他” with `chart-5`.
- Gold is series 1. Do not use green/red for categorical series; reserve them for win/loss and positive/negative semantics.
- Line charts: maximum 30 points per series; show a tooltip and a summary sentence for screen readers.
- Bar charts: maximum 12 categories; sort intentionally (time, value or rank), not alphabetically by accident.
- Donut/pie: only for composition metrics (for example hit types: singles / doubles / triples / home runs); never for rates such as AVG, OBP or ERA. Put the total or the leading share in the center, not a decorative number.
- Radar: approved only for the player “总体评价” (overall evaluation) section. Use one card per domain: batting and defense are always shown together; pitchers get a third pitching card. Never use tabs or a switcher to hide one of them. A radar must show the raw value and percentile for every axis, plus the comparison scope and sample size. Do not use radar for time series, rankings over time or arbitrary dashboards.
- Scatter/heatmap are not approved chart types yet. Do not add them without a design decision.
- Implementation variables are `--bs-chart-1` … `--bs-chart-6`; use them in order and do not introduce local series hex values.

#### Overall evaluation radar

The approved player radar uses five axes per domain, in this fixed order:

| Card | Axes | Domain color |
|---|---|---|
| Batting | AVG, OBP, SLG, RBI, SB | `chart-6` (deep navy) |
| Defense | FPCT, PO/G, A/G, DP/G, RF/9 | `{colors.primary}` gold |
| Pitching | ERA, WHIP, K/9, BB/9, IP | `chart-3` (teal) |

- Normalize each axis to a `0–100` percentile within the selected represented team and event. Do not mix events or teams. Show the comparison scope (“同队同赛事”) and the number of players in the sample.
- Require at least three comparable players. Below that, show an empty state with the reason instead of inventing a shape.
- Lower-is-better metrics (ERA, WHIP, BB/9) must be reversed before plotting. State this in the section footnote. IP keeps baseball notation on the axis (`5.2` = 5⅔ innings); percentile math uses outs.
- At widths where three cards would fall below roughly 300px each, wrap to two columns + one rather than shrinking the chart labels. The final mobile breakpoint stacks all cards in domain order.
- Draw five concentric rings at 20% intervals with a 1px hairline; no ring labels are required. Fill the data polygon with the domain color at 15% opacity and a 2px outline; dots use the surface color with a 2px domain stroke.
- Use the domain color as a 3px card edge, not as a background wash. The score and grade sit in the card header; the strongest and weakest axes are named below the chart.
- The batting and defense cards appear side by side on desktop and stack on mobile. All cards remain visible simultaneously; no accordion, carousel or tab. Pitching is appended as a third card when the player has pitching data.
- Defense is position-sensitive. Keep the scope note visible and do not imply that PO/G or A/G compare like-for-like across every position.

## Typography

| Style | Family | Size / line height | Usage |
|---|---|---|---|
| `{typography.display-xl}` | Serif | 64 / 1 | Player name in the hero |
| `{typography.display-lg}` | Serif | 48 / 1.05 | Large editorial numbers or secondary hero |
| `{typography.heading-lg}` | Serif | 28 / 1.2 | Page title, rank-band title |
| `{typography.heading-md}` | Serif | 22 / 1.25 | Panel headings on the personal page |
| `{typography.heading-sm}` | Sans | 16 / 1.3 | Dense dashboard panel headings |
| `{typography.body-lg}` | Sans | 16 / 1.7 | Intro text |
| `{typography.body-md}` | Sans | 14 / 1.65 | Default body |
| `{typography.body-sm}` | Sans | 12 / 1.6 | Tables, secondary rows |
| `{typography.caption}` | Sans | 11 / 1.5 | Metadata, event tags, links |
| `{typography.label}` | Sans | 10 / 1.4, tracking 1.2px | Uppercase kickers only |
| `{typography.stat-xl}` | Numeric | 48 / 1 | Hero snapshot, rank band |
| `{typography.stat-lg}` | Numeric | 32 / 1 | KPI values, rank values |
| `{typography.stat-md}` | Numeric | 22 / 1 | Table numerals |
| `{typography.button-md}` | Sans | 13 / 1, weight 600 | Buttons |

Production minimum is 11px for readable text and 10px for uppercase labels. The 7–9px micro type in the mockups is a design device, not a production size.

Serif is reserved for portal/dashboard display headings and the player name. Do not use serif in admin tables, forms, filters or dense operational screens.

## Layout

- Portal header height: `68px`.
- Dashboard content max width: `1280px`; wide portal content may use `1440px`.
- Desktop page padding: `32px`; mobile: `16px`.
- Dashboard grid: 12 columns, `20px` gap.
- Section rhythm: `56px` between major sections; `20px` between cards.
- Scope bar sits directly under the page head and above the hero.
- Hero on desktop: 2-column split (identity / player card or snapshot); on mobile it stacks.
- Rank band: 4 equal columns on desktop, 2×2 on mobile.
- Dashboard module grid: one 12-column CSS Grid on desktop. Module widths default to `12` / `8` / `4` from the module catalog but can be resized between each module's `minSpan` and `12`; height snaps to `10px` rows and never shrinks below rendered content. Order comes from the user's module config; do not use dense packing.
- Touch targets: at least `44px` on mobile.

Breakpoints: the dashboard switches from the 12-column module grid to a single column at `980px`; phone typography and spacing tighten at `560px`. The page content max width is `1280px`.

## Elevation & Depth

Light theme:

- `--bs-shadow-sm`: `0 10px 30px rgba(7, 26, 51, .045)`
- `--bs-shadow-md`: `0 18px 48px rgba(7, 26, 51, .08)`
- `--bs-shadow-hero`: `0 26px 60px rgba(0, 0, 0, .20)`

Dark theme:

- `--bs-shadow-sm`: `0 10px 30px rgba(0, 0, 0, .24)`
- `--bs-shadow-md`: `0 18px 48px rgba(0, 0, 0, .36)`
- `--bs-shadow-hero`: `0 26px 60px rgba(0, 0, 0, .45)`

Borders are preferred over shadows for dense data surfaces. Use a 1px `{colors.hairline-soft}` border on every panel; use the stronger `{colors.hairline}` only for structural divisions.

The hero stays dark in both themes. In dark mode it gets a 1px `rgba(255, 255, 255, .07)` hairline so it does not disappear into the canvas.

## Shapes

- Panels: `{rounded.xl}` `18px`.
- Hero and photo cards: `{rounded.2xl}` `24px`.
- Player card: `{rounded.lg}` `12–15px`; use 15px for the card object.
- Data cards and inset blocks: `10–12px`.
- Controls and pills: `6–8px`; fully rounded only for avatars, badges and segmented pills.
- Avatar: `{rounded.full}`.
- Avoid mixing more than two radii in one component group.

## Components

### Site header
Sticky, 68px high, canvas background at 92% opacity with backdrop blur, 1px header hairline. Nav links 12px; active link gets a 2px gold underline. User chip is a white/dark surface pill with a 30px avatar, gold ring and chevron.

### Page head
Breadcrumb in caption; page title in `{typography.heading-lg}`; actions on the right. Do not add a second hero above this.

### Scope bar
A single-line surface bar with a gold "数据范围" label, two scope pills ("全部赛事", "全部代表球队"), a short hint and a right-aligned summary ("代表 2 支球队 · 3 项赛事"). It is the canonical place to switch event/team scope. It must wrap gracefully on tablet.

### Hero
Deep navy gradient with the player name in serif, short intro, tags and team strip. In V3 the right side holds the player card stack. One gold primary CTA and one ghost CTA. Do not use a white hero in light mode.

### Player card
The signature object: dark navy card, gold hairline, inner hairline frame, monogram avatar, player name, team line and three headline stats. It is decorative but must be built from real player data. It is not a generic "image card".

### Dashboard modules
The nine configurable modules and their default / minimum widths are `队内坐标` (12/6), `总体评价` (12/8), `生涯里程碑` (12/8), `数据表现` (8/6), `代表球队` (4/4), `接下来的比赛` (8/6), `关于我` (4/4), `最近战报` (8/6), `快捷入口` (4/4). The default order follows the approved V3 page; default height is auto.

- Hidden modules are removed from the grid; the page header, hero, identity strip and scope bar stay fixed.
- Reordering is visual and follows the saved array. Cards do not pack densely; if a card is taller than the card beside it, the next row starts after the taller card. This blank space is intentional and approved.
- Width snaps to the 12-column grid; height snaps to `10px` rows and `0` means auto. A module can never be shorter than its rendered content. Enlarging a module pushes later rows down; it does not overlap neighbours.
- All modules can be hidden. Show a panel empty state with “所有内容模块已隐藏” and a primary “去设置” action; keep the top-right “数据设置” entry visible.
- The module settings drawer lists every module with a drag handle, label, size badge (`N 列 · 高度自适应` / `N 列 · 至少 N px`), up/down buttons and a switch. Gold is the on state for both themes. Hidden rows dim only the drag handle; labels stay at full contrast and the switch remains fully interactive. Use 44×44px hit areas for the handle, arrows and switch wrapper at `≤980px`.
- **Layout editing is desktop-only.** Show a top-right “编辑布局” button at `>980px`; entering edit mode shows a gold instruction bar with “完成”. The hero, identity strip and scope bar stay fixed, and scope selects are disabled.
- **Selection:** in edit mode, click any module to select it; do not require a long press. Show a 2px gold frame with soft outer glow and no layout shift. Module content is non-interactive while editing; clicking blank grid space clears the selection. Pressing a handle must not select text or trigger module links. Entering edit mode focuses the first module; leaving it returns focus to “编辑布局”.
- **Keyboard:** in edit mode each module is tabbable and shows a 2px accent-ink focus ring. Enter/Space selects it and focuses the corner handle (both axes); arrow keys on a focused handle resize by one column/row (left/right = width, up/down = height, corner = both). The selected module itself also accepts arrow keys. Save status uses `role="status"` / `aria-live="polite"`.
- **Resize handles:** right edge changes width, bottom edge changes height, bottom-right corner changes both. Anchor the top-left corner. Keep the visible gold bar compact but give every handle a ≥24×24px transparent hit area. Show the module name, current `N 列 × M 行`, “移动”, “重置尺寸” and close in a compact surface toolbar inside the frame’s top-right. Drag previews live; release auto-saves.
- **Move:** drag the toolbar’s “移动” control to reorder. The module lifts (surface shadow + slight opacity) and swaps live as the pointer enters another module; the frame follows; dragging back across a module reverses the swap. Use pointer capture so a release outside the window still ends the drag. Near the viewport top/bottom edge, auto-scroll up to 12px per frame. Release saves the new array order; Esc or leaving edit mode cancels and restores the pre-drag order. ArrowUp/ArrowDown on the focused “移动” control reorders one visible slot; the settings drawer up/down buttons remain the full fallback.
- **Current event context:** the “接下来的比赛” module shows a compact current-event row (name, status, date range) above the featured game. The featured game prefers the selected event’s next game; the list below remains cross-event. The API caps the combined schedule at 6 upcoming games, so when the selected event’s next game is outside that cap, drop the “当前赛事优先” header promise, show the dashed note “当前赛事近期暂无未开赛比赛”, and let the featured card’s own event pill identify the fallback. Do not duplicate current event/next game above the hero.
- **Data definitions:** the rank band has a persistent one-line 口径 note. Rank, overall evaluation and performance headers have a 24px info button that opens a click tooltip; the button must be keyboard focusable and tappable on mobile.
- **Readability:** dashboard labels use a 10px minimum and key captions 11px; do not reintroduce 7–9px production text. Keep the scope hint short.
- **Failure:** save failure restores the pre-drag size/order and shows an error toast. Reset size restores the catalog default width and auto height. Mobile width never exposes the editor, handles or move control and ignores saved widths/heights.
- Do not expose free positioning in this phase; canvas drag-reorder is supported only inside desktop layout edit mode.

### Career milestones
A full-width module in the default order after overall evaluation. It combines career cumulative progress and a recent milestone timeline. Its data loads lazily from `GET /account/clubhouse/milestones` only when the module is enabled; hiding it must not read the full career log.

- Scope is all-tenant career across game modes: every event and represented team, ranking-included games only. The module is unaffected by the top scope bar. Label it “全租户生涯 · 跨赛事与球队” and note “统计自系统记录 · 仅计入排行的比赛”.
- Users select 1–6 cumulative metrics in settings. Default: H, RBI, SB, G, IP, SO. Available catalog also includes HR, R, W, SV and NP.
- Each metric card shows current value, next round-number target, a 3px progress track and remaining amount. IP uses baseball notation (`42.1` = 42⅓ innings); progress is computed from outs.
- The timeline lists up to 20 latest achieved milestones, newest first (earliest at the bottom), with date, title, event and opponent. Summary shows total achieved count. First-hit / first-home-run / first-RBI / first-win / first-save use dedicated titles.
- Timeline rail: a 1px dashed gold line connects the small gold dots from the first dot centre to the last. The list has a max height of 238px and scrolls; hide the native scrollbar and use an 8px custom gold track/thumb on the right. The thumb is draggable; wheel, trackpad and touch scrolling work. The list is keyboard-focusable with a visible focus ring. Header and footnote stay fixed outside the scroll area.
- Desktop: 2-column inner layout (metric grid + timeline rail), 3 metric columns × 2 rows. Mobile: metric cards 2-up, timeline stacked below with the same scroll rail.
- The settings drawer includes “生涯里程碑 · 最多 6 项” with a multi-select; 1–6 unique metrics are required. Selecting metrics never changes the module order or visibility.

### Settings drawer
The module list sits above “表现图” and “队内排名”. Use `sortablejs` on the drag handle only; keep up/down buttons as the keyboard/touch fallback. Show “已启用 N / 9”. Saving persists module order, enabled state, width and height together with the existing metric choices; size editing itself happens in the page’s “编辑布局” mode, not in this drawer. Hiding a module never clears its metric or size configuration.

### Panels
White/dark surface, 18px radius, 1px soft hairline, low shadow. Header: uppercase gold kicker, serif title, optional "切换/完整数据" link. Do not nest more than one panel level.

### Rank band
Full-width surface with four rank features. Each feature has a large numeric rank, a metric name, a supporting note and a 3px progress track. The band header must name the team and event scope.

### Overall evaluation cards
Full-width module in the default order directly below the rank band. The header names the section “总体评价” and the current team/event scope. Cards use a 14px radius inside the 18px panel, a 3px domain-colored left edge, a numeric overall score with letter grade, a five-axis radar, a compact raw-value matrix, strongest/weakest callouts, and a sample footer. Batting and defense are never separated into tabs. On mobile all cards stack in the same order. Use `{colors.surface-soft}` for the metric strip; do not fill the whole card with the domain color.

### Buttons
- `{components.button-primary}`: gold gradient fill, dark brown text. One per section.
- `{components.button-primary-hover}`: lighter gold.
- `{components.button-ghost}`: transparent on hero, white text, 1px white 18% border.
- Focus-visible: 2px gold outline with 2px offset.
- Disabled: 40% opacity, no gradient.

### Event tags
Three tones only:
- League: `{colors.gold-soft}` background, `{colors.accent-ink}` text.
- Cup: `rgba(61,127,181,.10)` background, `#3D7FB5` text; dark `rgba(89,185,222,.10)` / `#89B9DE`.
- Invitational: `rgba(47,127,122,.10)` background, `#2F7F7A` text; dark `rgba(127,209,200,.10)` / `#7FD1C8`.

Do not invent a fourth tag color. Use the same tone for the same event type everywhere.

### Data table
Header 11–12px, uppercase tracking; rows 12–14px; 1px `{colors.hairline-soft}` row separator; right-aligned numeric columns. Result badges use green/red soft backgrounds; never use pure green/red fills.

Clubhouse tables must override the portal's blue hover/current-row backgrounds: hover uses `{colors.surface-soft}` and the current row uses `{colors.gold-soft}`. The scoped override lives in `design-tokens.css` because the portal global rule uses `!important`.

### Charts
Gold line on a transparent panel. Area fill and grid lines use `--bs-chart-fill` / `--bs-chart-grid`:
- Light: fill `rgba(201,164,91,.16)`, grid `rgba(7,26,51,.08)`.
- Dark: fill `rgba(212,175,106,.13)`, grid `rgba(255,255,255,.08)`.

Reference lines are dashed and faint. Axis labels use `{typography.caption}`; do not use 8px labels.

### Footer
Deep ink bar, 64px, white 68% text, 10–11px. In dark mode use `#060A11` with a top hairline.

## Motion & State Feedback

- Motion: `160ms` fast / `240ms` base, `ease-out`; hover and theme transitions only. Respect `prefers-reduced-motion: reduce` by removing non-essential transitions.
- Focus-visible: 2px `{colors.accent-ink}` outline on light surfaces, 2px `{colors.primary}` on dark surfaces, with a 2px offset. Never remove the focus ring.
- Selected state: use a gold left border plus a text label or check icon; do not rely on color alone.
- Loading: use skeleton blocks on `{colors.surface-soft}`; do not show invented numbers or zeros before data arrives.
- Empty: use `{colors.muted}` copy and one gold link; never fabricate stats, milestones or rankings.
- Error: use `{colors.danger}` for text/icon and a retry action; keep the rest of the dashboard usable.
- Success/positive: use `{colors.success}` with a W/L label or icon; color is not the only signal.
- Disabled: 40% opacity, no gradient, `cursor: not-allowed`; disabled controls still need readable labels.

## CSS Variable Mapping

| DESIGN token | CSS variable | Light | Dark |
|---|---|---|---|
| `{colors.canvas}` | `--bs-canvas` | `#F2EFE9` | `#0B1018` |
| `{colors.canvas-soft}` | `--bs-canvas-soft` | `#F7F4EE` | `#0E141D` |
| `{colors.surface}` | `--bs-surface` | `#FFFFFF` | `#131B26` |
| `{colors.surface-soft}` | `--bs-surface-soft` | `#FBF9F4` | `#101823` |
| `{colors.surface-elevated}` | `--bs-surface-elevated` | `#F6F1E7` | `#182231` |
| `{colors.ink}` | `--bs-ink` | `#071A33` | `#F3F6FA` |
| `{colors.text}` | `--bs-text` | `#1F2937` | `#D5DDE8` |
| `{colors.muted}` | `--bs-text-muted` | `#5B6472` | `#8D99A9` |
| `{colors.faint}` | `--bs-text-faint` | `#8A919C` | `#5F6B7B` |
| `{colors.hairline}` | `--bs-hairline` | `#E8E2D7` | `#243041` |
| `{colors.hairline-soft}` | `--bs-hairline-soft` | `#F0ECE4` | `#1B2533` |
| `{colors.primary}` | `--bs-accent` | `#C9A45B` | `#D4AF6A` |
| `{colors.primary-hover}` | `--bs-accent-hover` | `#D8B775` | `#E8CE97` |
| `{colors.gold-soft}` | `--bs-accent-soft` | `#F6EEDC` | `rgba(212,175,106,.12)` |
| `{colors.on-primary}` | `--bs-on-accent` | `#201507` | `#201507` |
| `{colors.accent-ink}` | `--bs-accent-ink` | `#7A5A1F` | `#E8CE97` |
| `{colors.success}` | `--bs-success` | `#1F7A5A` | `#5FD3A5` |
| `{colors.danger}` | `--bs-danger` | `#B5122E` | `#F0758B` |
| `{colors.hero-start}` | `--bs-hero-start` | `#0A2442` | `#0A1B30` |
| `{colors.hero-mid}` | `--bs-hero-mid` | `#071A33` | `#06101D` |
| `{colors.hero-end}` | `--bs-hero-end` | `#041020` | `#030810` |

These variables are declared once in the shared frontend style layer. Components consume them and must not re-declare theme colors locally.

## Theme Switching

Canonical selectors in production:

- `:root` / `html` for light.
- `html.dark` and `html[data-portal-theme="dark"]` for dark. The existing `portalTheme` store already toggles `html.dark`; keep that behavior.
- Design previews may use `[data-theme="dark"]`; production components must not depend on the preview selector.

Rules:

1. Define semantic CSS variables once, under `:root` and the dark selectors.
2. Never hard-code a light-theme hex inside a component.
3. Map Element Plus variables within the portal/dashboard scope only:
   - `--el-bg-color` → surface
   - `--el-bg-color-page` → canvas
   - `--el-text-color-primary` → ink
   - `--el-text-color-regular` → text
   - `--el-border-color` → hairline
   - `--el-color-primary` → primary (gold) for portal surfaces only
4. Keep the admin console on its current Element Plus blue until a separate decision. The gold primary is a portal/dashboard brand accent, not a replacement for all admin actions.
5. Preserve the existing `light / dark / auto` theme switch and localStorage key. Do not introduce a second theme storage key.
6. Teleported Element Plus overlays (ElMessage, ElMessageBox, Select dropdowns, Tooltip) mount under `body` and do not inherit `.clubhouse-page` tokens. Either set `teleported=false` for clubhouse-owned overlays or map the needed tokens explicitly on the overlay class. Do not solve this by declaring the clubhouse Element Plus bridge globally.

## Do's and Don'ts

- Do use `{colors.canvas}` + `{colors.surface}` rather than pure white/black.
- Do keep the hero deep navy in both themes.
- Do use serif only for editorial display headings.
- Do use `{colors.accent-ink}` for small gold text on light.
- Do keep the same event-tag tone for the same event type.
- Do use a text label or icon alongside green/red semantic colors.
- Do render dashboard modules from `moduleCatalog` + saved order; do not hard-code a second span/label map in the page.
- Do keep the top-right “数据设置” entry available even when data modules are hidden.
- Do keep size editing desktop-only; mobile must never show resize handles or apply saved heights.
- Do verify both light and dark screenshots for every UI change.
- Don't hard-code hex values in Vue components or page CSS.
- Don't use `#C9A45B` for small text on warm paper.
- Don't put more than one primary gold CTA in the same card.
- Don't add new accent hues (purple, cyan, orange) without a design decision.
- Don't apply the portal serif/gold treatment to dense admin forms and tables.
- Don't use 7–9px text in production.
- Don't use the legacy `--bsball-red` / `--bsball-navy` values for new work; they are misnamed and visually stale.
- Don't use color alone to indicate selected, win/loss, error or success states.
- Don't invent stats, rankings, milestones or scout notes when the API has no data.

## Responsive Behavior

- **≥1200px**: 12-column dashboard; hero 2 columns; rank band 4 columns; player card stack on the right; evaluation radar cards 2- or 3-up; configurable modules use their saved width/height in saved order; layout editing is available.
- **981–1199px**: reduce page padding; hero right column narrows; scope hint may hide; evaluation radar cards retain the same column count if each card remains at least 300px wide; saved module widths and heights remain active; layout editing remains available.
- **≤980px**: single column; every dashboard module spans the full row in saved order; saved width and height are ignored, height is content-driven; hide “编辑布局”, the instruction bar and selection handles; hero stacks; rank band 2×2; evaluation radar cards stack; scope pills wrap; chart and data matrix stack; schedule rows become two-line cards; result cards stack; touch targets ≥44px.
- **≤560px**: phone typography and spacing; settings module hit targets remain 44×44px from the ≤980px rule.
- The page must never scroll horizontally at 390px.

## Adoption Rules for Future Frontend Changes

Any future frontend change must answer these questions in its plan:

1. Does this screen use the semantic tokens above, or does it introduce a new color/size?
2. Does the change work in both light and dark themes?
3. Does it stay inside the portal/dashboard scope, or does it need an admin-specific exception?
4. Does it touch the shared header, footer, buttons, cards or tables? If yes, align them with this document in the same change.
5. Are there screenshots at 1440 desktop and 390 mobile for both themes?
6. Is there a visual-regression baseline or a design QA checklist update?

Phased adoption:

- **Phase 1**: DESIGN.md + personal dashboard (V3) + mobile adaptation.
- **Phase 2**: portal shell (header, nav, footer, buttons, cards) as those files are touched.
- **Phase 3**: admin console optional, separate review; keep operational clarity first.

## Conflicts and Open Decisions

- **Legacy `--bsball-*` tokens**: `--bsball-red` is actually navy (`#051e45`) and `--bsball-navy` is actually red (`#bf0d3e`). They conflict with the approved palette and must not be used for new work. A migration aliases them to the new semantic tokens or removes them when the portal shell is touched.
- **Element Plus primary blue**: the admin console and some shared controls still use blue. Decision needed: keep blue in admin, use gold only in portal/dashboard scope (recommended).
- **Card radius**: existing `--portal-card-radius: 0` conflicts with the 18px panel radius. Migrate portal cards when touched; do not force a global restyle in one PR.
- **Header height**: existing 60px vs 68px in V3. Adopt 68px on the portal shell when touched; admin header can remain.
- **Micro typography**: mockups use 7–9px labels. Production must use 10px labels / 11px captions minimum.
- **Serif availability**: Songti/Noto Serif SC fallbacks are good on macOS/Windows; verify Android/WebView rendering and bundle only if needed.
- **Data contract**: multi-team / multi-event scope and ranking computation are a separate backend/API decision; this document only fixes presentation.

## Known Gaps

- No production component screenshots yet; tokens are confirmed, component states are proposed.
- No automated contrast/token lint is configured.
- No dark-mode visual regression baselines.
- Admin console adoption is intentionally undecided.
- Module visual order uses CSS `order` while the DOM keeps the legacy main/side sequence (`performance → schedule → recentGames → teams → about → quickLinks`). Keyboard and screen-reader traversal follows that DOM sequence, not the saved visual order, including at the default setting. A future module renderer should flatten modules into configured DOM order; until then this is an accepted accessibility limitation for this phase.
- Career timeline starts at the earliest game recorded in the system; historical games outside the system cannot be reconstructed.

package com.liktun.japanesehabitlock.domain.immersion

import com.liktun.japanesehabitlock.domain.Roadmap

/**
 * One phrase in every form the interface might need to show it.
 *
 * Kept as one object per phrase rather than three parallel string tables so a
 * translation can never drift out of sync with its reading.
 *
 * [furigana] is the kana reading of [japanese] as a whole. Per-character ruby would be
 * more faithful, but it needs a morphological analyser to be correct and a wrong ruby
 * position teaches the wrong reading — a single reading above the phrase is honest and
 * cheap. Phrases with no kanji leave it null.
 */
data class Phrase(val english: String, val japanese: String, val furigana: String? = null) {

  /** The text to display at [level], ignoring furigana (which the UI renders separately). */
  fun textAt(level: ImmersionLevel, japaneseFrom: ImmersionLevel): String =
    if (level.includes(japaneseFrom)) japanese else english

  /** The reading to show above [textAt], or null when it should not be shown. */
  fun rubyAt(level: ImmersionLevel, japaneseFrom: ImmersionLevel): String? =
    furigana.takeIf { level.includes(japaneseFrom) && level.showsFurigana }
}

/**
 * Every user-facing phrase in the checklist, in English and Japanese.
 *
 * Japanese here is written the way a learner app should write it: plain, short, and
 * using the vocabulary the roadmap's own tools (WaniKani, Bunpro) actually teach.
 * It is not trying to be literary.
 */
object ImmersionStrings {

  // ---- section labels: the first thing to switch, at LABELS ----
  val TODAY = Phrase("Today", "今日", "きょう")
  val THIS_WEEK = Phrase("This week", "今週", "こんしゅう")
  val REQUIRED = Phrase("Required", "必須", "ひっす")
  val OPTIONAL = Phrase("Optional", "任意", "にんい")
  val WEEKLY_CHECKPOINT = Phrase("Weekly checkpoint", "週の確認", "しゅうのかくにん")
  val NEVER_BLOCKS = Phrase("Never blocks", "制限なし", "せいげんなし")

  // ---- gate state ----
  val LOCKED = Phrase("Locked", "施錠中", "せじょうちゅう")
  val UNLOCKED = Phrase("Unlocked", "解錠", "かいじょう")
  val OPEN_ACTION = Phrase("Open", "開く", "ひらく")
  val WEEK = Phrase("Week", "週", "しゅう")
  val DONE = Phrase("done", "完了", "かんりょう")

  // ---- phases ----
  val PHASE_SHADOWING = Phrase("Shadowing", "シャドーイング")
  val PHASE_SELF_TALK = Phrase("Self-talk", "独り言", "ひとりごと")
  val PHASE_SPEAKING = Phrase("Speaking", "会話", "かいわ")

  /**
   * Task titles, keyed by the stable ids in [Roadmap].
   *
   * Keyed by id rather than held on [com.liktun.japanesehabitlock.domain.RoadmapTask]
   * so the domain model stays free of presentation concerns and this table can be
   * translated without touching the roadmap.
   */
  val TASK_TITLES: Map<String, Phrase> =
    mapOf(
      Roadmap.ID_WANIKANI to Phrase("WaniKani reviews cleared", "漢字の復習", "かんじのふくしゅう"),
      Roadmap.ID_BUNPRO to Phrase("Bunpro grammar reviews done", "文法の復習", "ぶんぽうのふくしゅう"),
      Roadmap.ID_SHADOWING to Phrase("Shadowing session", "シャドーイング"),
      Roadmap.ID_IMMERSION to Phrase("Immersion reading / listening", "多読・多聴", "たどく・たちょう"),
      Roadmap.ID_AI_TUTOR to Phrase("Talk with an AI teacher", "AIの先生と話す", "エーアイのせんせいとはなす"),
      Roadmap.ID_SELF_TALK to Phrase("Self-talk production practice", "独り言の練習", "ひとりごとのれんしゅう"),
      Roadmap.ID_CONVERSATION to Phrase("iTalki / HelloTalk", "会話の相手", "かいわのあいて"),
    )

  /** Task detail lines. Switched last, at [ImmersionLevel.FULL_FURIGANA]. */
  val TASK_DETAILS: Map<String, Phrase> =
    mapOf(
      Roadmap.ID_WANIKANI to Phrase("Review queue down to zero.", "復習をゼロにする。", "ふくしゅうをゼロにする。"),
      Roadmap.ID_BUNPRO to Phrase("Clear the grammar queue.", "文法の復習を終わらせる。", "ぶんぽうのふくしゅうをおわらせる。"),
      Roadmap.ID_SHADOWING to
        Phrase(
          "15-20 min. Stay on the same clip for 3-4 days running.",
          "十五分から二十分。同じ音声を三、四日続ける。",
          "じゅうごふんからにじゅっぷん。おなじおんせいをさん、よっかつづける。",
        ),
      Roadmap.ID_IMMERSION to
        Phrase("NHK Easy News, or anime with Japanese subtitles.", "やさしい日本語のニュース、または字幕付きのアニメ。", "やさしいにほんごのニュース、またはじまくつきのアニメ。"),
      Roadmap.ID_AI_TUTOR to
        Phrase(
          "10-15 min out loud in Japanese. Ask it to correct you, not just chat.",
          "十分から十五分、声に出して話す。直してもらうこと。",
          "じゅっぷんからじゅうごふん、こえにだしてはなす。なおしてもらうこと。",
        ),
      Roadmap.ID_SELF_TALK to
        Phrase("Narrate your day out loud. Answer questions about your clip in your own words.", "今日のことを声に出して話す。", "きょうのことをこえにだしてはなす。"),
      Roadmap.ID_CONVERSATION to Phrase("A task, not a hard requirement.", "義務ではない。", "ぎむではない。"),
    )

  /** Gate explanation lines, which depend on state rather than on a task. */
  fun gateSummary(done: Int, total: Int): Phrase =
    Phrase(
      english = "$done of $total done",
      japanese = "$total 個中 $done 個完了",
      furigana = "こちゅうかんりょう",
    )

  /** The phrase for a task title, falling back to the roadmap's English if untranslated. */
  fun taskTitle(id: String, fallback: String): Phrase =
    TASK_TITLES[id] ?: Phrase(fallback, fallback)

  /** The phrase for a task detail, falling back to the roadmap's English if untranslated. */
  fun taskDetail(id: String, fallback: String): Phrase =
    TASK_DETAILS[id] ?: Phrase(fallback, fallback)
}

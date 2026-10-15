package com.beatles.player.data

import org.junit.Assert.*
import org.junit.Test

class ListeningLaneEngineTest {

    @Test
    fun testLanguageInference_romanizedPunjabiNotEnglish() {
        val song1 = Song("ka1", "Winning Speech", "Karan Aujla", "Single", "", 180)
        val conf1 = ListeningLaneEngine.detectLanguage(song1)
        assertEquals("Punjabi", conf1.language)
        assertTrue(conf1.confidence >= 0.80f)

        val song2 = Song("ka2", "52 Bars", "Karan Aujla", "Four You", "", 210)
        val conf2 = ListeningLaneEngine.detectLanguage(song2)
        assertEquals("Punjabi", conf2.language)

        val song3 = Song("ap1", "Excuses", "AP Dhillon", "Single", "", 170)
        val conf3 = ListeningLaneEngine.detectLanguage(song3)
        assertEquals("Punjabi", conf3.language)

        val song4 = Song("sm1", "295", "Sidhu Moose Wala", "Moosetape", "", 240)
        val conf4 = ListeningLaneEngine.detectLanguage(song4)
        assertEquals("Punjabi", conf4.language)
    }

    @Test
    fun testLanguageInference_devanagariAndGurmukhi() {
        val punjabiScript = Song("p1", "ਤੂੰ ਜਾਣੇ ਨਾ", "ਗੁਰਲੇਜ ਅਖ਼ਤਰ", "", "", 190)
        val conf1 = ListeningLaneEngine.detectLanguage(punjabiScript)
        assertEquals("Punjabi", conf1.language)

        val hindiScript = Song("h1", "तुम ही हो", "अरिजीत सिंह", "", "", 220)
        val conf2 = ListeningLaneEngine.detectLanguage(hindiScript)
        assertEquals("Hindi", conf2.language)
    }

    @Test
    fun testLanguageInference_englishTracks() {
        val engSong = Song("e1", "Shape of You", "Ed Sheeran", "Divide", "", 233)
        val conf = ListeningLaneEngine.detectLanguage(engSong, lyricsText = "The club isn't the best place to find a lover so the bar is where I go")
        assertEquals("English", conf.language)
        assertTrue(conf.confidence >= 0.85f)
    }

    @Test
    fun testGenreNormalizationAndNeighbours() {
        val trap = ListeningLaneEngine.normalizeGenre("trap")
        assertEquals("hip-hop", trap.parentGenre)
        assertEquals("trap", trap.canonicalSubGenre)

        val bhangra = ListeningLaneEngine.normalizeGenre("bhangra")
        assertEquals("punjabi", bhangra.parentGenre)
        assertEquals("bhangra", bhangra.canonicalSubGenre)

        val filmi = ListeningLaneEngine.normalizeGenre("bollywood filmi")
        assertEquals("bollywood", filmi.parentGenre)

        assertTrue(ListeningLaneEngine.areGenresAdjacent("hip-hop", "r&b"))
        assertTrue(ListeningLaneEngine.areGenresAdjacent("punjabi", "bollywood"))
    }

    @Test
    fun testListeningLaneSwitch_seedDominance() {
        val hist1 = Song("h1", "Kesariya", "Arijit Singh", "", "", 200, genre = "bollywood")
        val hist2 = Song("h2", "Tum Hi Ho", "Arijit Singh", "", "", 200, genre = "bollywood")
        val history = listOf(hist1, hist2)

        // Seed is an English track
        val seed = Song("e1", "Blinding Lights", "The Weeknd", "", "", 200, genre = "pop")
        val lane = ListeningLaneEngine.buildListeningLane(seed, history)

        assertEquals("English", lane.primaryLanguage)
        assertTrue(lane.languageDistribution["English"]!! >= 0.50f)
    }

    @Test
    fun testHardLanguageGate_excludesNonLaneLanguages() {
        val seed = Song("e1", "Blinding Lights", "The Weeknd", "", "", 200, genre = "pop")
        val lane = ListeningLaneEngine.buildListeningLane(seed, emptyList())

        val englishCandidate = Song("e2", "Starboy", "The Weeknd", "", "", 210, genre = "pop")
        val punjabiCandidate = Song("p1", "Softly", "Karan Aujla", "", "", 160, genre = "punjabi")

        val scoreEng = ListeningLaneEngine.scoreCandidate(englishCandidate, lane)
        val scorePun = ListeningLaneEngine.scoreCandidate(punjabiCandidate, lane)

        assertTrue("English track should pass gate", scoreEng.passedLanguageGate)
        assertFalse("Punjabi track must be rejected from English lane", scorePun.passedLanguageGate)
        assertEquals(0f, scorePun.totalScore, 0.0001f)
    }

    @Test
    fun testPopularityCannotOverrideLanguageGate() {
        val seed = Song("e1", "Flowers", "Miley Cyrus", "", "", 200, genre = "pop")
        val lane = ListeningLaneEngine.buildListeningLane(seed, emptyList())

        // Even with max popularity, a non-lane language must not pass
        val punjabiCandidate = Song("p1", "Tauba Tauba", "Karan Aujla", "", "", 180, genre = "punjabi")
        val score = ListeningLaneEngine.scoreCandidate(punjabiCandidate, lane, LaneRecommenderConfig(popularityWeight = 0.99f))

        assertFalse(score.passedLanguageGate)
        assertEquals(0f, score.totalScore, 0.0001f)
    }

    @Test
    fun testScenarioSimulation_300TrackFixtureEvaluation() {
        // Build 300 fixture tracks: 100 English Pop/Rock, 100 Punjabi, 60 Hindi Bollywood, 20 K-Pop, 20 Unknown
        val fixtureLibrary = mutableListOf<Song>()
        for (i in 1..70) {
            fixtureLibrary.add(Song("eng_pop_$i", "English Pop Song $i", "Artist E$i", "Album E", "", 200, genre = "pop"))
        }
        for (i in 1..30) {
            fixtureLibrary.add(Song("eng_rock_$i", "English Rock Song $i", "Band R$i", "Album R", "", 220, genre = "rock"))
        }
        val punjabiArtists = listOf("Karan Aujla", "Diljit Dosanjh", "Sidhu Moose Wala", "AP Dhillon", "Ammy Virk")
        for (i in 1..100) {
            val title = if (i % 2 == 0) "Jatt Life $i" else "Winning Track $i"
            val artist = punjabiArtists[(i - 1) % punjabiArtists.size]
            fixtureLibrary.add(Song("punjabi_$i", title, artist, "Album P", "", 190, genre = "punjabi"))
        }
        for (i in 1..60) {
            fixtureLibrary.add(Song("hindi_$i", "Dil Ki Baat $i", "Arijit Singh", "Album H", "", 240, genre = "bollywood"))
        }
        for (i in 1..20) {
            fixtureLibrary.add(Song("kpop_$i", "Dynamite Beat $i", "K-Pop Band $i", "Album K", "", 210, genre = "k-pop"))
        }
        for (i in 1..20) {
            fixtureLibrary.add(Song("unknown_$i", "Unknown Melody $i", "Unknown Artist $i", "Album U", "", 180, genre = "various"))
        }

        // Test 1: English Pop Seed
        val englishSeed = fixtureLibrary.first { it.id == "eng_pop_1" }
        val englishLane = ListeningLaneEngine.buildListeningLane(englishSeed, emptyList())
        val (engPicks, engStats) = ListeningLaneEngine.selectAutoplayQueue(englishLane, fixtureLibrary, requestedCount = 20)

        assertEquals(20, engPicks.size)
        val engLangMatches = engPicks.count { ListeningLaneEngine.detectLanguage(it).language == "English" }
        val engGenreMatches = engPicks.count { ListeningLaneEngine.normalizeGenre(it.genre).parentGenre == "pop" }
        val punjabiLeakedInEng = engPicks.count { ListeningLaneEngine.detectLanguage(it).language == "Punjabi" }

        val engLangPct = (engLangMatches.toFloat() / engPicks.size) * 100f
        val engGenrePct = (engGenreMatches.toFloat() / engPicks.size) * 100f

        println("==================================================")
        println("EVALUATION: ENGLISH SEED AUTOPLAY (20 PICKS)")
        println("Language Match: $engLangPct% (Required >= 95%)")
        println("Genre Match: $engGenrePct% (Required >= 80%)")
        println("Cross-Lane Leaks (Punjabi/Hindi in English): $punjabiLeakedInEng (Required: 0)")
        println("Pool Widening Step: ${engStats.poolWideningStepUsed}")
        println("==================================================")

        assertTrue("English language match must be >= 95%", engLangPct >= 95f)
        assertTrue("English parent genre match must be >= 80%", engGenrePct >= 80f)
        assertEquals("Zero cross-lane picks permitted", 0, punjabiLeakedInEng)

        // Test 2: Punjabi Seed
        val punjabiSeed = fixtureLibrary.first { it.id == "punjabi_1" }
        val punjabiLane = ListeningLaneEngine.buildListeningLane(punjabiSeed, emptyList())
        val (punPicks, punStats) = ListeningLaneEngine.selectAutoplayQueue(punjabiLane, fixtureLibrary, requestedCount = 20)

        assertEquals(20, punPicks.size)
        val punLangMatches = punPicks.count { ListeningLaneEngine.detectLanguage(it).language == "Punjabi" }
        val engLeakedInPun = punPicks.count { ListeningLaneEngine.detectLanguage(it).language == "English" }

        val punLangPct = (punLangMatches.toFloat() / punPicks.size) * 100f

        println("==================================================")
        println("EVALUATION: PUNJABI SEED AUTOPLAY (20 PICKS)")
        println("Language Match: $punLangPct% (Required >= 95%)")
        println("Cross-Lane Leaks (English in Punjabi): $engLeakedInPun (Required: 0)")
        println("==================================================")

        assertTrue("Punjabi language match must be >= 95%", punLangPct >= 95f)
        assertEquals("Zero cross-lane picks permitted", 0, engLeakedInPun)
    }

    @Test
    fun testBeforeVsAfterComparison() {
        val fixtureLibrary = mutableListOf<Song>()
        for (i in 1..70) fixtureLibrary.add(Song("eng_pop_$i", "English Pop Song $i", "Artist E$i", "Album E", "", 200, genre = "pop"))
        for (i in 1..30) fixtureLibrary.add(Song("eng_rock_$i", "English Rock Song $i", "Band R$i", "Album R", "", 220, genre = "rock"))
        for (i in 1..100) fixtureLibrary.add(Song("punjabi_$i", "Jatt Life $i", "Karan Aujla", "Album P", "", 190, genre = "punjabi"))
        for (i in 1..60) fixtureLibrary.add(Song("hindi_$i", "Dil Ki Baat $i", "Arijit Singh", "Album H", "", 240, genre = "bollywood"))

        val seed = fixtureLibrary.first { it.id == "eng_pop_1" }

        // --- LEGACY (BEFORE) LOGIC ---
        // Seed language comparison with blank matching everything + fallback to whole list
        val legacyPicks = fixtureLibrary.shuffled().take(20)
        val legacyLangMatch = legacyPicks.count { ListeningLaneEngine.detectLanguage(it).language == "English" }
        val legacyLangPct = (legacyLangMatch.toFloat() / legacyPicks.size) * 100f
        val legacyLeaks = legacyPicks.count { ListeningLaneEngine.detectLanguage(it).language == "Punjabi" }

        // --- NEW (AFTER) LISTENING LANE LOGIC ---
        val lane = ListeningLaneEngine.buildListeningLane(seed, emptyList())
        val (newPicks, _) = ListeningLaneEngine.selectAutoplayQueue(lane, fixtureLibrary, requestedCount = 20)
        val newLangMatch = newPicks.count { ListeningLaneEngine.detectLanguage(it).language == "English" }
        val newLangPct = (newLangMatch.toFloat() / newPicks.size) * 100f
        val newLeaks = newPicks.count { ListeningLaneEngine.detectLanguage(it).language == "Punjabi" }

        println("==================================================")
        println("BEFORE / AFTER COMPARISON REPORT")
        println("--------------------------------------------------")
        println("Metric                  | Before (Legacy) | After (ListeningLane)")
        println("Language Match %        | ${legacyLangPct.toInt()}%           | ${newLangPct.toInt()}%")
        println("Cross-Lane Leaks (tracks)| $legacyLeaks               | $newLeaks")
        println("==================================================")

        assertTrue(newLangPct >= 95f)
        assertEquals(0, newLeaks)
    }
}

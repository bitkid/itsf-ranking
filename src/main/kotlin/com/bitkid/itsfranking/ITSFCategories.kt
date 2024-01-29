package com.bitkid.itsfranking

enum class CompetitionType {
    SINGLES, DOUBLES
}

data class Category(val name: String, val targetAudience: String, val type: CompetitionType = CompetitionType.DOUBLES, val system: Int = 1)

object Categories {
    val openSingles = Category("Open Singles", "os", CompetitionType.SINGLES)
    val openDoubles = Category("Open Doubles", "od")
    val womenSingles = Category("Women Singles", "ws", CompetitionType.SINGLES)
    val womenDoubles = Category("Women Doubles", "wd")
    val juniorSingles = Category("Junior Singles", "js", CompetitionType.SINGLES)
    val juniorDoubles = Category("Junior Doubles", "jd")
    val seniorSingles = Category("Senior Singles", "ss", CompetitionType.SINGLES)
    val seniorDoubles = Category("Senior Doubles", "sd")
    val mixedDoubles = Category("Mixed Doubles", "md")
    val classicOpen = Category("Classic Open", "oc", system = 3)
    val classicWomen = Category("Classic Women", "wc", system = 3)
    val classicJunior = Category("Classic Junior", "jc", system = 3)
    val classicSenior = Category("Classic Senior", "sc", system = 3)
    val all = listOf(
        openSingles,
        openDoubles,
        womenDoubles,
        womenSingles,
        juniorSingles,
        juniorDoubles,
        seniorSingles,
        seniorDoubles,
        mixedDoubles,
        classicOpen,
        classicWomen,
        classicJunior,
        classicSenior
    )
}

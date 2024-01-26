package com.bitkid.itsfranking

data class Category(val name: String, val targetAudience: String, val system: Int = 1)

val openSingles = Category("Open Singles", "os")
val openDoubles = Category("Open Doubles", "od")
val womenSingles = Category("Women Singles", "ws")
val womenDoubles = Category("Women Doubles", "wd")
val juniorSingles = Category("Junior Singles", "js")
val juniorDoubles = Category("Junior Doubles", "jd")
val seniorSingles = Category("Senior Singles", "ss")
val seniorDoubles = Category("Senior Doubles", "sd")
val mixedDoubles = Category("Mixed Doubles", "md")
val classicOpen = Category("Classic Open", "oc", 3)
val classicWomen = Category("Classic Women", "wc", 3)
val classicJunior = Category("Classic Junior", "jc", 3)
val classicSenior = Category("Classic Senior", "sc", 3)

val allCategories = listOf(
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

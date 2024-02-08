## ITSF Ranking App

### Installation

To run the app you need to have a Java JRE installed (version 8 or higher) and you need internet access.

Download the executable jar file and launch it (probably by double-clicking on it)

The latest release can always be found here https://github.com/bitkid/itsf-ranking/packages/2052592 and is named itsf-ranking-{version}-all.jar

### Features

- downloads the ranking for the chosen tour from the ITSF page (top 2000 player in every category)
- you can look at the rankings and copy them in an Excel friendly way
- you can search by ITSF license number
- you can search for player names (phonetic, string part matching)
- you can upload text files (for singles 1 player per line for doubles 2 players per line separated by `;`) and the app tries to match it with the current ranking
- you can search for missing players or players with multiple matches and complete the list by providing points manually
- columns can be sorted by clicking on them, which gives you a seeding

### Support

There is no support whatsoever. You can write issues and if I have time I might fix them. But I always accept PRs!

### Development

Needs at least JDK 8 and IntelliJ Idea (for Kotlin development)

Use `./gradlew check` for running the tests and `./gradlew run` to start the app.

User `./gradlew shadowJar` to build a runnable jar file.

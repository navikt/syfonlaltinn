# syfonlaltinn
This project contains the application code and infrastructure for syfonlaltinn

## Technologies used
* Kotlin
* Ktor
* Gradle
* Junit
* JDK 21

### :scroll: Prerequisites
* JDK 21
  Make sure you have the Java JDK 21 installed
  You can check which version you have installed using this command:
``` shell
java -version
```

## Getting started
### Building the application
#### Compile and package application
To build locally and run the integration tests you can simply run
``` bash
./gradlew shadowJar 
```
or  on windows 
`gradlew.bat shadowJar`

### Upgrading the gradle wrapper
Find the newest version of gradle here: https://gradle.org/releases/ Then run this command:

``` bash 
./gradlew wrapper --gradle-version $gradleVersjon
```

### Contact

This project is maintained by [navikt/teamsykmelding](CODEOWNERS)

Questions and/or feature requests? Please create an [issue](https://github.com/navikt/syfonlaltinn/issues)

If you work in [@navikt](https://github.com/navikt) you can reach us at the Slack
channel [#team-sykmelding](https://nav-it.slack.com/archives/CMA3XV997)
# muse-lib
A Processing-based library to access data from the Muse brain-sensing headband.

This library, and the included demos, require a Muse headband with compatible
mobile application (e.g., Muse Direct or Muse Monitor).  The mobile application
connects to the headband and relays neural data to the demo via this library.

Muse headband -> mobile Muse application -> computer running this library -> demo application

## Software Environment

* Java 8
* Git
* Gradle (optional, will be installed by gradlew script below)
* IntelliJ IDEA CE (optional)

## Installation Steps

* Clone this repository to your computer.
* Build and test with Gradle; run demo (on Windows, use ./gradlew.bat):
  * ./gradlew build
  * ./gradlew demos:bubbles:run

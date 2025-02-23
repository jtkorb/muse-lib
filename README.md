# muse-lib
A Processing-based library to access data from the Muse brain-sensing headband.

This library, and the included demos, require a Muse headband with compatible
mobile application (e.g., Muse Direct or Mind Monitor).  The mobile application
connects to the headband and relays neural data to the demo application via this library.

```
Muse headband ->
    mobile Muse application ->
    computer running this library ->
    demo application
```

This software and the smartphone connection apps have been tested with the original Muse and Muse 2 headbands,
but not the Muse S.

## Software Environment

* Java 18
* Git
* Gradle (optional, will be installed by gradlew script below)
* IntelliJ IDEA CE (optional)

## Libraries Needed

* [OSCP5](https://code.google.com/archive/p/oscp5/downloads)
* [ControlP5](https://www.sojamo.de/libraries/controlP5/)
* [Processing Core](https://processing.org/download)

Create a set of directories in ~/lib:
* ~/lib/oscP5
* ~/lib/controlP5
* ~/lib/processing-core

From the downloaded library zip files, locate the .jar files and install in the corresponding directory:
* oscP5.jar
* controlP5.jar
* processing-core-4.3.2.jar (copied and renamed from Processing.app/Contents/Java/core.jar) 

## Installation Steps

* Fork this repository and clone it to your computer.
* Build and test with Gradle; run demo (on Windows, use ./gradlew.bat):

```
$ ./gradlew build
$ ./gradlew demos:bubbles:run
```

## Working with the Muse Headband

* Install a smartphone app to communicate between the Muse headband and this application. These two apps have been tested:
  * **Muse Direct.**  Developed by InteraXon for the Muse.  Official, but iOS only.  Costs $24/year.  
  * **Mind Monitor.**  Developed by a third party (James Clutterbuck).  Runs on iOS and Android.  Cost is $15 (one time charge).
* Turn on and fit the Muse headband.
* Launch one of the demo apps (bubbles or control_layout).
* On the control panel, select the "Headband" check box.  An IP address will appear.
* Configure the smartphone app with the given IP address and port 8000.  Note that the smartphone must be able to connect to this IP address, e.g., the IP address is public or the smartphone is connected to the same private network.
* Ensure the smartphone app is sending individual sensor values, not averages.  (In Mind Monitor, for example, on the Settings cog tab, set OSC Stream Brainwaves to "All values".)
* Packets should start to flow and the display react.  (The Mind Monitor app has a streaming control icon (concentric 3/4 circles) at the bottom to toggle whether data is streamed to the headphone or not.  Ensure this icon has a backslash through it.)

## Control Panel

The Control Panel provides monitoring and some control over the data arriving from the Muse headband.  See this [annotated image](https://imgur.com/a/OZXjMig).

* **Source selection:** Selects the source for Muse data: headband, generator, or file.  Loading data from a file is not currently implemented from the Control Panel.
* **Right forehead / Right ear:** The four regions of five wave blocks correspond to the four brainwave sensors on the Muse headband.
* **Contact quality:** Shows the quality of physical connection with the headband.  Green is good, yellow is poor, red is bad, and black is none.
* **Wave selection:** Choose which waves are to be displayed by the application.  (Note: The Control Panel displays all waves, regardless of selection).
* **Sensor selection:** Choose which of the four sensors are to be displayed by the application. (Note: The Control Panel displays all waves, regardless of selection).
* **Feature selection:** Choose optional processing to be performed by the library on the waves.

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

* Java 8
* Git
* Gradle (optional, will be installed by gradlew script below)
* IntelliJ IDEA CE (optional)

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
* Packets should start to flow and the display react.

## Control Panel

The Control Panel provides monitoring and some control over the data arriving from the Muse headband.  See this [annotated image](https://imgur.com/a/OZXjMig).

* **Source selection:** Selects the source for Muse data: headband, generator, or file.  Loading data from a file is not currently implemented from the Control Panel.
* **Right forehead / Right ear:** The four regions of five wave blocks correspond to the four brainwave sensors on the Muse headband.
* **Contact quality:** Shows the quality of physical connection with the headband.  Green is good, yellow is poor, red is bad, and black is none.
* **Wave selection:** Choose which waves are to be displayed by the application.  (Note: The Control Panel displays all waves, regardless of selection).
* **Sensor selection:** Choose which of the four sensors are to be displayed by the application. (Note: The Control Panel displays all waves, regardless of selection).
* **Feature selection:** Choose optional processing to be performed by the library on the waves.

## Launcher Demo

Included with this repository is a Gradle configuration to run the "Launcher" demo used in the
[ArtWaves demonstration](https://www.purdue.edu/discoverypark/2050/exhibits.php) at the
[Purdue 2050 Conference of the Future](https://www.purdue.edu/discoverypark/2050/index.php).  To run the
demonstration, use the gradle command:

```
$ ./gradlew demos:launcher:run
````

While the main window is "active" (on top), use the "1" and "2" keys to toggle between demonstrations, 
"Focus" and "Calm", respectively.

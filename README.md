# SafeCommuteCompanionApp

This is the Android companion App for an Arduino Nano BLE 33 Sense project. 

To recreate the project navigate to the sub-directory "safe-commute-companion_project" and clone the whole directory. 
To be able to use the app, you need to have an Arduino board and the Arduino sketch counterpart. 

Important hints before testing out the app: 
Make sure to change the String on line 59 in safe-commute-companion_project/app/src/main/java/com/punchthrough/blestarterappandroid/ScanResultAdapter.kt to the Serial Number of your own Arduino board (we used the Lightblue App to find out our boards Serial number, which is available on the Google Play Store)
Also change the name of the device on line 167 in safe-commute-companion_project/app/src/main/java/com/punchthrough/blestarterappandroid/MainActivity.kt to the name that you gave your Arduino board in the Arduino sketch. 

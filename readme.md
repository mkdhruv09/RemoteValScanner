# RemoteVal Scanning SDK

## Table of Contents

* [Example Project](#example-project)
* [RemoteVal library module](#remotevalcapture-library-module)
    * [Implementation](#implementation)
        * [Setting up](#setting-up)
        * [Warning sound](#warning-sound)
        * [Automatic and manual zipping](#automatic-and-manual-zipping)
    * [UI settings](#ui-settings)
        * [General](#general)
        * [Colors and alphas](#colors-and-alphas)
        * [Dimensions](#dimensions)
        * [Texts](#texts)
        * [Drawable graphics](#drawable-graphics)
    * [About warning priorities](#about-warning-priorities)
    * [Status codes](#status-codes)

## Example Project

This project provides an example implementation and use of the RemoteVal Video based scanning
library module. From this project you can get a basic idea of how to implement the scanning and
scan playback with RemoteVal capture to your app.

## Video based scan library module

RemoteVal Video-based scanning library module provides a scanning `Fragment` which can be used to
scan a floor plan with an Android device. The scanning `Fragment` saves scan files into a zip file,
which your app can upload to the RemoteVal back-end for processing. RemoteVal SDK handles corner
cases like fast scanning, Too near to the wall, and ceiling scanning to guide user for proper
scanning.

## Implementation

This implementation was made with Android Studio Electric Eel | 2022.1.1 using Gradle plugin
version 1.7.0.

### Setting up

Download following files

[Android library module](https://github.com/sculptsoft-dev/RemoteValScanner/releases) <br/>

Add the RemoteVal library module to your project:

1. Place the `remoteval-capture-release-1.0.0.aar` file to your project's `app/libs/` folder.
2. In Android Studio navigate to: `File` -> `Project Structure` -> `Dependencies` -> `app` ->
   In the `Declared Dependencies` tab, click `+` and select `JAR/AAR Dependency`.
3. In the `JAR/AAR Dependency` dialog, enter the path as `libs/remoteval-capture-release-1.0.0.aar`
   and select `implementation` as configuration. -> Press `OK`.
4. Or you can do it manually by copying `remoteval-capture-release-1.0.0.aar` file to `app/libs/`folder
   and do the next step (Skip this step if you already did previous step)
5. Check your app's `build.gradle` file to confirm a that in contains the following declaration
   `implementation files('libs/remoteval-capture-release-1.0.0.aar')`. (add this if not present)
6. Copy `lib.gradle` file into your root project directory and then add following line to your app
   level
   `build.gradle` This will include all dependency that require by aar file

   ```groovy
   apply {
       from("$rootDir/remoteval-lib.gradle")
   }
   ```
7. Set the `targetSdkVersion` to API level `32` in app level `build.gradle`.
8. Make sure you've following configuration to your `app>build.gradle` file

   ```Groovy
   android {
       compileOptions {
           sourceCompatibility JavaVersion.VERSION_1_8
           targetCompatibility JavaVersion.VERSION_1_8
       }
       kotlinOptions {
           jvmTarget = '1.8'
       }
       buildFeatures {
           viewBinding true
       }
   }
   ```

## Scanning part

Create a layout file and add RemoteValFragment

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto" android:layout_width="match_parent"
    android:layout_height="match_parent">

    <androidx.fragment.app.FragmentContainerView android:id="@+id/fragment"
        android:name="com.app.videoscanner.RemoteValFragment" android:layout_width="match_parent"
        android:layout_height="match_parent" app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent" app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

Create an Activity to use scan feature (You can give whatever name based on your app)

```Kotlin
class ScanActivity : AppCompatActivity()
```

To allow your scanning `Activity` to consume more RAM, to lock screen orientation to landscape and
to prevent multiple `onCreate()` calls add the following code to your `AndroidManifest` file inside
your scanning `Activity`'s `activity` tag - Example:

```xml

<application>
    <activity android:name=".ScanActivity" android:largeHeap="true"
        android:screenOrientation="landscape" android:configChanges="orientation|screenSize" />
</application>
```

1. Declare lateinit variable of remoteValFragment into your scanning activity

```kotlin
private lateinit var remoteValFragment: RemoteValFragment
```

2. Initialize remoteValFragment inside onCreate()

```kotlin
remoteValFragment = supportFragmentManager.findFragmentById(R.id.fragment) as RemoteValFragment
```

3. Implement in ScanActivity RemoteValEventListener for Scanner Callback

```kotlin
class ScanActivity : AppCompatActivity(), RemoteValEventListener
```

4. Add the following function to receive status updates from `RemoteValEventListener`:

```Kotlin
override fun getStatus(code: Int, description: String) {

}
```

5. Add the following function to receive scan folder and zip file as `File`
   from `RemoteValEventListener`:

```Kotlin
override fun getFile(fileStatus: RemoteValFileStatus, file: File) {

}
```

6.Register `RemoteValEventListener`'s interface callback for scan events in `onCreate()`:

```Kotlin
remoteValFragment.registerCallback(this)
```

7. Set scanning location by giving folder name `scanFolderName: String`
   where we store the scan files.
   **Note!** You should always check that a scan folder with that name does not already exist and
   that the `String` value of the scan folder name is a valid `File` name!

```Kotlin
val scanningFolderName: String =
    getScanFolderName() // here you can give name which usually want to create folder as
remoteValFragment.scanFolderName = scanningFolderName
```

8. Set the `allScansFolder: File?` to set the directory where RemoteVal will save all the scan
   folders. Android 11 Storage updates will restrict where your app can store data. We suggest the
   directory returned by `fileDir` for storing scan data. Just make sure that the storage is
   available and that the returned `File` is not `null`. The default folder location is cacheDir
   <br/>
   If you're using other directory then read more about Android 11 storage updates from :
   [Android Storage](https://developer.android.com/about/versions/11/privacy/storage)

Example:

```Kotlin
val mainStorage: File = filesDir
remoteValFragment.allScansFolder = mainStorage
```

## Uploading Part

To take input from user regarding job order you've to create activity. This will be launcher
activity for your previously created activity (ScanActivity)

1. Create RemoteValManager instance inside your Activity

```kotlin
private val remoteValManager = RemoteValManager.createInstance()
```

2. Initialize SDK

```kotlin
remoteValManager.init(
    clientKey = < YOUR_CLIENT_KEY >,
apiEnvironment = ApiEnvironment.PRODUCTION_STAGING,
sdkTokenCallback = object : RemoteValManager.SdkTokenCallback {
    override fun onSuccess() {
    }

    override fun onFailure(throwable: Throwable) {
    }
}) 
```

3.Generate Order

```kotlin
//Create OrderInformation by filling below values
val orderInfo = OrderInfoRequest(
    ownerEmail = binding.etOwnerEmail.trimmedText(),
    ownerName = binding.etOwnerName.trimmedText(),
    ownerPhone = binding.etOwnerPhone.trimmedText(),
    propertyAddress1 = binding.etAddress1.trimmedText() + binding.etAddress2.trimmedText(),
    propertyCity = binding.etCity.trimmedText(),
    propertyCountry = binding.etCountry.trimmedText(),
    propertyState = binding.etState.trimmedText(),
    propertyZip = binding.etPostalCode.trimmedText(),
    propertyType = OrderInfoRequest.PROPERTY_TYPE_RESIDENTIAL or PROPERTY_TYPE_COMMERCIAL,
    latitude = currentLatitude.toString(),
    longitude = currentLongitude.toString()
)

remoteValManager.generateJobOrder(
    orderInfoRequest = orderInfo, object : RemoteValManager.JobOrderCallback {
        override fun onSuccess(orderInfo: OrderInfo) {
            // After successful generation of order 
            // you'll get OrderInfo response from server it contains "jobOrderId" for your reference
            // Now you can ask user to select floor and wall thickness : this is optional
            startFloorSelectionScreen() or startScanning()
        }

        override fun onFailure(throwable: Throwable) {

        }
    })
```

4. Upload Scanned file : Once your scanning completed upload zip file
   First, It requires storage detail to upload scanned file. To retrieve storage info call

```kotlin
 remoteValManager.getStorageDetail(orderInfo!!,
    success = { storageInfo ->

    }, failure = { throwable ->

    }
)
```

Once you've storage detail then you can upload scanned file using that storage info by

```kotlin
 remoteValManager.upload(
    file = zipFile,
    storageInfo = storageInfo,
    jobUploadCallback = object : RemoteValManager.JobUploadCallback {
        override fun onProgressPercentage(percentage: Int) {

        }

        override fun onSuccess() {

        }

        override fun onFailure(throwable: Throwable) {

        }
    })
```

5. Create Floor Scan : After successfully upload Scanned Zip file you've to create a floor scan using

```kotlin
remoteValManager.uploadFloorScan(
    orderId = orderInfo?.jobOrderId!!,
    floorIndex = floorIndex,
    wallThickness = wallThickness,
    file = zipFile,
    floorScanCallback = object : RemoteValManager.FloorScanCallback {
        override fun onSuccess() {
            rescanAnotherFloorDialog.show()
        }

        override fun onFailure(throwable: Throwable) {
            toast(throwable.localizedMessage ?: "")
        }
    }
)

```

## State Country Data

To create job order it requires Country and state. it should be one of them. so you can get those
list by

```kotlin
RemoteValResource.getStates(): List<State>
RemoteValResource.getCountries(): List<Country>
```

## Floor Selection

To Scan floor it must have Floor Index and Wall thickness.
RemoteVal also provides multi floor uploading option.
using it you can also upload multiple scan (like ground floor,1st floor,etc..) for one job order

To get list of supported Floors & Thickness you can use

```kotlin
RemoteValResource.getFloorIndexNames(context: Context): List<String>
RemoteValResource.getFloorThicknessList(): List<String>
```

## HttpException

To retrieve error message body from HttpException (if throwable is HttpException)

```kotlin
throwable.parseHttpException()?.let { errorResponse ->
    toast(errorResponse.message) // basic error message retrieve from API
    Log.e("Error", errorResponse.errorFields.toString()) //Field declaration which generates error
} ?: run {
    toast(throwable.localizedMessage ?: "")
}

```

<h2>Customization</h2>

#### Warning sound

Warning sound is played when the ARCore's `TrackingState` is **not** `TRACKING`.

To change the warning sound call:

```Kotlin
remoteValFragment.setWarningSound(R.raw.warning)
```

#### Automatic and manual zipping

To disable the automatic zipping after a scan call:

```Kotlin
remoteValFragment.setAutoZippingEnable(false) // true (auto zips) by default
```

Zipping scan folder if automatic zipping is disabled. This can be called after scan files are saved
successfully. This returns the Zip file if it is successful or null if zipping failed.

```Kotlin
val zipFile = remoteValFragment.zipScan(scannedFolder) // Pass scan folder path as String
```

Manual zipping `zipScan()` method expects the scan folder to contain the following files;
`ARPoses.txt`, `Frames.mp4` and `ARPoses.txt`,etc... If any of the files above doesn't
exist, `zipScan()` returns `null`.

Here's directory structure where `zipScan()` method would successfully zip the scan

```
└── AllScansFolder (filesDir/cacheDir/externalStorageDir)
    ├── ScanFolder (OrderId)
        ├── DeviceInfo.json
        └── 0
            ├── ARPoses.txt
            ├── driftFrameNumber.txt
            ├── Frames.mp4
            ├── Frames.txt
            ├── noFeaturePointData.txt
            └── OrignalPose.txt
```

## UI settings

#### General

To set the visibility of scan timer call:

```Kotlin
remoteValFragment.setTimerEnabled(false) // Visible (true) by default
```

To set the visibility of RemoteVal's back button call:

```Kotlin
remoteValFragment.setBackButtonEnabled(false) // Visible (true) by default
```

To set the enabled status of the record button `View`:

```Kotlin
remoteValFragment.recordButtonEnabled(false)
```

#### Colors and alphas

To change the colors or alphas of the default RemoteVal graphics you have to redefine the
default `color`s in your application `colors.xml` file.

```xml

<resources>
    <color name="rv_start_scan_background_color">#0db350</color>
    <color name="rv_start_scan_text_color">#FFFFFF</color>
    <color name="rv_end_scan_background_color">#334AA6</color>
    <color name="rv_end_scan_text_color">#FFFFFF</color>
</resources>
```

#### Drawable graphics

To change CubiCapture's default drawables you have to override them by redefining the drawables in
your applications `drawables.xml` file. You need to have the `drawables.xml` file created in the
`values` directory where the `colors.xml` file is as well.

Here's all the default drawables defined in CubiCapture library:

```xml

<resources>
    <drawable name="ic_rv_attach_cross">@drawable/ic_rv_attach_cross_original</drawable>
    <drawable name="ic_rv_rotate">@drawable/ic_rv_attach_cross_original</drawable>
    <drawable name="ic_rv_tilt_back">@drawable/ic_tilt_back_original</drawable>
    <drawable name="ic_too_close">@drawable/ic_too_close_original</drawable>
    <drawable name="ic_resume_scan">@drawable/ic_resume_scan_original</drawable>
    <drawable name="ic_re_scan">@drawable/ic_re_scan_original</drawable>
    <drawable name="ic_move_device">@drawable/ic_move_device_original</drawable>
    <drawable name="ic_landscap">@drawable/ic_landscap_original</drawable>
    <drawable name="ic_fast">@drawable/ic_fast_original</drawable>
    <drawable name="ic_celing">@drawable/ic_celing_original</drawable>
    <drawable name="ic_camera_off">@drawable/ic_camera_off_original</drawable>
    <drawable name="ic_floor_scan">@drawable/ic_floor_scan_original</drawable>
    <drawable name="ic_floor_plan_published">@drawable/ic_floor_plan_published_original</drawable>
</resources>
```

#### Dimensions

To change the RemoteVal's default layout sizes, margins and text sizes you have to override the
library's default dimensions by defining the dimensions in your applications
`dimens.xml` file. To override the library's default dimensions, you need to have the `dimens.xml`
file created in the `values` directory where the `colors.xml` file is as well. Here are all the
default dimensions defined in CubiCapture library:

```xml

<resources>
    <dimen name="button_record_size">72dp</dimen>
    <dimen name="button_record_margin_end">52dp</dimen>
    <dimen name="hint_label_width">0dp</dimen>
</resources>
```

#### Texts

To change RemoteVal's default texts you have to override the library's default strings by redefining
the strings in your applications `strings.xml` file.

Here's all the default texts defined in CubiCapture library:

```xml

<resources>
    <string name="rv_btn_cancel">Cancel</string>
    <string name="rv_yes">Yes</string>
    <string name="rv_no">No</string>
    <string name="rv_exit">Exit</string>
    <string name="rv_ok">OK</string>
    <string name="rv_max_participants_msg">There Can Be Maximum of 4 Participants.</string>
    <string name="rv_retry">Retry</string>
    <string name="rv_max_participants_title">Limit of Participants Has Been Reached</string>
    <string name="rv_move_your_device_in_circle">Move your device in circle</string>
    <string name="rv_start_title">Start Scanning</string>
    <string name="rv_stop_title">End & Publish</string>
    <string name="rv_fail_to_upload_zip_file">Files uploading failed,Please try again.</string>
    <string name="rv_zip_uploading">Uploading files:</string>
    <string name="rv_preparing_zip">Compressing files:</string>
    <string name="rv_app_not_close_warning">Please do not exit the application and don\'t go in the
        background.
    </string>
    <string name="rv_uploading_failed">Uploading failed</string>
    <string name="rv_ar_alert_landscap_title">Turn your device left to landscape orientation
    </string>
    <string name="rv_ar_alert_celling_title">Do not scan ceilings</string>
    <string name="rv_ar_alert_celling_msg">We can lose track of your position if ceilings are
        scanned
    </string>
    <string name="rv_ar_alert_rotate_title">Rotate your device 180°</string>
    <string name="rv_ar_alert_rotate_msg">Make sure the end and publish button is on the right
    </string>
    <string name="rv_ar_alert_tilt_backward_title">Tilt the device back a bit</string>
    <string name="rv_ar_alert_fast_title">You are turning too fast</string>
    <string name="rv_ar_alert_fast_msg">Avoid fast turns for best scanning results</string>
    <string name="rv_ar_alert_low_light_title">It is too dark, Turn on the Lights</string>
    <string name="rv_ar_alert_object_title">We have trouble tracking your position</string>
    <string name="rv_ar_alert_object_msg">because there are not enough visual features here</string>
    <string name="rv_app_not_close">Please do not exit the application</string>
    <string name="rv_cancel_upload">Cancel Upload</string>
    <string name="rv_cancel_upload_msg">Are you sure you want to cancel this upload?</string>
    <string name="rv_lbl_minimum_one_for_end_call">Minimum 30 secs of video scanning is required to
        End Scanning & Process the Floor Plan.
    </string>
    <string name="rv_lbl_end_scan_and_publish">Are you sure you want to finish the current scanning&
        upload the files for the floor plan?
    </string>
    <string name="rv_lbl_cancel_ongoing_scan">Are you sure you want to cancel the scanning & go
        back?
    </string>
    <string name="rv_lbl_turn_in_landscape">Please turn your device in landscape mode to start the
        video scanning.
    </string>
    <string name="rv_lbl_ar_not_initialize">The scanning Environment is getting ready, Please Start
        Video Scanning Again.
    </string>
    <string name="rv_scan_aborted_title">Scan Aborted</string>
    <string name="rv_scan_aborted_message">The scan was aborted because the app was backgrounded.
        Please scan entire floor again.
    </string>
    <string name="rv_scan_fail_title">Scanning Failed</string>
    <string name="rv_scan_fail_message_mp4_not_crate">Error in starting the scan, Please try
        again(1001)
    </string>
    <string name="rv_scan_fail_message_video_shoor">Error in starting the scan, Please try
        again(1002)
    </string>
    <string name="rv_scan_fail_message_finish_Video">Error in starting the scan, Please try
        again(1003)
    </string>
    <string name="rv_scan_fail_message_directory_not_create">Error in starting the scan, Please try
        again(1004)
    </string>
    <string name="rv_scan_fail_message_write_file">Error in starting the scan, Please try
        again(1005)
    </string>
    <string name="rv_scan_aborted_insufficient">The scan was aborted as your phone is running out of
        required storage space for video-based scanning.
    </string>
    <string name="rv_scan_aborted_msg">To do video-based scanning please consider deleting content
        you no longer need & try again. Or Get the inspection done with corner-based scanning by
        just tapping on the corners.
    </string>
    <string name="rv_scan_aborted_insufficient_warn">Your device can only store another 0 minutes of
        scan data, this may not be enough to complete this scan.
    </string>
    <string name="rv_scan_aborted_confirmation">Are you sure you want to start scanning?</string>
    <string name="rv_scan_aborted_drift">The scan abort due to unstable ar environment</string>
    <string name="rv_resume">Resume</string>
    <string name="rv_ar_alert_too_close_title">You are too close</string>
    <string name="rv_ar_alert_too_close_msg">Keep more distance from the objects you are scanning.
    </string>
    <string name="rv_rescan_the_same_area">Rescan The Same Area</string>
    <string name="rv_rescan_dialog_title">Please confirm you want to rescan the same area?</string>
    <string name="rv_rescan_same_area_title">Rescan this area again</string>
    <string name="rv_rescan_same_area_desc">Your scan had some missing information, please click the
        \"Rescan The Same Area\" button on your screen to re-do the scan.
    </string>
    <string name="rv_reach_starting_point_of_area">Please move to the same area/starting point as
        your current scan, then click on the Resume button to begin the re-scan.
    </string>
    <string name="rv_ar_environment_stable">Scanning feature ready, you may begin scanning!</string>
    <string name="rv_msg_camera_off_title">Your call is in progress</string>
    <string name="rv_msg_camera_view_is_off">The camera is turned off during the scan upload. Once
        it is completed, click "Scan Another Floor" to resume the video capture.
    </string>
    <string name="rv_camera_permission_message">Camera permission is needed to run this
        application
    </string>
    <string name="rv_permission_title">Permission</string>
    <string name="rv_open_setting">Open Setting</string>
</resources>

```

## About warning priorities

During a scan, RemoteVal shows a warning to the user if it detects bad scanning styles or any issues
with the tracking. All the warnings are divided into priority-level groups. Higher priority warnings
will override and hide any lower priority warning in order to only show the higher priority warning.
As an exception, ceiling warning will override horizontal warning, although they have the same
priority level. Priority level `1` is the highest priority, `2` is the second highest priority and
so on.

| Priority level | Warnings                                              |
|----------------|-------------------------------------------------------|
| 1              | Not tracking*                                         |
| 2              | Rotate device                                         |
| 3              | Sideways walking, Fast movement, Too close            |
| 4              | Ceiling scanning, Floor scanning, Horizontal scanning |

## Status codes

| Status Code | Message                                                           | Description                                                                                                                                                                                                                   |
|-------------|-------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 0           | Device turned to landscape orientation.                           | Received when the device is in landscape orientation and either the turn device guidance or the rotate device warning is hidden.                                                                                              |
| 1           | Started recording                                                 | Received when the scan is started by pressing the record button.                                                                                                                                                              |
| 2           | Finished recording                                                | Received when user has ended the scan with the slider and scan has enough data. The saving of the scan files begins after this.                                                                                               |
| 6           | Scan folder: $folderPath                                          | Received when the saving of the scan files is finished. The description will contain a path to the scan folder. To receive the scan folder as a File use the RemoteValEventListener's getFile(code: Int, file: File) method   |
| 7           | Zipping is done. Zip file: $zipFilePath                           | Received when the zipping of the scan files is finished. The description will contain a path to the zip file. To receive the zip file as a File use the RemoteValEventListener's getFile(code: Int, file: File) method        |
| 8           | ARCore TrackingFailureReason: INSUFFICIENT_LIGHT                  | Received when ARCore motion tracking is lost due to poor lighting conditions                                                                                                                                                  |
| 9           | ARCore TrackingFailureReason: EXCESSIVE_MOTION                    | Received when ARCore motion tracking is lost due to excessive motion.                                                                                                                                                         |
| 10          | ARCore TrackingFailureReason: INSUFFICIENT_FEATURES               | Received when ARCore motion tracking is lost due to insufficient visual features.                                                                                                                                             |
| 85          | ARCore TrackingFailureReason: SCANNING_TO_CLOSE_PROXIMITY_WARNING | Received if the user is scanning too close to objects and too close.                                                                                                                                                          |
| 87          | ARCore TrackingFailureReason:TOO_FAST_MOVING_SHOWING_WARNING      | Received when the user turns around too fast while scanning and fast movement.                                                                                                                                                |
| 101         | Device turned to landscape orientation.                           | Received when the device is in landscape orientation and either the turn device guidance or the rotate device warning is hidden.                                                                                              |
| 106         | Device is not compatible with ARCore.                             | Received when the device is not compatible with ARCore.                                                                                                                                                                       |
| 301         | Install Google Play Services for AR                               | Received when Google Play Services are not installed for AR                                                                                                                                                                   |
| 302         | Update ARCore                                                     | Received when ARCore are not updated                                                                                                                                                                                          |
| 305         | SDK is old                                                        | Received when the ARCore SDK that this application was built with is too old for the installed ARCore APK.                                                                                                                    |
| 306         | Camera is not available                                           | Received when Camera not available                                                                                                                                                                                            |
| 307         | Session creation failed                                           | Received when failed to create Ar session                                                                                                                                                                                     |


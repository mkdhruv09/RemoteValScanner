package com.app.vsitevideoscanner

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.app.videoscanner.RemoteValEventListener
import com.app.videoscanner.RemoteValFileStatus
import com.app.videoscanner.RemoteValFragment
import com.app.videoscanner.utils.FullScreenHelper
import com.app.vsitevideoscanner.incenter.BuildConfig
import com.app.vsitevideoscanner.incenter.R
import java.io.File

class ScanActivity : AppCompatActivity(), RemoteValEventListener {

    companion object {

        //Intent result constants
        const val RESULT_FILE = "filePath"

        //Intent request constants
        const val TIMER_ENABLE = "timerEnable"
        const val AUTO_ZIP = "autoZip"
        const val FOLDER_NAME = "folderName"
    }

    private lateinit var remoteValFragment: RemoteValFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanning)

        val folderName =
            intent.getStringExtra(FOLDER_NAME) ?: System.currentTimeMillis().toString()

        FullScreenHelper.setFullScreenOnWindowFocusChanged(this, true)

        remoteValFragment =
            supportFragmentManager.findFragmentById(R.id.fragment) as RemoteValFragment

        remoteValFragment.allScansFolder = cacheDir
        remoteValFragment.scanFolderName = folderName

        remoteValFragment.appBuild = BuildConfig.VERSION_CODE
        remoteValFragment.appVersion = BuildConfig.VERSION_NAME

        remoteValFragment.recordButtonEnabled(false)
        remoteValFragment.setBackButtonEnable(true)

        remoteValFragment.setWarningSound(R.raw.warning)
        remoteValFragment.setTimerEnable(true)
        remoteValFragment.setAutoZippingEnable(true)
        remoteValFragment.registerCallback(this)
    }

    override fun getFile(fileStatus: RemoteValFileStatus, file: File) {
        if (fileStatus == RemoteValFileStatus.FILE_SCAN_FOLDER) {
            Toast.makeText(this, "Scan Folder Received", Toast.LENGTH_LONG).show()
            val zipFile = remoteValFragment.zipScan(file)
            sendResult(zipFile)
        } else if (fileStatus == RemoteValFileStatus.FILE_ZIP) {
            Toast.makeText(this, "Zip File Received", Toast.LENGTH_LONG).show()
            sendResult(file)
        }
    }

    override fun getStatus(code: Int, description: String) {
        //Toast.makeText(this, description, Toast.LENGTH_LONG).show()
    }

    override fun onBackPressed() {
        if (remoteValFragment.onBackPressed().not()) {
            super.onBackPressed()
        }
    }

    private fun sendResult(file: File) {
        val resultIntent = Intent()
        resultIntent.putExtra(RESULT_FILE, file.absolutePath)
        setResult(RESULT_OK, resultIntent)
        finish()
    }
}
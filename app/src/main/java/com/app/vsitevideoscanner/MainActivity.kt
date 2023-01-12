package com.app.vsitevideoscanner

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.app.videoscanner.RemoteValManager
import com.app.videoscanner.data.model.OrderInfo
import com.app.videoscanner.data.network.ApiEnvironment
import com.app.videoscanner.data.request.OrderInfoRequest
import com.app.videoscanner.utils.AppUtils
import com.app.videoscanner.utils.createUploadProgressDialog
import com.app.videoscanner.utils.toast
import com.app.vsitevideoscanner.incenter.R
import com.app.vsitevideoscanner.incenter.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {

    //demo
    private val CLIENT_KEY =
        "5yO4To6yPEIdNihde3nQuk2oOE3J44tiAp3QVS46lfMtRZ1S_.d._sbdHVt8fS3oUn6UugQ9_.d._yI8cfN_.d._A5EbxP9mxscVA1nEURzmNKCC78V_.d._AfQW_n28y6BSWekEjeO3pWMblU9k8uTobp7ugtICFmhImQ"

    //incenter
    /* private val CLIENT_KEY =
         "5yO4To6yPEIdNihde3nQurUt8tXYbI4wqwB0JHWXTqHoIMjKDU67WluZWAitmoRBydv3mbahrZ0ZwqWsNU6Fpb_.d._ZQkvkjwHrQfmqBpXlMByWWxIQpaRbz7ZKScAsgPtABWlxv8GcPEKyQ_vL0lx9HexkpxIfZ7XX3xyJH9Q_DnwRmqyin_.d._C8P68LnUHJ3wpX"*/

    private val remoteValManager = RemoteValManager.createInstance()
    private lateinit var binding: ActivityMainBinding

    var orderInfo: OrderInfo? = null
    private var wallThickness: Int = 0
    private var floorIndex: Int = 0
    private var floorName: String = ""
    private val currentLatitude = 10.05000
    private val currentLongitude = 10.05000

    private val uploadDialog by lazy {
        return@lazy createUploadProgressDialog(
            title = getString(com.videoscanner.R.string.rv_zip_uploading),
            message = getString(com.videoscanner.R.string.rv_app_not_close_warning)
        )
    }

    private val loadingDialog by lazy {
        return@lazy createUploadProgressDialog(
            title = getString(R.string.fetching_tenant_info),
            message = getString(com.videoscanner.R.string.rv_app_not_close_warning)
        )
    }
    private val jobCreatingDialog by lazy {
        return@lazy createUploadProgressDialog(
            title = getString(R.string.creating_job_order),
            message = getString(com.videoscanner.R.string.rv_app_not_close_warning)
        )
    }

    private val rescanAnotherFloorDialog by lazy {
        return@lazy AppUtils.createAlertDialog(
            activity = this,
            title = getString(R.string.upload_successful),
            description = getString(R.string.scan_another_floor),
            positiveAction = Pair(first = getString(R.string.yes), second = {
                startFloorSelectionScreen()
            }),
            negativeAction = Pair(
                first = getString(com.videoscanner.R.string.rv_btn_cancel),
                second = {
                    finish()
                }),
            onDismiss = {
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        binding.btnStartScanning.setOnClickListener {
            try {
                val orderInfo = validate()
                generateJobOrder(orderInfo)
            } catch (e: java.lang.IllegalArgumentException) {
                toast(e.message ?: "")
            }
        }

        fillSampleOrderData()
        initRemoteValManager()
    }

    private fun fillSampleOrderData() {
        binding.etOrderId.setText(System.currentTimeMillis().toString())
        binding.etCity.setText("Newark")
        binding.etState.setText("New Jersey")
        binding.etPostalCode.setText("08002")
        binding.etCountry.setText("United States")
        binding.etAddress1.setText("1 Cherry hill")
        binding.etAddress2.setText("New Jersey")
        binding.etOwnerEmail.setText("demo@remoteval.com")
        binding.etOwnerPhone.setText("1111111111")
        binding.etOwnerName.setText("RemoteVal Demo User")
    }


    //Step 1
    private fun initRemoteValManager() {
        loadingDialog.show()
        remoteValManager.init(
            clientKey = CLIENT_KEY,
            apiEnvironment = ApiEnvironment.STAGING_ENVIRONMENT,
            sdkTokenCallback = object : RemoteValManager.SdkTokenCallback {
                override fun onSuccess() {
                    loadingDialog.dismiss()
                }

                override fun onFailure(throwable: Throwable) {
                    toast(throwable.localizedMessage ?: "")
                    loadingDialog.dismiss()
                }
            })
    }

    //Step 2
    private fun generateJobOrder(orderInfo: OrderInfoRequest) {
        jobCreatingDialog.show()
        remoteValManager.generateOrder(
            orderInfo,
            object : RemoteValManager.JobOrderCallback {
                override fun onSuccess(orderInfo: OrderInfo) {
                    this@MainActivity.orderInfo = orderInfo
                    jobCreatingDialog.dismiss()
                    startFloorSelectionScreen()
                }

                override fun onFailure(throwable: Throwable) {
                    jobCreatingDialog.dismiss()
                    toast(throwable.localizedMessage ?: "")
                }
            })
    }

    //Step 3
    private fun uploadScan(zipFile: File) {
        uploadDialog.show()
        remoteValManager.upload(
            file = zipFile,
            jobUploadCallback = object : RemoteValManager.JobUploadCallback {
                override fun onProgressPercentage(percentage: Int) {

                }

                override fun onSuccess() {
                    uploadDialog.dismiss()
                    uploadFloorScan(zipFile)
                }

                override fun onFailure(throwable: Throwable) {
                    uploadDialog.dismiss()
                    toast(throwable.localizedMessage ?: "")
                }
            })
    }

    //Step 4
    private fun uploadFloorScan(zipFile: File) {
        remoteValManager.uploadFloorScan(
            orderId = orderInfo?.jobOrderId!!,
            floorIndex = floorIndex,
            wallThickness = wallThickness,
            file = zipFile,
            floorScanCallback = object : RemoteValManager.FloorScanCallback {
                override fun onSuccess() {
                    //Scan another floor is optional you may complete job scanning if you don't want to scan other floor
                    rescanAnotherFloorDialog.show()
                }

                override fun onFailure(throwable: Throwable) {
                    toast(throwable.localizedMessage ?: "")
                }
            }
        )
    }

    private fun startFloorSelectionScreen() {
        floorSelectionLauncher.launch(Intent(this, SelectFloorActivity::class.java))
    }

    /**
     * Floor selection is optional
     * you can also upload single scan
     * This feature given just for only demonstrating purpose
     */
    private val floorSelectionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                floorIndex = it.data?.getIntExtra("floorIndex", 0) ?: 0
                wallThickness = it.data?.getIntExtra("wallThickness", 0) ?: 0
                floorName = it.data?.getStringExtra("floorName") ?: ""
                startScanner()
            }
        }


    private fun startScanner() {
        val intent = Intent(this@MainActivity, ScanActivity::class.java).apply {
            val folderName =
                remoteValManager.generateFolderName(orderInfo = orderInfo!!, floorName = floorName)
            putExtra(ScanActivity.FOLDER_NAME, folderName)
        }
        scanActivityLauncher.launch(intent)
    }

    private val scanActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
            if (activityResult.resultCode == Activity.RESULT_OK) {
                val zipFile = activityResult.data?.getStringExtra(ScanActivity.RESULT_FILE)
                if (zipFile.isNullOrEmpty().not()) {
                    uploadScan(File(zipFile))
                }
            }
        }

    @Throws(java.lang.IllegalArgumentException::class)
    private fun validate(): OrderInfoRequest {
        binding.etOwnerName.trimmedText().isEmpty().not()
            .validateOrThrow { getString(R.string.invalid_owner_name) }
        binding.etOwnerEmail.trimmedText().isEmpty().not()
            .validateOrThrow { getString(R.string.invalid_owner_email) }
        binding.etOwnerPhone.trimmedText().isEmpty().not()
            .validateOrThrow { getString(R.string.invalid_phone) }
        binding.etAddress1.trimmedText().isEmpty().not()
            .validateOrThrow { getString(R.string.invalid_address_first) }
        binding.etAddress2.trimmedText().isEmpty().not()
            .validateOrThrow { getString(R.string.invalid_address_second) }
        binding.etCity.trimmedText().isEmpty().not()
            .validateOrThrow { getString(R.string.invalid_city) }
        binding.etState.trimmedText().isEmpty().not()
            .validateOrThrow { getString(R.string.invalid_state) }
        binding.etCountry.trimmedText().isEmpty().not()
            .validateOrThrow { getString(R.string.invalid_country) }
        binding.etPostalCode.trimmedText().isEmpty().not()
            .validateOrThrow { getString(R.string.invalid_postal_code) }
        return OrderInfoRequest(
            ownerEmail = binding.etOwnerEmail.trimmedText(),
            ownerName = binding.etOwnerName.trimmedText(),
            ownerPhone = binding.etOwnerPhone.trimmedText(),
            propertyAddress1 = binding.etAddress1.trimmedText() + binding.etAddress2.trimmedText(),
            propertyCity = binding.etCity.trimmedText(),
            propertyCountry = binding.etCountry.trimmedText(),
            propertyState = binding.etState.trimmedText(),
            propertyZip = binding.etPostalCode.trimmedText(),
            propertyType = OrderInfoRequest.PROPERTY_TYPE_RESIDENTIAL,
            latitude = currentLatitude.toString(),
            longitude = currentLongitude.toString()
        )
    }

    @Throws(java.lang.IllegalArgumentException::class)
    private fun Boolean.validateOrThrow(block: () -> String) {
        if (this.not()) {
            throw java.lang.IllegalArgumentException(block())
        }
    }

    private fun EditText.trimmedText(): String {
        return text.toString().trim()
    }

    override fun onDestroy() {
        super.onDestroy()
        remoteValManager.clear()
    }
}
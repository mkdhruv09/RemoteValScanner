package com.app.vsitevideoscanner

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.app.videoscanner.RemoteValFloorResource
import com.app.vsitevideoscanner.incenter.databinding.ActivityFloorSelectionBinding

class SelectFloorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFloorSelectionBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFloorSelectionBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        binding.spSelectFloor.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            RemoteValFloorResource.getFloorIndexNames(this)
        )


        binding.spWallThickness.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            RemoteValFloorResource.getFloorThicknessList()
        )

        binding.btnStartScanning.setOnClickListener {
            val resultIntent = Intent()
            resultIntent.putExtra("floorName", binding.spSelectFloor.selectedItem.toString())
            resultIntent.putExtra(
                "floorIndex",
                binding.spSelectFloor.selectedItemPosition + 1
            )
            resultIntent.putExtra(
                "wallThickness",
                binding.spWallThickness.selectedItem.toString().toInt()
            )
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }
}
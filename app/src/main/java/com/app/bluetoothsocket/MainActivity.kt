package com.app.bluetoothsocket

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Build
import android.os.Build.VERSION
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : AppCompatActivity() {

    private var bluetoothPermission = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bluetoothScan()
    }

    private fun bluetoothScan() {
        val bluetoothManage: BluetoothManager = getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter: BluetoothAdapter = bluetoothManage.adapter
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Device Does Not Support Bluetooth", Toast.LENGTH_SHORT).show()
        } else {
            if (VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    blPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                blPermissionLauncher.launch(Manifest.permission.BLUETOOTH_ADMIN)
            }
        }
    }


    private val blPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                val bluetoothManage: BluetoothManager = getSystemService(BluetoothManager::class.java)
                val bluetoothAdapter: BluetoothAdapter = bluetoothManage.adapter
                bluetoothPermission = true
                if (!bluetoothAdapter.isEnabled) {
                    val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    btActivityResultLauncher.launch(enableIntent)
                }else{
                    scan()
                }
            }else  bluetoothPermission=false
        }

    private val btActivityResultLauncher= registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        result :ActivityResult -> if (result.resultCode == RESULT_OK) scan()
    }



    private fun scan(){
        Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show()
    }


}
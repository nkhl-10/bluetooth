package com.app.classicbluetooth.bluetooth

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import com.app.classicbluetooth.MainActivity
import com.app.classicbluetooth.Utils.ToastUtils
import java.io.IOException


@Suppress("DEPRECATION")
@SuppressLint("MissingPermission")
class BluetoothHelper(private val context: Context) {

    private val bluetoothManager: BluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter = bluetoothManager.adapter


    fun bluetoothEnable(activity: Activity) {
        if (bluetoothAdapter == null) {
            ToastUtils.showToast(context, "Device doesn't support Bluetooth")
            return
        }
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
    }

    fun bluetoothDisable() {
        if (bluetoothAdapter.isEnabled) {
            bluetoothAdapter.disable()
            ToastUtils.showToast(context, "Bluetooth disabled")
        }
    }

    fun getPairedDevices(): Set<BluetoothDevice> = bluetoothAdapter.bondedDevices?.map { it }?.toSet() ?: emptySet()

    fun isBluetoothEnable() : Boolean = bluetoothAdapter.isEnabled

    fun startDiscovery() = bluetoothAdapter.startDiscovery()

    fun discoverable(activity: Activity){
        val discoverableIntent: Intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 10000)
        }
        activity.startActivityForResult(discoverableIntent, REQUEST_DISCOVERABLE)
    }

     fun connectToDevice(device: BluetoothDevice) {
         startDiscovery()
         val socket: BluetoothSocket = device.createRfcommSocketToServiceRecord(MainActivity.UUID_KEY)
        try {
            socket.connect()
        } catch (e: IOException) {
            e.printStackTrace()
            ToastUtils.showToast(context,e.toString())
        }
    }


    //For Pairing
    fun pairDevice(device: BluetoothDevice) {
        try {
            ToastUtils.showToast(context,"Start Pairing...")
            val method = device.javaClass.getMethod("createBond")
            method.invoke(device)
            ToastUtils.showToast(context,"Pairing finished.")
        } catch (e: Exception) {
            ToastUtils.showToast(context,e.message.toString())
        }
    }

    fun pairDevice(device: BluetoothDevice?,activity: Activity) {
        val intent = Intent( "android.bluetooth.device.action.PAIRING_REQUEST")
        intent.putExtra("android.bluetooth.device.extra.DEVICE", device)
        intent.putExtra( "android.bluetooth.device.extra.PAIRING_VARIANT", 0)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        activity.startActivityForResult(intent, 0)
    }

    //For UnPairing
    private fun unpairDevice(device: BluetoothDevice) {
        try {
            ToastUtils.showToast(context,"Start UnPairing...")
            val method = device.javaClass.getMethod("removeBond")
            method.invoke(device)
            ToastUtils.showToast(context,"UnPairing finished.")
        } catch (e: Exception) {
            ToastUtils.showToast(context,e.message.toString())
        }
    }


    companion object {
        const val REQUEST_ENABLE_BT = 1
        const val REQUEST_DISCOVERABLE=100
    }
}

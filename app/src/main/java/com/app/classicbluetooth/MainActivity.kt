package com.app.classicbluetooth

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListAdapter
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.app.classicbluetooth.Utils.ToastUtils
import com.app.classicbluetooth.bluetooth.BluetoothHelper
import com.app.classicbluetooth.databinding.ActivityMainBinding
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID


@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    private lateinit var binding :ActivityMainBinding
    private lateinit var bluetooth:BluetoothHelper


    private lateinit var sendReceive : SendReceive
    private val pairedDevicesList = ArrayList<BluetoothDevice>()
    private val availableDevicesList = ArrayList<BluetoothDevice>()
    private val messageList = ArrayList<String>()

    private lateinit var pairedAdapter:ArrayAdapter<BluetoothDevice>
    private lateinit var availableAdapter:ArrayAdapter<BluetoothDevice>
    private lateinit var messageAdapter:ArrayAdapter<String>

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        bluetooth = BluetoothHelper(this)
        pairedAdapter = object : ArrayAdapter<BluetoothDevice>(this, android.R.layout.simple_list_item_1, pairedDevicesList) {
            @SuppressLint("MissingPermission")
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val bluetoothDevice = pairedDevicesList[position]
                val name = bluetoothDevice.name
                (view as TextView).text = name
                return view
            }
        }
        availableAdapter = object : ArrayAdapter<BluetoothDevice>(this, android.R.layout.simple_list_item_1, availableDevicesList) {
            @SuppressLint("MissingPermission")
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val bluetoothDevice = availableDevicesList[position]
                val name = bluetoothDevice.name ?: "Unknown Device"
                (view as TextView).text = name
                return view
            }
        }
        messageAdapter =  ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, messageList)

        if (bluetooth.isBluetoothEnable() == true){
            pairedDevicesList.clear()
            pairedDevices(pairedAdapter)
        }else{
            bluetooth.bluetoothEnable(this@MainActivity)
        }


        val bluetoothManager: BluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter: BluetoothAdapter = bluetoothManager.adapter

        binding.apply {

            messageListview.adapter = messageAdapter

            btOn.setOnClickListener {
                bluetooth.bluetoothEnable(this@MainActivity)
                pairedDevicesList.clear()
                pairedDevices(pairedAdapter)
            }
            btOff.setOnClickListener {
                bluetooth.bluetoothDisable()
            }
            scanBtn.setOnClickListener {
                if (bluetooth.isBluetoothEnable() == true){
                    if (checkPermission()){
                        bluetooth.startDiscovery()
                        availableDevicesList.clear()
                        getScanDevices()
                        binding.availableListview.adapter =availableAdapter
                    }
                }else{
                    bluetooth.bluetoothEnable(this@MainActivity)
                }
            }
            discoverable.setOnClickListener {
                bluetooth.discoverable(this@MainActivity)
            }

            listen.setOnClickListener {
                val serverClass =ServerClass(bluetoothAdapter)
                serverClass.start()
            }
            send.setOnClickListener {
                val messageToSend = messageBox.text.toString()
                val bytesToSend = messageToSend.toByteArray(Charsets.UTF_8)
                sendReceive.write(bytesToSend)
                binding.messageBox.text.clear()
            }

            pairedListview.setOnItemClickListener { _, _, position, _ ->
                val bluetoothDevice = pairedAdapter.getItem(position)
                if (bluetoothDevice != null) {
//                    bluetooth.pairDevice(bluetoothDevice)
                     val clientClass = ClientClass(bluetoothDevice,bluetoothAdapter)
                    clientClass.start()
                    binding.status.text = "Connecting.."
                }else{
                    ToastUtils.showToast(this@MainActivity,"Make Sure Device Bluetooth are enable")
                }
            }
            availableListview.setOnItemClickListener { _, _, position, _ ->
                val bluetoothDevice = availableAdapter.getItem(position)
                if (bluetoothDevice != null) {
                    bluetooth.pairDevice(bluetoothDevice,this@MainActivity)
                }else{
                    ToastUtils.showToast(this@MainActivity,"Make Sure Device Bluetooth are enable")
                }
            }

        }






    }

    private fun pairedDevices(pairedAdapter:ListAdapter){
        if (bluetooth.isBluetoothEnable() == true){
            pairedDevicesList.addAll(bluetooth.getPairedDevices())
            binding.pairedListview.adapter =pairedAdapter
        }
    }





    private fun getScanDevices()=registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_FOUND))

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when(intent.action) {
                BluetoothAdapter.ACTION_DISCOVERY_STARTED->{
                    ToastUtils.showToast(this@MainActivity,"ACTION_DISCOVERY_STARTED")
                }
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    val name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME)
                    device?.let {
                        availableDevicesList.add(device)
                    }
                    availableAdapter.notifyDataSetChanged()

                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED->{
                    ToastUtils.showToast(this@MainActivity,"ACTION_DISCOVERY_FINISHED")
                }
            }
        }
    }

    private fun getScanDevicesStop()=unregisterReceiver(receiver)

    override fun onDestroy() {
        super.onDestroy()
        getScanDevicesStop()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun checkPermission():Boolean {
        val permissions = arrayOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.BLUETOOTH_ADMIN,
            android.Manifest.permission.BLUETOOTH_CONNECT
            ,android.Manifest.permission.BLUETOOTH_SCAN
        )
        val permissionsToRequest = mutableListOf<String>()
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission)
            }
        }
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), 1)
        }
        return true
    }


    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == BluetoothHelper.REQUEST_ENABLE_BT) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    // Bluetooth was successfully enabled
                    ToastUtils.showToast(this, "Bluetooth enabled")
                }
                Activity.RESULT_CANCELED -> {
                    // User canceled enabling Bluetooth
                    ToastUtils.showToast(this, "Bluetooth enabling canceled")
                }
            }
        }

    }

    var handler: Handler = Handler(Handler.Callback { msg ->
        when (msg.what) {
            STATE_LISTENING -> binding.status.text = "Listening"
            STATE_CONNECTING ->  binding.status.text ="Connecting"
            STATE_CONNECTED ->  binding.status.text ="Connected"
            STATE_CONNECTION_FAILED -> binding.status.text ="Connection Failed"
            STATE_MESSAGE_RECEIVED -> {
                val readBuff = msg.obj as ByteArray
                val tempMsg = String(readBuff, 0, msg.arg1)
                messageList.add(tempMsg)
                messageAdapter.notifyDataSetChanged()
            }
        }
        true
    })


    @SuppressLint("MissingPermission")
    inner class ServerClass(bluetoothAdapter: BluetoothAdapter) : Thread() {
        private var serverSocket: BluetoothServerSocket? = null
        init {
            try {
                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, UUID_KEY)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        override fun run() {
            var socket: BluetoothSocket? = null
            while (socket == null) {
                try {
                    val message = Message.obtain()
                    message.what = STATE_CONNECTING
                    handler.sendMessage(message)
                    socket = serverSocket?.accept()
                }
                catch (e: IOException) {
                    e.printStackTrace()
                    val message = Message.obtain()
                    message.what = STATE_CONNECTION_FAILED
                    handler.sendMessage(message)
                }

                if (socket!= null){
                    val message = Message.obtain()
                    message.what = STATE_CONNECTED
                    handler.sendMessage(message)

                    sendReceive = SendReceive(socket)
                    sendReceive.start()

                    return
                }

            }
        }
    }

    @SuppressLint("MissingPermission")
    inner class ClientClass(private val device: BluetoothDevice,private val bluetoothAdapter: BluetoothAdapter) : Thread() {
        private var socket: BluetoothSocket? = null
        init {
            try {
                socket = device.createRfcommSocketToServiceRecord(UUID_KEY)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        override fun run() {
            bluetoothAdapter.cancelDiscovery()
            try {
                socket?.connect()
                val message = Message.obtain()
                message.what = STATE_CONNECTED
                handler.sendMessage(message)
                sendReceive = SendReceive(socket!!)
                sendReceive.start()
            } catch (e: IOException) {
                e.printStackTrace()
                val message = Message.obtain()
                message.what = STATE_CONNECTION_FAILED
                handler.sendMessage(message)
            }
        }
    }

    inner class SendReceive(private val bluetoothSocket: BluetoothSocket) : Thread() {
        private val inputStream: InputStream?
        private val outputStream: OutputStream?
        init {
            var tempIn: InputStream? = null
            var tempOut: OutputStream? = null
            try {
                tempIn = bluetoothSocket.inputStream
                tempOut = bluetoothSocket.outputStream
            } catch (e: IOException) {
                e.printStackTrace()
            }
            inputStream = tempIn
            outputStream = tempOut
        }

        override fun run() {
            val buffer = ByteArray(1024)
            var bytes: Int
            while (true) {
                try {
                    bytes = inputStream?.read(buffer) ?: -1
                    handler.obtainMessage(STATE_MESSAGE_RECEIVED, bytes, -1, buffer).sendToTarget()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        fun write(bytes: ByteArray) {
            try {
                outputStream?.write(bytes)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }


    companion object {
        const val APP_NAME = "YourAppName"
        val UUID_KEY: UUID = UUID.fromString("8eb07417-d7e0-4c30-8ecf-53f179832472")
        const val STATE_LISTENING = 1
        const val STATE_CONNECTING = 2
        const val STATE_CONNECTED = 3
        const val STATE_CONNECTION_FAILED = 4
        const val STATE_MESSAGE_RECEIVED = 5
    }
}

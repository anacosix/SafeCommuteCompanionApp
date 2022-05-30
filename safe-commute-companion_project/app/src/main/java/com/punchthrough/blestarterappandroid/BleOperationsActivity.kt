/*
 * Copyright 2019 Punch Through Design LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.punchthrough.blestarterappandroid

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothDevice
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.punchthrough.blestarterappandroid.ble.ConnectionEventListener
import com.punchthrough.blestarterappandroid.ble.ConnectionManager
import org.jetbrains.anko.alert
import java.nio.charset.Charset
import java.util.UUID

@SuppressLint("LogNotTimber")
class BleOperationsActivity : AppCompatActivity() {
    var lastUpdateEmergencyCounter : Long? = null
    var bicycleCounter = 0
    var carCounter = 0
    var emergencyCounter = 0
    var counterBicycle : TextView? = null
    var counterEmergency : TextView? = null
    private var counterCar : TextView? = null
    private var isRouteStarted : Boolean? = null
    private var readyToStartCounter : Boolean? = null

    lateinit var notificationChannel: NotificationChannel
    lateinit var notificationManager: NotificationManager
    lateinit var builder: Notification.Builder
    private val channelId = "55555"
    private val description = "Notification"

    private lateinit var device: BluetoothDevice
    private val characteristics by lazy {
        ConnectionManager.servicesOnDevice(device)?.flatMap { service ->
            service.characteristics ?: listOf()
        } ?: listOf()
    }

    private var notifyingCharacteristics = mutableListOf<UUID>()

    override fun onCreate(savedInstanceState: Bundle?) {
        ConnectionManager.registerListener(connectionEventListener)
        setContentView(R.layout.activity_ble_operations)
        super.onCreate(savedInstanceState)

        device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            ?: error("Missing BluetoothDevice from MainActivity!")

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
            title = getString(R.string.ble_playground)
        }

        counterBicycle = findViewById(R.id.counterBicycle)
        counterEmergency = findViewById(R.id.counterEmergency)
        counterCar = findViewById(R.id.counterCar)
        var startRoute : Button = findViewById(R.id.startRouteButton)
        var stopButton : Button = findViewById(R.id.stopRouteButton)
        lastUpdateEmergencyCounter = System.currentTimeMillis()

        startRoute.setOnClickListener{
            isRouteStarted = true
            readyToStartCounter = true
            startRoute.visibility = View.INVISIBLE
            stopButton.visibility = View.VISIBLE
        }

        stopButton.setOnClickListener{
            readyToStartCounter = false
            stopButton.visibility = View.INVISIBLE
            startRoute.visibility = View.VISIBLE
        }

        ConnectionManager.enableNotifications(device, characteristics.component4())
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private val connectionEventListener by lazy {
        ConnectionEventListener().apply {
            onDisconnect = {
                runOnUiThread {
                    alert {
                        title = "Disconnected"
                        message = "Disconnected from device."
                        positiveButton("OK") { onBackPressed() }
                    }.show()
                }
            }

            onCharacteristicChanged = { _, characteristic ->
                var receivedData = characteristic.value.toString(Charset.defaultCharset())
                receivedData = receivedData.replace("{","")
                    .replace("}","")
                    .replace(":","")
                    .replace("\"","")
                    .replace("sound","")
                if(isRouteStarted == true){
                    bicycleCounter = 0
                    carCounter = 0
                    emergencyCounter = 0
                    isRouteStarted = false
                }
                if(readyToStartCounter == true){
                    if(receivedData[0] == 'b'){
                        bicycleCounter = bicycleCounter!! + 1
                        counterBicycle!!.text = bicycleCounter.toString()
                        notifyApproaching("bicycle")
                    } else if(receivedData[0] == 'c'){
                        carCounter = carCounter!! + 1
                        counterCar!!.text = carCounter.toString()
                        notifyApproaching("car")
                    } else if(receivedData[0] == 'e'){
                        if(System.currentTimeMillis() - lastUpdateEmergencyCounter!! > 10000){
                            emergencyCounter = emergencyCounter!! + 1
                            counterEmergency!!.text = emergencyCounter.toString()
                            lastUpdateEmergencyCounter = System.currentTimeMillis()
                            notifyApproaching("emergency vehicle")
                        }
                    }
                }
            }

            onNotificationsEnabled = { _, characteristic ->
                notifyingCharacteristics.add(characteristic.uuid)
            }

            onNotificationsDisabled = { _, characteristic ->
                notifyingCharacteristics.remove(characteristic.uuid)
            }
        }
    }

    private fun notifyApproaching(sound : String) {
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel = NotificationChannel(channelId, description, NotificationManager .IMPORTANCE_HIGH)
            notificationChannel.enableVibration(true)
            notificationManager.createNotificationChannel(notificationChannel)

            builder = Notification.Builder(this, channelId).setContentTitle("Watch out!")
                .setContentText("Approaching $sound detected!").setSmallIcon(R.mipmap.ic_launcher_round).setLargeIcon(
                    BitmapFactory.decodeResource(this.resources, R.mipmap.ic_launcher_round)).setContentIntent(pendingIntent)
        }
        notificationManager.notify(55555, builder.build())
    }


}

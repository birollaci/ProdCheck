package com.example.prodcheck;

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.prodcheck.databinding.ActivityScanBinding
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import java.io.IOException

class ScanActivity : AppCompatActivity() {

    private val requestCodeCameraPermission = 1001
    private lateinit var cameraSource: CameraSource
    private lateinit var barcodeDetector: BarcodeDetector
    private var scannedValue = ""
    private lateinit var binding: ActivityScanBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        binding = ActivityScanBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        if (ContextCompat.checkSelfPermission(this@ScanActivity, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            askForCameraPermission()
        } else {
            setupControls()
        }
    }

    private fun askForCameraPermission() {
        ActivityCompat.requestPermissions(
            this@ScanActivity,
            arrayOf(android.Manifest.permission.CAMERA),
            requestCodeCameraPermission
        )
    }

    private fun setupControls() {
        barcodeDetector =
            BarcodeDetector.Builder(this).setBarcodeFormats(Barcode.ALL_FORMATS).build()

        cameraSource = CameraSource.Builder(this, barcodeDetector)
            .setRequestedPreviewSize(1000, 500)
            .setAutoFocusEnabled(true) //you should add this feature
            .build()

        binding.cameraSurfaceView.getHolder().addCallback(object : SurfaceHolder.Callback {
            @SuppressLint("MissingPermission")
            override fun surfaceCreated(holder: SurfaceHolder) {
                try {
                    //Start preview after 1s delay
                    cameraSource.start(holder)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            @SuppressLint("MissingPermission")
            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
                try {
                    cameraSource.start(holder)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                cameraSource.stop()
            }
        })


        barcodeDetector.setProcessor(object : Detector.Processor<Barcode> {
            override fun release() {
                Toast.makeText(applicationContext, "Scanner has been closed", Toast.LENGTH_LONG)
                    .show()
            }

            override fun receiveDetections(detections: Detector.Detections<Barcode>) {
                val barcodes = detections.detectedItems
                if (barcodes.size() == 1) {
                    scannedValue = barcodes.valueAt(0).rawValue

                    //Don't forget to add this line printing value or finishing activity must run on main thread
                    runOnUiThread {
                        // Start another activity
                        val intent = Intent(this@ScanActivity, MainActivity::class.java)
                        intent.putExtra("barCode", scannedValue);
                        startActivity(intent)
                        cameraSource.stop()
                        finish()
                    }
                }else
                {
                    Toast.makeText(this@ScanActivity, "value- else", Toast.LENGTH_LONG).show()

                }
            }
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestCodeCameraPermission && grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupControls()
            } else {
                Toast.makeText(applicationContext, "Permission Denied", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraSource.stop()
    }
}
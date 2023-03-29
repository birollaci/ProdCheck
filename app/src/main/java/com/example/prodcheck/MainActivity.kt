package com.example.prodcheck

import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.prodcheck.DataManager.barcode
import com.example.prodcheck.DataManager.nrScans
import com.example.prodcheck.DataManager.nrScansDB
import com.example.prodcheck.DataManager.nrValidScans
import com.example.prodcheck.DataManager.nrValidScansDB
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    private val dialog: Dialog = Dialog()
    private var warranty: Boolean = false
    private var valid: Boolean = false
    private var type: Int = 10
    var myNowDate: Calendar = Calendar.getInstance()
    var manufacturingDate: Calendar = Calendar.getInstance(Locale.US)
    var lineToProdcheck = ""
    val dbHelper = DatabaseHelper(this)

    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialization
        setText("Barcode: $barcode")
        val rate = if(nrScans == 0) {
            0
        } else {
            (nrValidScans * 100) / nrScans
        }

        var str = "Scans: $nrScans\nValid scans: $nrValidScans\nCorrectness rate: $rate%"
        actualSessionTextView.text = str

        // query from database
        dbHelper.select() // setting nrScansDB and nrValidScansDB

        // Initialization Since installation
        val rateDB = if(nrScansDB == 0) {
            0
        } else {
            (nrValidScansDB * 100) / nrScansDB
        }

        var strDB = "Scans: $nrScansDB\nValid scans: $nrValidScansDB\nCorrectness rate: $rateDB%"
        installationSessionTextView.text = strDB

        // CAMERA
        if(intent.getStringExtra("barCode").toString().isNotEmpty() && intent.getStringExtra("barCode") != null){
            barcode = intent.getStringExtra("barCode").toString()
            setText("Barcode: $barcode")

            barcodeAnalyzer()

            manualBarcodeEditText.setText("")
        }

        // Camera permission
        if (ContextCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(android.Manifest.permission.CAMERA),
                1001
            )
        }

        // WRITE_EXTERNAL_STORAGE permission
        if (ContextCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                112
            )
        }

        // MANUAL (By send button)
        btnSend.setOnClickListener{
            if(manualBarcodeEditText.text.toString().isEmpty()){
                alert("Please complete the manual barcode field!", "Info")
            } else {
                barcode = manualBarcodeEditText.text.toString()
                setText("Barcode: $barcode")

                barcodeAnalyzer()
            }
        }

        // MANUAL (ENTER)
        manualBarcodeEditText.setOnEditorActionListener {view, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE ||
                keyEvent == null ||
                keyEvent.keyCode == KeyEvent.KEYCODE_ENTER) {

                if(manualBarcodeEditText.text.toString().isEmpty()){
                    alert("Please complete the manual barcode field!", "Info")
                } else {
                    barcode = manualBarcodeEditText.text.toString()
                    setText("Barcode: $barcode")

                    barcodeAnalyzer()
                }
                true
            }
            false
        }

        btnCamera.setOnClickListener{
            startActivity(Intent(this, ScanActivity::class.java))
        }

    }

    fun writeLineToFile(line: String) {
        val fileName = "prodcheck.txt"
        val file = File("${System.getenv("EXTERNAL_STORAGE")}/Download/$fileName")
        val exists = file.exists()

        if (!exists) {
            file.createNewFile()
        }

        file.appendText("$line\n")
    }

    
    fun barcodeAnalyzer() {
        if (ContextCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                112
            )
        } else {
            // initialize the line to the File
            lineToProdcheck = ""

            // Datum + Tab
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            val date: String = sdf.format(Date())
            lineToProdcheck += date + "\t"
            // Barkod+ Tab
            lineToProdcheck += barcode + "\t"

            var status = ""

            // itt a type erteket kap
            valid = validateBarcode(barcode!!)
            if(valid) {
                warranty = getWarranty(myNowDate, manufacturingDate, type)
                if(warranty){ // OK
                    alert("The product warranty is good!","info")
                    status = "OK"
                    nrValidScans++
                } else { // EXPIRAT
                    alert("The product warranty has expired!","Alert")
                    status = "EXPIRAT"
                }
            } else { // INVALID
                alert("The barcode is invalid!", "Alert")
                status = "INVALID"
            }

            lineToProdcheck += status
            writeLineToFile(lineToProdcheck)
            nrScans++

            // insert to database
            val data = DatabaseHelper.Data(barcode!!, status)
            dbHelper.addData(data)
            dbHelper.select()

            val rateDB = (nrValidScansDB * 100) / nrScansDB
            var strDB = "Scans: $nrScansDB\nValid scans: $nrValidScansDB\nCorrectness rate: $rateDB%"
            installationSessionTextView.text = strDB

            val rate = (nrValidScans * 100) / nrScans
            var str = "Scans: $nrScans\nValid scans: $nrValidScans\nCorrectness rate: $rate%"
            actualSessionTextView.text = str
        }
    }

    // If property = "Info", Info box
    // If property = "Alert", Alert box
    private fun alert(txt: String, property: String) {
        dialog.showDefaultDialog(this, txt, property)
    }

    fun isNumeric(toCheck: String): Boolean {
        return toCheck.all { char -> char.isDigit() }
    }

    // true - valid => go to getWarranty
    // false - invalid
    fun validateBarcode(vBarcode: String): Boolean {

//        myNowDate[Calendar.HOUR] = 0
//        myNowDate[Calendar.MINUTE] = 0
//        myNowDate[Calendar.SECOND] = 0

        manufacturingDate[Calendar.HOUR] = 0
        manufacturingDate[Calendar.MINUTE] = 0
        manufacturingDate[Calendar.SECOND] = 0

        var year: Int
        var week: Int
        var day: Int
        if(vBarcode.length == 11){
            if(!isNumeric(vBarcode.substring(0,6))){
                alert("The first 6 characters must be numbers!", "alert")
                return false
            } else{
                type = vBarcode.substring(0,1).toInt()
                year = vBarcode.substring(1,3).toInt()
                week = vBarcode.substring(3,5).toInt()
                day = vBarcode.substring(5,6).toInt()

                val myFourDigitYear = yearGenerator(myNowDate.weekYear, year)

                val generatedWeekDay = getWeekDay(day)

                return if(generatedWeekDay == 0) {
                    alert("Weekday problem! (6. character) Good: 1 -> 7", "alert")
                    false
                } else if(week == 0 || week > 52) {
                    alert("Week problem! (4. and 5. character) Good: 01 -> 52", "alert")
                    false
                } else {
                    // Set manufacturing date
                    manufacturingDate[Calendar.YEAR] = myFourDigitYear
                    manufacturingDate[Calendar.WEEK_OF_YEAR] = week
                    manufacturingDate[Calendar.DAY_OF_WEEK] = generatedWeekDay

                    if(manufacturingDate.after(myNowDate)) {
                        alert("The production date is invalid because it is found after today's date!", "alert")
                        false
                    } else {
                        true
                    }
                }
            }
        }else {
            alert("The barcode must contains 11 characters!", "Alert")
            return false
        }
    }

    fun yearGenerator(now: Int, year: Int): Int {
        return if(now < 1900 || now > 9999 || year < 0 || year > 99){
            alert("Year is not good", "alert")
            0
        } else {
            val nowTwoDigit = now % 100
            val nowMinusDigits = now - nowTwoDigit
            // Interval: -70 -> +30
            // 19xx
            if(nowTwoDigit + 30 <= year){
                nowMinusDigits - 100 + year
            }
            // 20xx
            else {
                nowMinusDigits + year
            }
        }
    }

    // day 1 -> 7
    fun getWeekDay(day: Int): Int{
        return when (day) {
            1 -> Calendar.MONDAY
            2 -> Calendar.TUESDAY
            3 -> Calendar.WEDNESDAY
            4 -> Calendar.THURSDAY
            5 -> Calendar.FRIDAY
            6 -> Calendar.SATURDAY
            7 -> Calendar.SUNDAY
            else -> 0
        }
    }

    fun getWarranty(nowDate: Calendar, manufDate: Calendar, type: Int): Boolean {
        if(type > 9 || type < 0) {
            alert("Product type is invalid!", "alert")
        }
        // paros, 30 nap
        if(type % 2 == 0) {
            var expirationDate = manufDate
            expirationDate.add(Calendar.DAY_OF_YEAR, 30) // szavatossag ideje
            return if(nowDate.before(expirationDate)) { // ok
                true
            } else {
                alert("The product warranty has expired! Warranty: 30 days", "alert")
                false
            }
        }

        // paratlan - 7, 10 nap
        if(type % 2 == 1 && type != 7) {
            var expirationDate = manufDate
            expirationDate.add(Calendar.DAY_OF_YEAR, 10)
            return if(nowDate.before(expirationDate)) { // ok
                true
            } else {
                alert("The product warranty has expired! Warranty: 10 days", "alert")
                false
            }
        }

        if(type == 7) {
            var expirationDate = manufDate
            expirationDate.add(Calendar.YEAR, 1)
            return if(nowDate.before(expirationDate)) { // ok
                true
            } else {
                alert("The product warranty has expired! Warranty: 1 year", "alert")
                false
            }
        }

        return false
    }

    // ---------------------------------------------------- Scanner ---------------------------------------------------------------
    override fun onResume() {
        super.onResume()
        //Register receiver so my app can listen for intents which action is ACTION_BARCODE_DATA
        val intentFilter = IntentFilter(ACTION_BARCODE_DATA)
        registerReceiver(barcodeDataReceiver, intentFilter)

        //Will setup the new configuration of the scanner.
        claimScanner()
    }

    // Physical scanner
    private val barcodeDataReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        
        override fun onReceive(context: Context, intent: Intent) {
            Toast.makeText(context, "Received the Broadcast Intent", Toast.LENGTH_SHORT).show()
            val action = intent.action
            println("Action Received: $action")
            if (ACTION_BARCODE_DATA == action) {
                val version = intent.getIntExtra("version", 0)
                if (version >= 1) {
                    barcode = intent.getStringExtra("data").toString()
                    // validate physical scanner
                    setText("Barcode: $barcode")

                    barcodeAnalyzer()

                    manualBarcodeEditText.setText("")
                }
            }
        }
    }

    private fun claimScanner() {
        val properties = Bundle()


        //When we press the scan button and read a barcode, a new Broadcast intent will be launched by the service
        properties.putBoolean("DPR_DATA_INTENT", true)

        //That intent will have the action "ACTION_BARCODE_DATA"
        // We will capture the intents with that action (every scan event while in the application)
        // in our BroadcastReceiver barcodeDataReceiver.
        properties.putString("DPR_DATA_INTENT_ACTION", ACTION_BARCODE_DATA)
        //properties.putString("TRIGGER_MODE", "continuous");
        val intent = Intent()
        intent.action = ACTION_CLAIM_SCANNER

        /*
         * We use setPackage() in order to send an Explicit Broadcast Intent, since it is a requirement
         * after API Level 26+ (Android 8)
         */intent.setPackage("com.intermec.datacollectionservice")

        //We will use the internal scanner
        intent.putExtra(EXTRA_SCANNER, "dcs.scanner.imager")

        /*
        We are using "MyProfile1", so a profile with this name has to be created in Scanner settings:
               Android Settings > Honeywell Settings > Scanning > Internal scanner > "+"
        - If we use "DEFAULT" it will apply the settings from the Default profile in Scanner settings
        - If not found, it will use Factory default settings.
         */intent.putExtra(EXTRA_PROFILE, "MyProfile1")
        intent.putExtra(EXTRA_PROPERTIES, properties)
        sendBroadcast(intent)
        Toast.makeText(this, "Scanner Claimed", Toast.LENGTH_SHORT).show()
    }

    private fun setText(text: String) {
        if (barcodeTextView != null) {
            runOnUiThread { barcodeTextView!!.text = text }
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(barcodeDataReceiver)
        releaseScanner()
    }

    private fun releaseScanner() {
        val intent = Intent()
        intent.action = ACTION_RELEASE_SCANNER
        sendBroadcast(intent)
    }

    private fun bytesToHexString(array: ByteArray?): String {
        var s = "[]"
        if (array != null) {
            s = "["
            for (i in array.indices) {
                s += "0x" + Integer.toHexString(array[i].toInt()) + ", "
            }
            s = s.substring(0, s.length - 2) + "]"
        }
        return s
    }

    companion object {
        private val TAG = "IntentApiSample"
        private val EXTRA_CONTROL = "com.honeywell.aidc.action.ACTION_CONTROL_SCANNER"
        private val EXTRA_SCAN = "com.honeywell.aidc.extra.EXTRA_SCAN"
        val ACTION_BARCODE_DATA = "com.honeywell.sample.intentapisample.BARCODE"

        /**
         * Honeywell DataCollection Intent API
         * Claim scanner
         * Permissions:
         * "com.honeywell.decode.permission.DECODE"
         */
        val ACTION_CLAIM_SCANNER = "com.honeywell.aidc.action.ACTION_CLAIM_SCANNER"

        /**
         * Honeywell DataCollection Intent API
         * Release scanner claim
         * Permissions:
         * "com.honeywell.decode.permission.DECODE"
         */
        val ACTION_RELEASE_SCANNER = "com.honeywell.aidc.action.ACTION_RELEASE_SCANNER"

        /**
         * Honeywell DataCollection Intent API
         * Optional. Sets the scanner to claim. If scanner is not available or if extra is not used,
         * DataCollection will choose an available scanner.
         * Values : String
         * "dcs.scanner.imager" : Uses the internal scanner
         * "dcs.scanner.ring" : Uses the external ring scanner
         */
        val EXTRA_SCANNER = "com.honeywell.aidc.extra.EXTRA_SCANNER"

        /**
         * Honeywell DataCollection Instent API
         * Optional. Sets the profile to use. If profile is not available or if extra is not used,
         * the scanner will use factory default properties (not "DEFAULT" profile properties).
         * Values : String
         */
        val EXTRA_PROFILE = "com.honeywell.aidc.extra.EXTRA_PROFILE"

        /**
         * Honeywell DataCollection Intent API
         * Optional. Overrides the profile properties (non-persistend) until the next scanner claim.
         * Values : Bundle
         */
        val EXTRA_PROPERTIES = "com.honeywell.aidc.extra.EXTRA_PROPERTIES"
    }

}
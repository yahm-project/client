package it.unibo.yahm.client.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.requestPermissions
import it.unibo.yahm.R
import it.unibo.yahm.client.sensors.ReactiveLocation
import it.unibo.yahm.client.sensors.ReactiveSensor
import it.unibo.yahm.client.sensors.SensorCombiners
import it.unibo.yahm.client.sensors.SensorType
import it.unibo.yahm.client.utils.CsvFile
import it.unibo.yahm.client.utils.FunctionUtils.median
import it.unibo.yahm.client.utils.FunctionUtils.stdDeviation
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis.AxisDependency;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;


class TrainingActivity : AppCompatActivity() {

    private var isMeasuring = false
    private lateinit var startCallback: () -> Unit
    private lateinit var stopCallback: () -> Unit
    private lateinit var chart: LineChart;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training)
        setSupportActionBar(findViewById(R.id.my_toolbar))
        checkPermissions()
        initChart()
        initSavingEvents()
    }

    private fun initChart() {
        chart = findViewById(R.id.sensorChart)
        chart.setTouchEnabled(false)
        chart.description.isEnabled = false
        val rightAxis = chart.axisRight
        val leftAxis = chart.axisLeft
        leftAxis.axisMaximum = 9f
        leftAxis.axisMinimum = -9f
        rightAxis.axisMaximum = 9f
        rightAxis.axisMinimum = -9f
        leftAxis.setDrawLabels(false)
        leftAxis.setDrawGridLines(false)
        rightAxis.setDrawGridLines(false)
        chart.xAxis.setDrawLabels(false)
        chart.xAxis.setDrawGridLines(false)
    }

    private fun addAccelerationEntry(acc_x: Float, acc_y: Float, acc_z: Float) {

        var data = chart.data;
        if (data == null) {
            data = LineData();
            chart.data = data
        }
        var xAccDataSet = data.getDataSetByIndex(0)
        var yAccDataSet = data.getDataSetByIndex(1)
        var zAccDataSet = data.getDataSetByIndex(2)

        if (xAccDataSet == null) {
            xAccDataSet = createSet("acc_x")
            data.addDataSet(xAccDataSet)
            yAccDataSet = createSet("acc_y")
            data.addDataSet(yAccDataSet);
            zAccDataSet = createSet("acc_z")
            data.addDataSet(zAccDataSet);
        }

        data.addEntry( Entry(xAccDataSet.entryCount.toFloat(), acc_x), 0);
        data.addEntry( Entry(yAccDataSet.entryCount.toFloat(), acc_y), 1);
        data.addEntry( Entry(zAccDataSet.entryCount.toFloat(), acc_z), 2);
        data.notifyDataChanged();
        chart.notifyDataSetChanged();
        chart.setVisibleXRangeMaximum(30f);
        //chart.setVisibleYRangeMaximum(15, AxisDependency.LEFT);
        chart.moveViewTo(xAccDataSet.entryCount - 30f, 0f, AxisDependency.LEFT);

    }

    private fun createSet(name: String): LineDataSet {
        val data = chart.data;
        val count = (data.dataSetCount + 1);
        val color = ColorTemplate.VORDIPLOM_COLORS[count % ColorTemplate.VORDIPLOM_COLORS.size];
        var set = LineDataSet(null, name);
        set.lineWidth = 2f;
        set.circleRadius = 1f;
        set.color = color;
        set.setCircleColor(color);
        set.axisDependency = AxisDependency.LEFT;
        set.setDrawValues(false)

        return set;
    }


    private fun initSavingEvents() {
        val spotHoleButton = findViewById<Button>(R.id.spotHole)
        val spotBackButton = findViewById<Button>(R.id.spotBack)
        val spotRoadJointButton = findViewById<Button>(R.id.spotRoadJoint)
        val spotManHoleButton = findViewById<Button>(R.id.spotManHole)

        spotHoleButton.isEnabled = false
        spotBackButton.isEnabled = false
        spotManHoleButton.isEnabled = false
        spotRoadJointButton.isEnabled = false

        lateinit var reactiveSensor: ReactiveSensor
        lateinit var reactiveLocation: ReactiveLocation
        lateinit var sensorObservers: SensorCombiners

        val sensorValuesFile = CsvFile(
            "sensor_values", listOf(
                "timestamp",
                "x_min_acc", "y_min_acc", "z_min_acc",
                "x_max_acc", "y_max_acc", "z_max_acc",
                "x_avg_acc", "y_avg_acc", "z_avg_acc",
                "x_med_acc", "y_med_acc", "z_med_acc",
                "x_sdv_acc", "y_sdv_acc", "z_sdv_acc",
                "x_min_ang_vel", "y_min_ang_vel", "z_min_ang_vel",
                "x_max_ang_vel", "y_max_ang_vel", "z_max_ang_vel",
                "x_avg_ang_vel", "y_avg_ang_vel", "z_avg_ang_vel",
                "x_med_ang_vel", "y_med_ang_vel", "z_med_ang_vel",
                "x_sdv_ang_vel", "y_sdv_ang_vel", "z_sdv_ang_vel",
                "latitude", "longitude", "speed"
            ),
            applicationContext
        )
        val obstaclesFile = CsvFile(
            "obstacles", listOf("timestamp", "type"),
            applicationContext
        )

        startCallback = {
            reactiveSensor = ReactiveSensor(applicationContext)
            reactiveLocation = ReactiveLocation(applicationContext)
            sensorObservers = SensorCombiners(reactiveLocation, reactiveSensor)
            sensorValuesFile.open()
            obstaclesFile.open()
            Log.i("TrainingActivity", "Saving to ${sensorValuesFile.fileName}, ${obstaclesFile.fileName}")

            reactiveSensor.observer(SensorType.LINEAR_ACCELERATION).sample(100, TimeUnit.MILLISECONDS).subscribe({
                addAccelerationEntry(it.values[0], it.values[1], it.values[2])
            }, { it.printStackTrace() })

            sensorObservers.combineByTime().subscribe({ cv ->
                sensorValuesFile.writeValue(
                    listOf(
                        cv.timestamp,
                        cv.accelerationValues.map { it.x }.min(),
                        cv.accelerationValues.map { it.y }.min(),
                        cv.accelerationValues.map { it.z }.min(),
                        cv.accelerationValues.map { it.x }.max(),
                        cv.accelerationValues.map { it.y }.max(),
                        cv.accelerationValues.map { it.z }.max(),
                        cv.accelerationValues.map { it.x }.average(),
                        cv.accelerationValues.map { it.y }.average(),
                        cv.accelerationValues.map { it.z }.average(),
                        cv.accelerationValues.map { it.x }.median(),
                        cv.accelerationValues.map { it.y }.median(),
                        cv.accelerationValues.map { it.z }.median(),
                        cv.accelerationValues.map { it.x }.stdDeviation(),
                        cv.accelerationValues.map { it.y }.stdDeviation(),
                        cv.accelerationValues.map { it.z }.stdDeviation(),
                        cv.gyroscopeValues.map { it.x }.min(),
                        cv.gyroscopeValues.map { it.y }.min(),
                        cv.gyroscopeValues.map { it.z }.min(),
                        cv.gyroscopeValues.map { it.x }.max(),
                        cv.gyroscopeValues.map { it.y }.max(),
                        cv.gyroscopeValues.map { it.z }.max(),
                        cv.gyroscopeValues.map { it.x }.average(),
                        cv.gyroscopeValues.map { it.y }.average(),
                        cv.gyroscopeValues.map { it.z }.average(),
                        cv.gyroscopeValues.map { it.x }.median(),
                        cv.gyroscopeValues.map { it.y }.median(),
                        cv.gyroscopeValues.map { it.z }.median(),
                        cv.gyroscopeValues.map { it.x }.stdDeviation(),
                        cv.gyroscopeValues.map { it.y }.stdDeviation(),
                        cv.gyroscopeValues.map { it.z }.stdDeviation(),
                        cv.location?.latitude,
                        cv.location?.longitude,
                        cv.location?.speed
                    )
                )
            }, { it.printStackTrace() })

            val spotButtonClickListener = View.OnClickListener {
                obstaclesFile.writeValue(listOf(System.currentTimeMillis(), (it as Button).text))
            }
            spotHoleButton.setOnClickListener(spotButtonClickListener)
            spotBackButton.setOnClickListener(spotButtonClickListener)
            spotManHoleButton.setOnClickListener(spotButtonClickListener)
            spotRoadJointButton.setOnClickListener(spotButtonClickListener)

            spotHoleButton.isEnabled = true
            spotBackButton.isEnabled = true
            spotManHoleButton.isEnabled = true
            spotRoadJointButton.isEnabled = true
        }


        stopCallback = {
            spotHoleButton.isEnabled = false
            spotBackButton.isEnabled = false
            spotManHoleButton.isEnabled = false
            spotRoadJointButton.isEnabled = false

            reactiveLocation.dispose()
            reactiveSensor.dispose(SensorType.LINEAR_ACCELERATION)
            reactiveSensor.dispose(SensorType.GYROSCOPE)
            sensorValuesFile.close()
            obstaclesFile.close()
        }
    }

    private fun checkPermissions() {
        requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            1
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            1 -> {
                if (grantResults.any { it != PackageManager.PERMISSION_GRANTED }) {
                    this.finish()
                    exitProcess(1)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean { // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.training_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean { // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id: Int = item.itemId

        if (id == R.id.toggleMeasuring) {
            if (isMeasuring) {
                Toast.makeText(applicationContext, "Stopped.", Toast.LENGTH_SHORT).show()
                item.icon = getDrawable(R.drawable.ic_play_arrow_24dp)
                stopCallback()
            } else {
                Toast.makeText(applicationContext, "Started!", Toast.LENGTH_SHORT).show()
                item.icon = getDrawable(R.drawable.ic_stop_24dp)
                startCallback()
            }
            isMeasuring = !isMeasuring
            return true
        }
        return super.onOptionsItemSelected(item)
    }


}

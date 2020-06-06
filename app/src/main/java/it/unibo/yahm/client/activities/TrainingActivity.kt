package it.unibo.yahm.client.activities

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.YAxis.AxisDependency
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.utils.ColorTemplate
import it.unibo.yahm.R
import it.unibo.yahm.client.sensors.ReactiveLocation
import it.unibo.yahm.client.sensors.ReactiveSensor
import it.unibo.yahm.client.sensors.SensorCombiners
import it.unibo.yahm.client.sensors.SensorType
import it.unibo.yahm.client.utils.CsvFile
import it.unibo.yahm.client.utils.FunctionUtils.median
import java.util.concurrent.TimeUnit


class TrainingActivity : AppCompatActivity() {

    private var isMeasuring = false
    private lateinit var startCallback: () -> Unit
    private lateinit var stopCallback: () -> Unit
    private lateinit var chart: LineChart;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training)
        setSupportActionBar(findViewById(R.id.my_toolbar))
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

        data.addEntry(Entry(xAccDataSet.entryCount.toFloat(), acc_x), 0);
        data.addEntry(Entry(yAccDataSet.entryCount.toFloat(), acc_y), 1);
        data.addEntry(Entry(zAccDataSet.entryCount.toFloat(), acc_z), 2);
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
        val set = LineDataSet(null, name);
        set.lineWidth = 2f;
        set.circleRadius = 2f;
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
                "x_acc", "y_acc", "z_acc",
                "x_ang_vel", "y_ang_vel", "z_ang_vel",
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
            Log.i(
                "TrainingActivity",
                "Saving to ${sensorValuesFile.fileName}, ${obstaclesFile.fileName}"
            )

            reactiveSensor.observer(SensorType.LINEAR_ACCELERATION)
                .sample(100, TimeUnit.MILLISECONDS).subscribe({
                addAccelerationEntry(it.values[0], it.values[1], it.values[2])
            }, { it.printStackTrace() })

            sensorObservers.combineByTime().subscribe({ cv ->
                sensorValuesFile.writeValue(
                    listOf(
                        cv.timestamp,
                        cv.accelerationValues.map { it.x }.median(),
                        cv.accelerationValues.map { it.y }.median(),
                        cv.accelerationValues.map { it.z }.median(),
                        cv.gyroscopeValues.map { it.x }.median(),
                        cv.gyroscopeValues.map { it.y }.median(),
                        cv.gyroscopeValues.map { it.z }.median(),
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

package com.example.graduateproject

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth

class ChartActivity: BaseActivity() {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var listView: ListView
    private lateinit var recordAdapter: RecordAdapter
    private var exerciseRecordList = mutableListOf<Record>()
    private var sanitizedPhoneNumber: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.chart)

        supportActionBar?.title = ""

        firestore = FirebaseFirestore.getInstance()

        listView = findViewById(R.id.listview)

        recordAdapter = RecordAdapter(this, exerciseRecordList)
        listView.adapter = recordAdapter

        fetchRecordFromDatabase()
        val actionBar = supportActionBar

        actionBar?.displayOptions = androidx.appcompat.app.ActionBar.DISPLAY_SHOW_CUSTOM
        actionBar?.setCustomView(R.layout.custom_actionbar_withhome)

        val bnt_backhome = actionBar?.customView?.findViewById<ImageButton>(R.id.bnt_backhome)
        val imageView = actionBar?.customView?.findViewById<ImageView>(R.id.customImageView)

        val btnChoose = findViewById<Button>(R.id.btn_choose)
        btnChoose.setOnClickListener {
            showDateSelectionDialog()
        }
        imageView?.setImageResource(R.drawable.logo)

        bnt_backhome?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }
    }
    private fun showDateSelectionDialog() {
        val allDates = exerciseRecordList.map { it.date }.distinct().sorted().toTypedArray()
        val checkedDates = BooleanArray(allDates.size)

        val builder = AlertDialog.Builder(this)
        builder.setTitle("選擇日期")
        builder.setMultiChoiceItems(allDates, checkedDates) { _, _, _ -> }
        builder.setPositiveButton("確定") { _, _ ->
            val selectedDates = allDates.filterIndexed { index, _ -> checkedDates[index] }
            displayChartForDates(selectedDates)
        }
        builder.setNegativeButton("取消", null)
        builder.show()
    }

    private fun displayChartForDates(dates: List<String>) {
        val sortedDates = dates.sorted()
        val recordsForDates = exerciseRecordList.filter { it.date in sortedDates }
        val groupedRecords = recordsForDates.groupBy { it.date }

        val entriesDistance = ArrayList<Entry>()
        val entriesTime = ArrayList<Entry>()

        var index = 0f
        for (date in sortedDates) {
            val recordsForDate = groupedRecords[date] ?: emptyList()
            val totalDistanceForDate = recordsForDate.sumByDouble { it.distance.toDouble() }
            val totalTimeForDate = recordsForDate.sumBy { it.time.toInt() }

            entriesDistance.add(Entry(index, totalDistanceForDate.toFloat()))
            entriesTime.add(Entry(index, totalTimeForDate.toFloat()))

            index++
        }

        val dataSetDistance = LineDataSet(entriesDistance, "運動距離")
        dataSetDistance.color = Color.BLUE
        dataSetDistance.axisDependency = YAxis.AxisDependency.LEFT

        val dataSetTime = LineDataSet(entriesTime, "運動時間")
        dataSetTime.color = Color.parseColor("#FFA500")
        dataSetTime.axisDependency = YAxis.AxisDependency.RIGHT

        val data = LineData(dataSetDistance, dataSetTime)
        val lineChart = findViewById<LineChart>(R.id.chart)
        lineChart.data = data
        lineChart.description.isEnabled = false
        lineChart.setScaleEnabled(true) // 啟用縮放
        lineChart.setPinchZoom(true) // 啟用兩指縮放
        lineChart.fitScreen() // 使圖表適應屏幕，確保所有的數據都在顯示範圍內

        // 設置Y軸
        val leftAxis = lineChart.axisLeft
        leftAxis.textColor = Color.BLUE
        leftAxis.axisMinimum = 0f
        leftAxis.valueFormatter = object : ValueFormatter() {
            override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                return "$value km"
            }
        }
        val rightAxis = lineChart.axisRight
        rightAxis.textColor = Color.parseColor("#FF5809") // 橘色的HEX代碼
        rightAxis.axisMinimum = 0f
        rightAxis.valueFormatter = object : ValueFormatter() {
            override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                return "$value min"
            }
        }
        // 設置X軸
        // 設置X軸
        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.TOP
        xAxis.labelRotationAngle = 45f
        xAxis.granularity = 1f
        xAxis.isGranularityEnabled = true
        xAxis.labelCount = sortedDates.size

        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                // 確保value是整數，因為它應該是資料點的index
                val index = value.toInt()

                // 若index在有效範圍內，則回傳對應日期，否則回傳空字串
                return if (index >= 0 && index < sortedDates.size) {
                    sortedDates[index]
                } else {
                    ""
                }
            }
        }

        // 顯示圖表
        lineChart.invalidate()
    }



    private fun fetchRecordFromDatabase() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "未登入的用戶", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val phoneNumber = currentUser.phoneNumber
        if (phoneNumber.isNullOrEmpty()) {
            Toast.makeText(this, "找不到手機號碼", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        sanitizedPhoneNumber = "0" + phoneNumber.replace("+886", "")

        firestore.collection("users").document(sanitizedPhoneNumber).collection("運動紀錄")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val record = Record(
                        date = document.data["日期"] as? String ?: "",
                        distance = document.data["運動距離(公里)"] as? String ?: "",
                        time = (document.data["運動時長(分鐘)"] as? Long)?.toString() ?: ""
                    )
                    exerciseRecordList.add(record)
                }

                exerciseRecordList.reverse()  // 反轉列表

                recordAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "錯誤: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onBackPressed() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        startActivity(intent)
        finish()
    }
}
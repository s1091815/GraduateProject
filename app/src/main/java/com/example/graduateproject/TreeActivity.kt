package com.example.graduateproject

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class TreeActivity: BaseActivity() {

    val rankImageMap = mapOf(
        1 to R.drawable.rank_1,
        2 to R.drawable.rank_2,
        3 to R.drawable.rank_3,
        4 to R.drawable.rank_4,
        5 to R.drawable.rank_5
    )
    lateinit var rankProgressBar: ProgressBar
    lateinit var currentScoreTextView: TextView
    lateinit var maxScoreTextView: TextView
    lateinit var currentTotalScoreTextView: TextView
    lateinit var level: TextView

    fun updateRankProgressBar(score: Int) {
        val (currentMin, currentMax, currentProgress) = when {
            score < 500 -> Triple(0, 500, score)
            score < 1500 -> Triple(500, 1500, score - 500)
            score < 3000 -> Triple(1500, 3000, score - 1500)
            score < 5000 -> Triple(3000, 5000, score - 3000)
            else -> Triple(5000, 6000, 1000) // 這邊只是一個示例，你可以根據需求調整
        }
        rankProgressBar.max = currentMax - currentMin
        rankProgressBar.progress = currentProgress

        currentTotalScoreTextView.text = "目前分數: $score"
        currentScoreTextView.text = currentMin.toString()
        maxScoreTextView.text = currentMax.toString()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tree)

        rankProgressBar = findViewById(R.id.rankProgressBar)

        supportActionBar?.title = ""

        val actionBar = supportActionBar

        actionBar?.displayOptions = androidx.appcompat.app.ActionBar.DISPLAY_SHOW_CUSTOM
        actionBar?.setCustomView(R.layout.custom_actionbar_withhome)

        val bnt_backhome = actionBar?.customView?.findViewById<ImageButton>(R.id.bnt_backhome)
        val bnt_sos = actionBar?.customView?.findViewById<ImageButton>(R.id.bnt_sos)
        val imageView = actionBar?.customView?.findViewById<ImageView>(R.id.customImageView)
        val btnRank: Button = findViewById(R.id.btn_rank)
        currentScoreTextView = findViewById(R.id.currentScoreTextView)
        maxScoreTextView = findViewById(R.id.maxScoreTextView)
        currentTotalScoreTextView = findViewById(R.id.currentTotalScoreTextView)
        level = findViewById(R.id.level)
        val btnHint: ImageButton = findViewById(R.id.btn_hint)
        btnHint.setOnClickListener {
            showRankingRulesDialog()
        }


        btnRank.setOnClickListener {
            showRankBottomSheet()
        }

        imageView?.setImageResource(R.drawable.logo)

        bnt_backhome?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }
        getCurrentRankFromFirebase { rank ->
            val treeImageView: ImageView = findViewById(R.id.rankImageView)
            if (rank != null) {
                treeImageView.setImageResource(rankImageMap[rank] ?: R.drawable.load)
            } else {
                treeImageView.setImageResource(R.drawable.load)
            }
        }
    }
    private fun showRankingRulesDialog() {
        val rankRules = """
        * 分數計算：
        運動距離：每公里得10分。
        運動時間：每分鐘得1分。
        EX: 運動距離5公里，30分鐘，
        得分：(5公里 x 10分)+(30分鐘 x 1分) = 80

        * 階級提升：
        階級1：0 - 499分 -> 種子。
        階級2：500 - 1499分 -> 發芽。
        階級3：1500 - 2999分 -> 樹苗。
        階級4：3000 - 4999分 -> 成長。
        階級5：5000分以上 -> 大樹。
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("電子樹分數階級規則")
            .setMessage(rankRules)
            .setPositiveButton("知道了", null)
            .show()
    }

    private fun getCurrentRankFromFirebase(callback: (Int?) -> Unit) {
        val firestore = FirebaseFirestore.getInstance() // 初始化FirebaseFirestore實例
        val rawPhoneNumber = FirebaseAuth.getInstance().currentUser?.phoneNumber // 獲取當前用戶的電話號碼

        if (rawPhoneNumber != null && rawPhoneNumber.startsWith("+886")) {
            val localPhoneNumber = rawPhoneNumber.replaceFirst("+886", "0") // 將+886替換為0

            // 使用修改後的電話號碼查找用戶的階級文檔
            firestore.collection("排行榜").document(localPhoneNumber).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        // 從文檔中獲取階級字段的值
                        val rank = document.getLong("階級")?.toInt()
                        // 更新level TextView的內容
                        level.text = "階級: $rank"
                        callback(rank) // 使用回調函數返回階級值
                        val score = document.getLong("分數")?.toInt() ?: 0
                        updateRankProgressBar(score)
                    } else {
                        // 如果文檔不存在，返回null
                        callback(null)
                        Toast.makeText(this, "找不到用戶的階級資料", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("FirebaseError", "Error fetching rank", exception)
                    callback(null) // 出現錯誤時返回null
                    Toast.makeText(this, "載入階級失敗: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            // 如果用戶未登錄或未找到電話號碼，或者電話號碼格式不是我們期望的，返回null
            callback(null)
        }
    }
    private fun showRankBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.rank_bottom_sheet, null)

        val listView: ListView = view.findViewById(R.id.rank_list)
        val rankList = ArrayList<RankItem>() // RankItem是之前定義的數據類
        val adapter = RankAdapter(this, rankList)
        listView.adapter = adapter

        // 從Firebase加載數據
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("排行榜").orderBy("分數", Query.Direction.DESCENDING).get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val name = document.getString("姓名")
                    val rank = document.getLong("階級")?.toInt()
                    val score = document.getLong("分數")?.toInt()

                    if (name != null && rank != null && score != null) {
                        rankList.add(RankItem(name, rank, score))
                    }
                }
                adapter.notifyDataSetChanged()
            }

        bottomSheetDialog.setContentView(view)
        val height = Resources.getSystem().displayMetrics.heightPixels
        bottomSheetDialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, height / 2)

        bottomSheetDialog.show()
    }

    override fun onBackPressed() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        startActivity(intent)
        finish()
    }
}
data class RankItem(val name: String, val rank: Int, val score: Int)

class RankAdapter(context: Context, val rankList: ArrayList<RankItem>) : BaseAdapter() {
    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getCount(): Int {
        return rankList.size
    }

    override fun getItem(position: Int): Any {
        return rankList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: inflater.inflate(R.layout.rank_item, parent, false)
        val rankItem = getItem(position) as RankItem

        val nameTextView: TextView = view.findViewById(R.id.name_text)
        val rankTextView: TextView = view.findViewById(R.id.rank_text)
        val scoreTextView: TextView = view.findViewById(R.id.score_text)

        nameTextView.text = rankItem.name
        rankTextView.text = rankItem.rank.toString()
        scoreTextView.text = rankItem.score.toString()

        return view
    }
}

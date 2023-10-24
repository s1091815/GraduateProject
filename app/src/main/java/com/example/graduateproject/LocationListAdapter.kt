package com.example.graduateproject

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.ImageButton
import android.app.AlertDialog
import android.content.Intent
import com.google.firebase.firestore.FirebaseFirestore

class LocationListAdapter(
    private val context: Context,
    private val locationList: MutableList<String>,
    private val firestore: FirebaseFirestore,
    private val phoneNumber: String
) : BaseAdapter() {

    override fun getCount(): Int = locationList.size

    override fun getItem(position: Int): Any = locationList[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = LayoutInflater.from(context).inflate(R.layout.list_item_location, parent, false)

        val locationNameTextView: TextView = view.findViewById(R.id.locationNameTextView)
        val goButton = view.findViewById<ImageButton>(R.id.btn_go)
        val editButton = view.findViewById<ImageButton>(R.id.btn_edit)
        val deleteButton = view.findViewById<ImageButton>(R.id.btn_delete)

        val locationName = locationList[position]
        locationNameTextView.text = locationName

        goButton.setOnClickListener {
            val docRef = firestore.collection("users").document(phoneNumber).collection("地點").document(locationName)

            docRef.get().addOnSuccessListener { documentSnapshot ->
                val geoPoint = documentSnapshot.getGeoPoint("位置")

                if (geoPoint != null) {
                    val builder = AlertDialog.Builder(context)
                    builder.setTitle("導航確認")
                    builder.setMessage("您確定要前往此地點嗎?")

                    builder.setPositiveButton("確定") { _, _ ->
                        //跳轉到MainActivity運作導航，並且傳遞所選地點的經緯度作為額外資訊
                        val intent = Intent(context, MainActivity::class.java)
                        intent.putExtra("LOCATION_LATITUDE", geoPoint.latitude)
                        intent.putExtra("LOCATION_LONGITUDE", geoPoint.longitude)
                        context.startActivity(intent)
                    }

                    builder.setNegativeButton("取消") { dialog, _ ->
                        dialog.dismiss()
                    }

                    builder.show()
                } else {
                    Toast.makeText(context, "找不到地點的經緯度", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { exception ->
                Toast.makeText(context, "獲取地點失敗: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }

        editButton.setOnClickListener {
            val builder = AlertDialog.Builder(context)
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val view = inflater.inflate(R.layout.dialog_edit_location, null)
            val editText = view.findViewById<EditText>(R.id.editTextNewLocationName)
            editText.setText(locationName)

            builder.setView(view)
                .setTitle("編輯地點名稱")
                .setPositiveButton("更新") { _, _ ->
                    val newLocationName = editText.text.toString()
                    if (newLocationName.isNotEmpty()) {
                        val oldDocRef = firestore.collection("users").document(phoneNumber).collection("地點").document(locationName)
                        val newDocRef = firestore.collection("users").document(phoneNumber).collection("地點").document(newLocationName)

                        oldDocRef.get().addOnSuccessListener { documentSnapshot ->
                            if (documentSnapshot.exists()) {
                                val oldData = documentSnapshot.data

                                oldData?.remove("名稱")
                                oldData?.put("名稱", newLocationName)

                                newDocRef.set(oldData!!)
                                    .addOnSuccessListener {
                                        oldDocRef.delete()
                                            .addOnSuccessListener {
                                                locationList[position] = newLocationName
                                                notifyDataSetChanged()
                                                Toast.makeText(context, "更新成功", Toast.LENGTH_SHORT).show()
                                            }
                                            .addOnFailureListener { exception ->
                                                Toast.makeText(context, "更新失敗: ${exception.message}", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                    .addOnFailureListener { exception ->
                                        Toast.makeText(context, "設置失敗: ${exception.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }
                    } else {
                        Toast.makeText(context, "請輸入您想修改的地點名稱", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("取消", null)
                .show()
        }

        deleteButton.setOnClickListener {
            firestore.collection("users").document(phoneNumber).collection("地點")
                .document(locationName)
                .delete()
                .addOnSuccessListener {
                    locationList.removeAt(position)
                    notifyDataSetChanged()
                    Toast.makeText(context, "地點已刪除", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(context, "刪除失敗: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }

        return view
    }
}

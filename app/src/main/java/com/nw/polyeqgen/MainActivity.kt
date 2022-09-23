package com.nw.polyeqgen

import android.animation.*
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.round

class MainActivity : AppCompatActivity() {

    private val colorX = "#EE0000"
    private val colorY = "#018786"
    private val colorP = "#0000EE"

    private lateinit var tvRes: TextView
    private lateinit var linCenter: LinearLayout
    private lateinit var adapter: DataAdapter

    private lateinit var sharedPref: SharedPreferences
    private var decPlace = 4 // by default
    private var printedResult = false
    private var topTitle = "<font color='$colorY'>Y</font> = Polynomial Equation of <font color='$colorX'><b>x</b></font>"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this)

        tvRes = findViewById(R.id.tv_res)
        linCenter = findViewById(R.id.layout_center)

        tvRes.text = makeHtml(topTitle)

        val listView : RecyclerView = findViewById(R.id.data_items_view)
        adapter = DataAdapter(mutableListOf())
        listView.adapter = adapter
        listView.layoutManager = LinearLayoutManager(this)

        init()

        findViewById<ImageButton>(R.id.bt_settings).setOnClickListener{
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<Button>(R.id.bt_add).setOnClickListener{
            inputDialog()
        }

        findViewById<ImageButton>(R.id.bt_center_add).setOnClickListener{
            inputDialog()
        }

        findViewById<Button>(R.id.bt_clear).setOnClickListener{
            adapter.clearAllData()
            init()
        }

        findViewById<Button>(R.id.bt_cal).setOnClickListener{
            val s: String = getEquation()
            if (s.isNotEmpty()) {
                animateResultText(s)
                printedResult = true
            }
        }

    }

    private fun init(animateCenter: Boolean = false){
        if (printedResult){
            animateResultText(topTitle)
            printedResult = false
        }

        if(linCenter.visibility == View.INVISIBLE){
            linCenter.alpha = 0f
            linCenter.visibility = View.VISIBLE

            ObjectAnimator.ofFloat(linCenter, "alpha", 1f).apply {
                duration = 200
                start()
            }
        }

        updateDecPlace()

        if (animateCenter){
            val pvhx1 = PropertyValuesHolder.ofFloat("scaleX", 1.3f)
            val pvhx2 = PropertyValuesHolder.ofFloat("scaleX", 1f)

            val pvhy1 = PropertyValuesHolder.ofFloat("scaleY", 1.3f)
            val pvhy2 = PropertyValuesHolder.ofFloat("scaleY", 1f)

            val animZoomIn = ObjectAnimator.ofPropertyValuesHolder(linCenter, pvhx1, pvhy1).apply { duration=150 }
            val animZoomOut = ObjectAnimator.ofPropertyValuesHolder(linCenter, pvhx2, pvhy2).apply { duration=150 }

            val animSet = AnimatorSet()
            animSet.playSequentially(animZoomIn, animZoomOut)
            animSet.start()
        }
    }

    private fun animateResultText(s: String){
        val animDuration = 100 // ms
        ObjectAnimator.ofFloat(tvRes, "alpha", 0f).apply {
            duration=animDuration.toLong()
            addListener(object : AnimatorListenerAdapter(){
                override fun onAnimationEnd(animation: Animator?) {
                    super.onAnimationEnd(animation)

                    tvRes.text = makeHtml(s)

                    ObjectAnimator.ofFloat(tvRes, "alpha", 1f).apply{
                        duration = animDuration.toLong()
                        start()
                    }
                }
            })
            start()
        }
    }

    fun inputDialog(pos: Int = -1){
        val mDialogView = LayoutInflater.from(this).inflate(R.layout.input_dialog, null)
        val title = if (pos == -1) "Enter" else "Modify"
        val mDialogBuilder = AlertDialog.Builder(this)
            .setView(mDialogView)
            .setTitle("$title x & y")

        val mAlertDialog = mDialogBuilder.show()

        val edX: EditText = mDialogView.findViewById(R.id.dialog_ed_x)
        val edY: EditText = mDialogView.findViewById(R.id.dialog_ed_y)

        if(pos != -1){
            var ss1 = adapter.data[pos].x.toString()
            var ss2 = adapter.data[pos].y.toString()
            if(ss1.endsWith(".0")) ss1 = ss1.substring(0, ss1.length-2)
            if(ss2.endsWith(".0")) ss2 = ss2.substring(0, ss2.length-2)
            edX.setText(ss1)
            edY.setText(ss2)
        }

        mDialogView.findViewById<Button>(R.id.dialog_bt_cancel).setOnClickListener{
            mAlertDialog.dismiss()
        }

        mDialogView.findViewById<Button>(R.id.dialog_bt_ok).setOnClickListener{
            val s1 = edX.text.toString()
            val s2 = edY.text.toString()

            if (s1.isEmpty()) edX.error = "Invalid"
            else if (s2.isEmpty()) edY.error = "Invalid"
            else{
                var x:Double = s1.toDouble()
                var y:Double = s2.toDouble()

                x = roundDec(x)
                y = roundDec(y)

                var flag = false
                for ((i, item) in adapter.data.withIndex()){
                    if (item.x == x && (pos == -1 || pos != i)){
                        flag = true
                        edX.error = "x = $x already exist"
                        break
                    }
                }
                if (!flag) {
                    mAlertDialog.dismiss()
                    if (pos == -1) {
                        if (linCenter.visibility == View.VISIBLE) {
                            linCenter.visibility = View.INVISIBLE
                        }
                        adapter.addData(DataItem(x, y))
                    }else{
                        adapter.modifyData(DataItem(x, y), pos)
                    }
                }
            }
        }
    }

    private fun solve (temp: MutableList<DataItem>, start: Int, end: Int) : Double{
        // actual solve mechanism
        return if(start == end)
            temp[start].y
        else
            (solve(temp, start, end-1)-solve(temp, start+1, end)) / (temp[start].x-temp[end].x)
    }

    private fun getEquation() : String{
        // solve mechanism
        val temp: MutableList<DataItem> = mutableListOf()
        val res = mutableListOf<Double>()

        val size = adapter.data.size
        if(size == 0){
            init(true)
            return ""
        }

        for(i in 0 until size){
            val x = adapter.data[i].x
            val y = adapter.data[i].y
            temp.add(DataItem(x, y))
        }

        for(i in 0 until size){
            res.add(solve(temp,0, size-i-1))
            temp.removeLast()
            for (j in 0 until temp.size){
                temp[j].y = temp[j].y - res[i] * pow(temp[j].x, size-i-1)
            }
        }

        var s = ""

        for (i in 0 until res.size){

            res[i] = roundDec(res[i])

            if (res[i] != 0.0){
                val p = res.size - i - 1
                var ss: String

                if (p == 0){
                    if (res[i] > 0) s += "+"
                    ss = res[i].toString()
                    s += if (ss.endsWith(".0")) ss.substring(0, ss.length-2)
                        else ss
                }else {
                    if (res[i] > 0)  s += "+"

                    if (res[i] == -1.0) {
                        s += "-"
                    }else if (res[i] != 1.0){
                        ss = res[i].toString()
                        s += if (ss.endsWith(".0")) ss.substring(0, ss.length-2)
                            else ss
                    }
                    s += "<font color='$colorX'><b>x</b></font>"
                    if (p != 1){
                        s += "<font color='$colorP'><sup><small>$p</small></sup></font>"
                    }
                }
            }
        }

        if (s.startsWith("+")){
            s = s.substring(1)
        }
        if (s.isEmpty()){
            s = "0"
        }

        return "<font color='$colorY'><b>Y</b></font> = $s"
    }

    private fun updateDecPlace(){
        val d = sharedPref.getString("dec_place", "4")?.toIntOrNull()
        if((d != null) && (d in 0..10)){
            decPlace = d
        }
    }

    override fun onResume() {
        super.onResume()
        updateDecPlace()
    }

    private fun makeHtml(s: String): Spanned{
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            Html.fromHtml(s, Html.FROM_HTML_MODE_COMPACT)
        else
            Html.fromHtml(s)
    }

    private fun pow(n: Double, i: Int) : Double{
        return when(i){
            0 -> 1.0
            1 -> n
            else -> n * pow(n, i-1)
        }
    }

    private fun roundDec(d: Double, n: Int=decPlace) : Double{
        val i = pow(10.0, n)
        return round(d*i) / i
    }
}



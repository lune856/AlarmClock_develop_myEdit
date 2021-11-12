package com.theglendales.alarm.presenter

import android.view.View
import android.widget.CompoundButton
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.theglendales.alarm.R
import com.theglendales.alarm.configuration.Layout
import com.theglendales.alarm.view.DigitalClock

/**
 * Created by Yuriy on 05.08.2017.
 */
class RowHolder(view: View, id: Int, val layout: Layout) {
    val digitalClock: DigitalClock
    val digitalClockContainer: View
    val rowView: View = view
    val onOff: CompoundButton
    val container: View
    val alarmId: Int = id
    //val daysOfWeek: TextView
    //val label: TextView
    val detailsButton: View
    val idHasChanged: Boolean
// 내가 추가->
    val albumArt: ImageView// Album Art 추가 (detailsButton 대체 <== '...' 요렇게 생긴 놈.)
    val swipeBtnDelete: ImageButton // Swipe 했을 때  Delete 하는 버튼
    val tvSun: TextView
    val tvMon: TextView
    val tvTue: TextView
    val tvWed: TextView
    val tvThu: TextView
    val tvFri: TextView
    val tvSat: TextView

    init {
        digitalClock = find(R.id.list_row_digital_clock) as DigitalClock
        digitalClockContainer = find(R.id.list_row_digital_clock_container)
        onOff = find(R.id.list_row_on_off_switch) as CompoundButton
        container = find(R.id.list_row_on_off_checkbox_container)
        //daysOfWeek = find(R.id.list_row_daysOfWeek) as TextView
         //label = find(R.id.list_row_label) as TextView
        detailsButton = find(R.id.details_button_container) // ' ... ' 이렇게 생긴 놈. -> 지금은 album art 로 대체되어 있음.
        val prev: RowHolder? = rowView.tag as RowHolder?
        idHasChanged = prev?.alarmId != id
        rowView.tag = this

    // 내가 추가->
        digitalClockContainer.tag = this
        albumArt = find(R.id.id_row_albumArt) as ImageView
        swipeBtnDelete = find(R.id.imgBtn_swipe_delete) as ImageButton

        tvSun = find(R.id._tvSun) as TextView
        tvMon = find(R.id._tvMon) as TextView
        tvTue = find(R.id._tvTue) as TextView
        tvWed = find(R.id._tvWed) as TextView
        tvThu = find(R.id._tvThu) as TextView
        tvFri = find(R.id._tvFri) as TextView
        tvSat = find(R.id._tvSat) as TextView
    // 내가 추가<-
        


        // 입력받는 id 를 활용해서 해당 알람이 설정해놓은 Album Art 이미지 찾기.
    }

    private fun find(id: Int): View = rowView.findViewById(id)
}
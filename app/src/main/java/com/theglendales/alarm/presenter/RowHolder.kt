package com.theglendales.alarm.presenter

import android.view.View
import android.widget.CompoundButton
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
    val daysOfWeek: TextView
    val label: TextView
    val detailsButton: View
    val idHasChanged: Boolean

    init {
        digitalClock = find(R.id.list_row_digital_clock) as DigitalClock
        digitalClockContainer = find(R.id.list_row_digital_clock_container)
        onOff = find(R.id.list_row_on_off_switch) as CompoundButton
        container = find(R.id.list_row_on_off_checkbox_container)
        daysOfWeek = find(R.id.list_row_daysOfWeek) as TextView
        label = find(R.id.list_row_label) as TextView
        detailsButton = find(R.id.details_button_container) // ' ... ' 이렇게 생긴 놈.
        val prev: RowHolder? = rowView.tag as RowHolder?
        idHasChanged = prev?.alarmId != id
        rowView.tag = this
    }

    private fun find(id: Int): View = rowView.findViewById(id)
}
package com.theglendales.alarm.model

import android.net.Uri
import android.util.Log
import com.theglendales.alarm.configuration.globalInject
import com.theglendales.alarm.jjmvvm.helper.MySharedPrefManager
import com.theglendales.alarm.jjmvvm.util.DiskSearcher
import java.lang.Exception

private const val TAG="Alarmtone.kt"
//private val defaultAlarmAlertUri = Settings.System.DEFAULT_ALARM_ALERT_URI.toString()
private val defaultAlarmAlertUri = getDefaultRtaPath()
val mySharedPrefManager: MySharedPrefManager by globalInject() // Shared Pref by Koin!!

fun Alarmtone.ringtoneManagerString(): Uri? {
    Log.d(TAG, "ringtoneManagerString: this=$this")
    return when (this) {
        is Alarmtone.Silent -> null
        is Alarmtone.Default -> Uri.parse(defaultAlarmAlertUri)
        is Alarmtone.Sound -> Uri.parse(this.uriString)
    }
}
// **최초 인스톨 후 생성되는 **신규 알람 2개
private fun getDefaultRtaPath(): String? { // 최초 인스톨 후 생성되는 **신규 알람 2개는** 무조건 defRta1 으로 지정할것! (둘 다 같음!!)
    Log.d(TAG, "getDefaultRta: called!!")
    //val randomNumber = (0..4).random() //

        try{
            var rtaPath = DiskSearcher.finalRtArtPathList[0].audioFilePath

            if(!rtaPath.isNullOrEmpty()) {
                return rtaPath
            } else { // 위에서 만약 실패했을 경우
                val listFromSharedPref = mySharedPrefManager.getRtaArtPathList()
                rtaPath = listFromSharedPref[0].audioFilePath
                return rtaPath
            }
        }catch (e: Exception) {
            Log.d(TAG, "getDefaultRta: error getting default rta path.. error=$e ")
            return null
        }
}

sealed class Alarmtone(open val persistedString: String?) {

    data class Silent(override val persistedString: String? = null) : Alarmtone(persistedString)
    data class Default(override val persistedString: String? = "") : Alarmtone(persistedString)
    data class Sound(val uriString: String) : Alarmtone(uriString)

    companion object {
        fun fromString(string: String?): Alarmtone {

            when (string) {
                null -> {
                    Log.d(TAG, "fromString: null")
                    return Silent()}
                "" -> {Log.d(TAG, "fromString: 최초 알람 생성 -> defaultAlarmAlertUri 전달 예정!") //
                    if(!defaultAlarmAlertUri.isNullOrEmpty()) {

                        return Sound(defaultAlarmAlertUri)
                        } else {
                        return Default() // <- todo: 이거 괜찮을지..
                        }
                    }
                defaultAlarmAlertUri -> {
                    Log.d(TAG, "fromString: defaultAlarmAlertUri. string=$string")
                    return Sound(defaultAlarmAlertUri)}
                else -> { // user 가 지정해놓은 rta 일 경우 string 이 이상한 경로겠지.. ex. string=/storage/emulated/0/Android/data/com.theglendales.alarm.debug/files/.AlarmRingTones/defrt5.rta
                    Log.d(TAG, "fromString: else.. string=$string")
                    return Sound(string)}
            }
        }
    }
}
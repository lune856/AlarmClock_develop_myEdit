package com.theglendales.alarm.jjmvvm

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theglendales.alarm.configuration.globalInject
import com.theglendales.alarm.jjdata.RtInTheCloud
import com.theglendales.alarm.jjfirebaserepo.FirebaseRepoClass
import com.theglendales.alarm.jjmvvm.iapAndDnldManager.MyIAPHelperV3
import com.theglendales.alarm.jjmvvm.util.ToastMessenger
import kotlinx.coroutines.launch

/**
 * 쫑 한말씀: This ViewModel should follow Activity(AlarmsListActivity)'s life cycle.
 */
private const val TAG="JjMainViewModel"

class JjMainViewModel : ViewModel() {
//ToastMessenger
    private val toastMessenger: ToastMessenger by globalInject()
//IAP variable
    private val iapV3: MyIAPHelperV3 by globalInject()
//FireBase variables
    var isFreshList = false
    private val firebaseRepoInstance: FirebaseRepoClass by globalInject()
    private val _rtInTheCloudList = MutableLiveData<MutableList<RtInTheCloud>>() // Private& Mutable LiveData
    val rtInTheCloudList: LiveData<MutableList<RtInTheCloud>> = _rtInTheCloudList // Public but! Immutable (즉 이놈은 언제나= _liveRtList)

    init {
        Log.d(TAG, "init: called.. ^^ ")
        refreshAndUpdateLiveData()}
    //ViewModel 최초 로딩시 & Spinner 로 휘리릭~ 새로고침 할 때 아래 function 이 불림.
    fun refreshAndUpdateLiveData() {
        firebaseRepoInstance.getPostList().addOnCompleteListener {
            if(it.isSuccessful)
            {
                viewModelScope.launch {
                //** Coroutine 안에서는 순차적(Sequential) 으로 모두 진행됨. Async 걱정 안해도 될듯..?
                    //1) Fb 에서 RtList를 받아옴
                    val rtList = it.result!!.toObjects(RtInTheCloud::class.java)
                    //2) IAP 에서 Price, PurchaseBool 을 채워준(+) rtList 를 받아옴.
                    val rtListPlusIAPInfo = iapV3.iapManager(rtList)

                    //3) LiveData Update -> SecondFrag 에서는 a)Lottie OFF b)RefreshRcV! --- todo: rcv 에서 iap 에 있는 map 의존 없애기.
                    Log.d(TAG, "refreshAndUpdateLiveData: rtListPlusIAPInfo[0].itemPrice=${rtListPlusIAPInfo[0].itemPrice} //purchaseBool= ${rtListPlusIAPInfo[0].purchaseBool}")
                    //_rtInTheCloudList.value = rtListPlusIAPInfo

                    isFreshList = true //todo: 지우기
                    Log.d(TAG, "getRtList: <<<<<<<<<getRtList: successful")
                }


            }else { // 문제는 인터넷이 없어도 이쪽으로 오지 않음. always 위에 if(it.isSuccess) 로 감.
                Log.d(TAG, "<<<<<<<getRtList: ERROR!! Exception message: ${it.exception!!.message}")
                //lottieAnimController(1) // this is useless at the moment..
                toastMessenger.showMyToast("Unable to fetch Data. Please check your connection.", isShort = false)
            }
        }

    }
//*******************Network Detector -> LottieAnim 까지 연결
    var prevNT = true
    private val _isNetworkWorking = MutableLiveData<Boolean>() // Private& Mutable LiveData
    val isNetworkWorking: LiveData<Boolean> = _isNetworkWorking // Public but! Immutable (즉 이놈은 언제나= _liveRtList)

    fun updateNTWKStatus(isNetworkOK: Boolean) {
        _isNetworkWorking.postValue(isNetworkOK) // .postValue= backgroundThread 사용. // (이 job 은 발생지가 backgrouond thread 니깐 .value=xx 안되고 postValue() 써야함!)
    }

//***********************
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "onCleared: called..")
    }
}
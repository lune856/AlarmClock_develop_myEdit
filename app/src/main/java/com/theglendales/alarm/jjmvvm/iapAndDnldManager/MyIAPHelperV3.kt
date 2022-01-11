package com.theglendales.alarm.jjmvvm.iapAndDnldManager

import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.theglendales.alarm.configuration.globalInject
import com.theglendales.alarm.jjdata.RtInTheCloud
import com.theglendales.alarm.jjmvvm.util.ToastMessenger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlin.Exception
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private const val TAG="MyIAPHelperV3"

class MyIAPHelperV3(val context: Context ) : PurchasesUpdatedListener {

    private val toastMessenger: ToastMessenger by globalInject() // ToastMessenger

    var rtListPlusIAPInfo= mutableListOf<RtInTheCloud>()
    private var billingClient: BillingClient? = null

    init {
        Log.d(TAG, "init MyIapHelperV3 called. ")
        billingClient = BillingClient.newBuilder(context).enablePendingPurchases().setListener(this).build() //todo: 밑에 C .isReady 는 항상 false 네..
    }
// ****************** <1> 최초 SecondFrag 로딩 후 (과거 정보) 복원 관련
    //todo: Try & Catch
    fun iap_A_initBillingClient() {
        Log.d(TAG, "iap_A_initBillingClient: <A> Called")
        billingClient = BillingClient.newBuilder(context).enablePendingPurchases().setListener(this).build()
        Log.d(TAG, "iap_A_initBillingClient: <A> Finished")
    }

    fun iap_B_feedRtList(rtListFromFb: MutableList<RtInTheCloud>) {
        Log.d(TAG, "iap_B_feedRtList: <B> Called")
        rtListPlusIAPInfo = rtListFromFb}

    suspend fun iap_C_prepBillingClient(): BillingResult {
        Log.d(TAG, "iap_C_prepBillingClient: <C> Called")
        if(!billingClient!!.isReady) {
            Log.d(TAG, "iap_C_prepBillingClient: <C> BillingClient Not Ready(X)! Re init!")
            billingClient = BillingClient.newBuilder(context).enablePendingPurchases().setListener(this).build()
        }
        return suspendCoroutine { continuation ->
            Log.d(TAG, "iap_C_prepBillingClient: <C> BillingClient Ready(O)")
            billingClient!!.startConnection(object : BillingClientStateListener{
                override fun onBillingSetupFinished(p0: BillingResult) {
                    continuation.resume(p0)
                }
                override fun onBillingServiceDisconnected() {
                    continuation.resumeWithException(Exception("Error <C> Billing Service Disconnectoed"))
                    //todo:  Try to restart the connection on the next request to
                    // Google Play by calling the startConnection() method.
                }
            })
        }
    }
    //<D1> 현재 리스트에 상품별 구매 여부 (true,false) 적어주기.
    suspend fun iap_D1_addPurchaseBoolToList() {
        Log.d(TAG, "iap_D1_addPurchaseBoolToList: <D1> called")
        //throw Exception("D1 Error") // 에러 테스팅
        delay(2000L) // 2.0 초 걸린다고 치고.
        rtListPlusIAPInfo[0].itemPrice="$2,000"
        //Log.d(TAG, "iap_D1_addPurchaseBoolToList: <D1> Finished")
    }
    //<D2> 현재 리스트에 상품별 가격 적어주기 (ex. $1,000)
    suspend fun iap_D2_addPriceToList() {
        Log.d(TAG, "iap_D2_addPriceToList: <D2> called")
        val itemNameList = ArrayList<String>()
        rtListPlusIAPInfo.forEach {rtObject -> itemNameList.add(rtObject.iapName)}

        val myParams = SkuDetailsParams.newBuilder()
        myParams.setSkusList(itemNameList).setType(BillingClient.SkuType.INAPP)
        //**Very Nice: 아래 querySkuDetailsAsync 는 Async 콜이기 때문에 -> billingClient.query...Async{} 블락 아래 Logd 로 점프함 -> 그렇지만!
        //이 전체 콜백을 하나의 thread 로 봐주기에 -> 듯 Query 가 .Async 콜임에도 (JJMainViewMode>viewModelScope 에서 벗어나지 않았음!)
        billingClient!!.querySkuDetailsAsync(myParams.build()) {queryResult, skuDetailsList ->
            if(queryResult.responseCode == BillingClient.BillingResponseCode.OK && skuDetailsList!=null) {
                for(skuDetails in skuDetailsList) {
                    //todo: something better than .single?
                    rtListPlusIAPInfo.single { rtObj -> rtObj.iapName == skuDetails.sku }.itemPrice = skuDetails.price
                    Log.d(TAG,"iap_D2_addPriceToList : <D2> a) item title=${skuDetails.title} b)item price= ${skuDetails.price}, c)item sku= ${skuDetails.sku}")
                }
                Log.d(TAG, "iap_D2_addPriceToList: <D2> Finished (O)")
            } else {
                Log.d(TAG, "iap_D2_addPriceToList: <D2> Finished(X) - Error! XXX loading price for items")
                throw Exception("<D2> Error ")
            }
            //todo: 다운로드 관련 여기서..?

        }
        //delay(4000L) // Delay Test .. +4.0 초 걸린다고 치고.
    }
    //<E> 완성 리스트를 전달 -> 라이브데이터 -> SecondFrag -> rcVUpdate
    fun iap_E_getFinalList(): MutableList<RtInTheCloud> {
        Log.d(TAG, "iap_E_getFinalList: <E> called")
        return rtListPlusIAPInfo
    }


// ****************** <2> 현재 구매 관련

//**
    override fun onPurchasesUpdated(p0: BillingResult, p1: MutableList<Purchase>?) {
        //TODO("Not yet implemented")
    }

}
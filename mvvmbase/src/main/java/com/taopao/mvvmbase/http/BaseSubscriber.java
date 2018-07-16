package com.taopao.mvvmbase.http;

import android.content.Context;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.widget.Toast;

import com.google.gson.JsonParseException;
import com.taopao.mvvmbase.R;
import com.taopao.mvvmbase.base.EventData;
import com.taopao.mvvmbase.base.ViewState;
import com.taopao.mvvmbase.utils.NetworkUtil;

import org.json.JSONException;

import java.text.ParseException;
import java.util.jar.Attributes;

import io.reactivex.observers.DisposableObserver;

/**
 * @Author： 淘跑
 * @Date: 2018/7/5 11:43
 * @Use：该类仅供参考，实际业务Code, 根据需求来定义，
 */
public abstract class BaseSubscriber<T> extends DisposableObserver<T> {
    private Context mContext;
    /**
     * 界面的显示状态
     */
    public ObservableInt mViewState = new ObservableInt(ViewState.Normal_view);
    /**
     * dialog的显示或隐藏
     */
    public ObservableBoolean showLoadingDialog = new ObservableBoolean(false);
    /**
     * dialog的显示或隐藏
     */
    public ObservableField<EventData> showCodeOrMessage = new ObservableField();

    /**
     * 是否加载中的dialog
     */
    private boolean isShowDialog = false;

    /**
     * 是否显示错误布局
     */
    private boolean isShowError = false;
    /**
     * //是否显示网络错误布局
     */
    private boolean isShowNetError = false;


    /**
     * @param context           上下文
     * @param viewState         界面的显示状态
     * @param showCodeOrMessage 服务器返回的错误
     */
    public BaseSubscriber(Context context, ObservableInt viewState, ObservableField<EventData> showCodeOrMessage) {
        mContext = context;
        mViewState = viewState;
        this.showCodeOrMessage = showCodeOrMessage;

        isShowDialog = false;

    }

    @Override
    public void onError(Throwable e) {
        //通用处理
        // 隐藏正在显示的dialog
        showLoadingDialog.set(false);

        //TODO 关闭刷新动画(考虑当中)
        //TODO 显示加载错误的界面(考虑当中)

        //TODO json解析异常
        if (e instanceof JSONException
                || e instanceof JsonParseException
                || e instanceof ParseException) {

            Toast.makeText(mContext, "Json解析异常", Toast.LENGTH_SHORT).show();
            //TODO 显示加载错误的界面
        }

        if (isShowError) {
            mViewState.set(ViewState.Error_view);
        }

        // TODO: 处理其它通用错误
        // 覆写此方法自行处理异常，通用异常使用super.onError(e)保留
        e.printStackTrace();
    }


    @Override
    public void onStart() {
        super.onStart();
        if (isShowDialog) {
            showLoadingDialog.set(false);
        }

        if (!NetworkUtil.isNetworkAvailable(mContext)) {
            Toast.makeText(mContext, "无网络，请检查网络设置", Toast.LENGTH_SHORT).show();
            if (isShowNetError) {
                mViewState.set(ViewState.NoNetwork_view);
            }
            onComplete();
        }
    }

    @Override
    public void onNext(T t) {
        if (t instanceof BaseResponse) {
            BaseResponse baseResponse = (BaseResponse) t;
            // 判断是否请求错误，出错直接转到onError()
            if (!baseResponse.isOk()) {
//                Throwable e = new Throwable(response.getErrorMsg());
//                this.onError(e);
                //普通错误只需显示Toast即可
                switch (baseResponse.getErrorCode()) {
                    case 400://Bad Request 请求出现语法错误, 一般是请求参数不对
                    case 404://Not Found 无法找到指定位置的资源
                    case 403://Forbidden 资源不可用
                    case 500://服务器内部错误, 请联系Java后台开发人员 !!!
                    case 401://Unauthorized 访问被拒绝
                        break;
                    case 10000://服务器忙
                        break;
                    case 10006://验证码错误
                        break;
                    case 102://登录失效
                        showCodeOrMessage.set(new EventData(baseResponse.getErrorCode(), baseResponse.getErrorMsg(), 1));
                        break;
                    default:
                        //返回错误码以及错误信息  想处理错误信息的话就自己重写此方法
                        showCodeOrMessage.set(new EventData(baseResponse.getErrorCode(), baseResponse.getErrorMsg(), 0));
                }
                //关于登录方面的错误
//                if(mView!=null) {
//                    mView.showLoginDialog(response.getErrorMsg());
//                }
                return;
            }
        }
        //显示加载成功的界面
        mViewState.set(ViewState.Normal_view);
        onResult(t);
    }

    public abstract void onResult(T t);


    @Override
    public void onComplete() {
        // 隐藏正在显示的dialog
        showLoadingDialog.set(false);
        //TODO 关闭刷新动画(考虑当中)
    }

}

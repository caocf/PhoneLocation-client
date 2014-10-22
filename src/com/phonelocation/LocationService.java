package com.phonelocation;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.phonelocation.utils.JSONUtil;
import com.phonelocation.utils.MD5;
import com.phonelocation.utils.PhoneStateUtil;
import com.phonelocation.utils.PropertiesUtil;
import com.phonelocation.utils.SendPostRequestUtil;

public class LocationService extends Service {

	private static final int UPDATE_TIME = 30000;

	private BDLocation tmpLocation;
	private boolean iswork = false;
	private int location_count = 0;

	private String serverURL = "";
	private String phoneID;
	private String phoneIDMD5;
	private String tokenid = "";

	private LocationClient locationClient;

	private LocationServiceBinder binder = new LocationService.LocationServiceBinder();

	public class LocationServiceBinder extends Binder {
		LocationService getService() {
			return LocationService.this;
		}
	}

	public BDLocation getLocation() {
		return this.tmpLocation;
	}

	public boolean start() {
		tokenid = PropertiesUtil.getTokenId(this);
		if (locationClient.isStarted())
			return false;
		locationClient.start();
		locationClient.requestLocation();
		iswork = true;
		return true;
	}

	public boolean stop() {
		if (!locationClient.isStarted())
			return false;
		locationClient.stop();
		iswork = false;
		return true;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return binder;
	}

	Runnable sendLocationRunnable = new Runnable() {
		@Override
		public void run() {
			if (!iswork)
				return;
			if (serverURL == "") {
				serverURL = PropertiesUtil.getProperties(LocationService.this)
						.getProperty("serverUrl");
				Log.i("mytag", serverURL);
			}
			int status = SendPostRequestUtil.sendJSONRequest(
					JSONUtil.makeJSON(tmpLocation, phoneIDMD5), serverURL,
					tokenid);
			if (status != 200) {
				stop();
				Intent mIntent = new Intent(MainActivity.ACTION);
				mIntent.putExtra(MainActivity.MESSAGE,
						MainActivity.MESSAGE_NOAUTH);
				sendBroadcast(mIntent);
			}
		}
	};

	@Override
	public void onCreate() {
		phoneID = PhoneStateUtil.getPhoneID(this);
		phoneIDMD5 = MD5.string2MD5(phoneID);
		Log.i("mytag", phoneID);
		Log.i("mytag", phoneIDMD5);

		iswork = false;
		locationClient = new LocationClient(this);
		// ���ö�λ����
		LocationClientOption option = new LocationClientOption();
		option.setOpenGps(true); // �Ƿ��GPS
		option.setCoorType("bd09ll"); // ���÷���ֵ���������͡�
		option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy); // ���ö�λģʽ
		option.setProdName("LocationDemo"); // ���ò�Ʒ�����ơ�ǿ�ҽ�����ʹ���Զ���Ĳ�Ʒ�����ƣ����������Ժ�Ϊ���ṩ����Ч׼ȷ�Ķ�λ����
		option.setScanSpan(UPDATE_TIME); // ���ö�ʱ��λ��ʱ��������λ����
		locationClient.setLocOption(option);
		locationClient.registerLocationListener(new BDLocationListener() {

			@Override
			public void onReceiveLocation(BDLocation location) {
				if (location == null) {
					return;
				}
				tmpLocation = location;
				location_count++;
				new Thread(sendLocationRunnable).start();
				Intent mIntent = new Intent(MainActivity.ACTION);
				mIntent.putExtra(MainActivity.MESSAGE,
						MainActivity.MESSAGE_NEWLOCATION);
				sendBroadcast(mIntent);
			}
		});
		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}
}

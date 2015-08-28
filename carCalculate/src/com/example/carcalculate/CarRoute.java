package com.example.carcalculate;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import cn.comjoin.app.util.Taxijifei;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.location.LocationManagerProxy;
import com.amap.api.location.LocationProviderProxy;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;

/**
 * 混合定位示例
 * */
public class CarRoute extends Activity implements LocationSource,
		AMapLocationListener {

	// 每秒的距离，现假设是100km/h
	private static final int INTERVAL_DISTANCE = 28;
	private static final double DELAY_DISTANCE = 3.4;
	private Polyline polyline;
	private AMap aMap;
	private MapView mapView;
	private OnLocationChangedListener mListener;
	private LocationManagerProxy mAMapLocationManager;
	// 记录每个定位点信息的链表
	private List<LatLng> list = new ArrayList<LatLng>();
	private List<Double> timeList = new ArrayList<Double>();
	private List<Double> speedList = new ArrayList<Double>();
	private List<LatLng> beforeList = new ArrayList<LatLng>();
	// 按钮
	private Button end;
	private TextView jifei;
	// 是否第一次定位
	private boolean isFirstLocate = true;
	LatLng lastLatLng;

	/* 是否上车 */
	private boolean isGetOn = false;
	/* 是否第一次用 */
	private boolean isNext = true;
	/* 点击后退键的时间 */
	private long clickTime = 0;
	/*判断是否上车 还是下车*/
	private int count = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);// 不显示程序的标题栏
		setContentView(R.layout.activity_map);
		mapView = (MapView) findViewById(R.id.map);

		mapView.onCreate(savedInstanceState);// 此方法必须重写
		init();
		end = (Button) findViewById(R.id.end);
		jifei = (TextView) findViewById(R.id.jifei);

		/*
		 * 下车 计算结果
		 */
		end.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				/* deactivate(); */
				
				if(count == 0){
					init();
					// 两秒定位一次
					if (!isNext)
						mAMapLocationManager.requestLocationData(
								LocationProviderProxy.AMapNetwork, 2 * 1000, 2,
								CarRoute.this);
	
					isGetOn = true;
					if (polyline != null) {
						polyline.remove();
						Log.i("remove", "remove");
					}
	
					jifei.setText("开始计算价格中...");
					end.setText("下车");
					count = 1;
				}else{
					/*
					 * 下车
					 */
					// 总路程
					if (timeList.size() != 0 && list.size() != 0
							&& speedList.size() != 0) {
						double total = 0;
						double total_jifei = 0;
						double total_delay_time_jifei = 0;
						int delay_time_count = 0;
						double total_delay = 0;
						// 打印结果
						String all_distances = "before:\r\n";
						String right_distance = "after:\r\n";
						String unGetOn = "没上车:\r\n";
						// 前一次正确的信息
						double first = timeList.get(0);
						LatLng firstPosition = list.get(0);
						double first_speed = speedList.get(0);
						// 前一次是否加速度是否有浮动
						boolean isRight = true;
						// 前一次判断是否是“正确”的结果
						boolean isResult = true;

						int count = 0;
						// 未上车的信息
						for (int i = 1; i < beforeList.size(); i++) {
							double distance = AMapUtils.calculateLineDistance(
									beforeList.get(i), beforeList.get(i - 1));
							unGetOn += distance + "\r\n";
						}
						// 计算距离
						for (int i = 1; i < list.size(); i++) {
							double distance = AMapUtils.calculateLineDistance(
									list.get(i), firstPosition);
							double interval = timeList.get(i) - first;

							double distance_log = AMapUtils.calculateLineDistance(
									list.get(i), list.get(i - 1));
							double interval_log = timeList.get(i)
									- timeList.get(i - 1);
							all_distances += distance_log + "     "
									+ speedList.get(i) + "     " + interval_log
									+ "\r\n";

							// 两次判断，半径&加速度判断
							if (isProperDistance(distance, interval / 1000.0)) {

								if (isRight && count < 3) {
									double vMax = getMax(speedList.get(i),
											first_speed);
									// 20/3.6=5.6
									if (vMax / 3.6 + 5.6 >= distance * 1000.0
											/ interval) {
										total += distance;
										right_distance += distance_log + "  ";
										isRight = true;
										isResult = true;
									} else {
										isResult = false;
										isRight = false;
									}

								} else {
									total += distance;
									right_distance += distance_log + "  ";
									isRight = true;
									isResult = true;
								}
								first = timeList.get(i);
								firstPosition = list.get(i);
								first_speed = speedList.get(i);
								count = 0;
							} else {
								isRight = true;
								isResult = false;
								count++;
							}
							if (isResult) {
								if (isDelay(distance, interval / 1000.0)) {
									delay_time_count++;
									total_delay += interval / 1000.0;
								}
								// 计费部分调用
								Taxijifei taxi = new Taxijifei(CarRoute.this,
										"HeFei.txt", total, distance);
								double jifei_temp = taxi.jifei();
								total_jifei += jifei_temp;
								Taxijifei dtj = new Taxijifei(CarRoute.this,
										"HeFei.txt", interval / 1000.0, total_delay);
								total_delay_time_jifei += dtj.jifei();
								right_distance += jifei_temp + "  " + total_delay
										+ "  " + total_delay_time_jifei + "\r\n";
							}
						}
						right_distance += total + "\r\n";
						Date now = new Date();
						SimpleDateFormat dateFormat = new SimpleDateFormat(
								"yyyy-MM-dd.HH:mm:ss");
						String now_ = dateFormat.format(now);

						writeFileToSD(now_ + ".txt", unGetOn + all_distances
								+ right_distance);
						// double delay_time_jifei = (delay_time_count * 3 - 300) /
						// 60 *
						// 0.4;
						Taxijifei inital_jifei = new Taxijifei(CarRoute.this,
								"HeFei.txt");
						//费用去除小数，显示米  (int)Math.floor(total)
						jifei.setText("总距离:"+(int)Math.floor(total)+"米\n费用是"
								+ String.valueOf(inital_jifei.Getfujiafei()
										+ inital_jifei.Getqibujia() + total_jifei
										+ total_delay_time_jifei));
						Log.i("JIFEI",
								total_jifei + " " + inital_jifei.Getqibujia());
						

					} else {
						jifei.setText("没有费用");
					}
					list.clear();
					timeList.clear();
					if (polyline != null) {
						polyline.remove();
						Log.i("remove", "remove");
					}
					isFirstLocate = true;
					isGetOn = false;
					beforeList.clear();
					deactivate();
					isNext = false;
					
					count = 0;
					end.setText("上车");
					/*
					 * 下车
					 */
				}
				
				
			}
		});

	}

	/**
	 * 初始化
	 */
	private void init() {
		if (aMap == null) {
			aMap = mapView.getMap();
			setUpMap();
			Log.i("init", "init");
		}

	}

	/**
	 * 设置一些amap的属性
	 */
	private void setUpMap() {
		aMap.setLocationSource(this);// 设置定位监听
		aMap.getUiSettings().setMyLocationButtonEnabled(false);// 设置默认定位按钮是否显示
		aMap.setMyLocationEnabled(true);// 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
		// 设置定位的类型为定位模式 ，可以由定位、跟随或地图根据面向方向旋转几种,跟随模式
		aMap.setMyLocationType(AMap.LOCATION_TYPE_MAP_FOLLOW);
		/* 设置缩放级别 */
		aMap.moveCamera(CameraUpdateFactory.zoomTo(15));
		aMap.getUiSettings().setZoomControlsEnabled(false);
		Log.i("setup", "setup");
	}

	protected void onResume() {
		super.onResume();
		mapView.onResume();
	}

	/**
	 * 方法必须重写
	 */

	@Override
	protected void onPause() {
		super.onPause();
		mapView.onPause();

	}

	/**
	 * 方法必须重写
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mapView.onSaveInstanceState(outState);
	}

	/**
	 * 方法必须重写
	 */

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mapView.onDestroy();
		deactivate();
	}

	/**
	 * 此方法已经废弃
	 */
	@Override
	public void onLocationChanged(Location location) {
	}

	@Override
	public void onProviderDisabled(String provider) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	/**
	 * 定位成功后回调函数
	 */
	@Override
	public void onLocationChanged(AMapLocation amapLocation) {
		if (mListener != null && amapLocation != null) {
			if (amapLocation.getAMapException().getErrorCode() == 0) {

				double lan = amapLocation.getLatitude();
				double lon = amapLocation.getLongitude();

				LatLng p1 = new LatLng(lan, lon);
				mListener.onLocationChanged(amapLocation);// 显示系统小蓝点

				// String address = amapLocation.getAddress();
//				Time t = new Time();
//				t.setToNow();
//				current_timeList.add(t.hour);
				Calendar calendar = Calendar.getInstance();
				double lastTime = calendar.getTimeInMillis();
				// 如果定位准确
				if (amapLocation.hasAccuracy()) {
					if (isGetOn) {

						if (isFirstLocate) {
							isFirstLocate = false;
						} else {
							polyline = aMap.addPolyline((new PolylineOptions()
									.add(lastLatLng, p1).color(Color.RED)
									.width(10)));
						}
						lastLatLng = p1;
						list.add(p1);
						timeList.add(lastTime);
						speedList.add(amapLocation.getSpeed() * 3.6);
					} else {
						beforeList.add(p1);
					}
				}

				Log.i("position", lan + ";" + lon);

			} else {
				Log.e("AmapErr", "Location ERR:"
						+ amapLocation.getAMapException().getErrorCode());
				Toast.makeText(CarRoute.this, "网路出问题咯", 1000).show();
			}
		}
		Log.i("change", "change");
	}

	/* 判断间隔是否正确 */
	public boolean isProperDistance(double x, double interval) {
		if (x <= INTERVAL_DISTANCE * interval)
			return true;
		else
			return false;
	}

	/* 判断是否堵车 */

	public boolean isDelay(double x, double interval) {
		if (x <= DELAY_DISTANCE * interval)
			return true;
		else
			return false;
	}

	/**
	 * 激活定位
	 */
	@Override
	public void activate(OnLocationChangedListener listener) {
		mListener = listener;
		if (mAMapLocationManager == null) {
			mAMapLocationManager = LocationManagerProxy.getInstance(this);
			// 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
			// 注意设置合适的定位时间的间隔（最小间隔支持为2000ms），并且在合适时间调用removeUpdates()方法来取消定位请求
			// 在定位结束后，在合适的生命周期调用destroy()方法
			// 其中如果间隔时间为-1，则定位只定一次
			// 在单次定位情况下，定位无论成功与否，都无需调用removeUpdates()方法移除请求，定位sdk内部会移除

			/* 确保首次定位准确，连续定位5次 */
			// 上车就定位
			// 参数分别为 定位方式  定位时间间隔 2s 位置变化通知
			mAMapLocationManager.requestLocationData(
					LocationProviderProxy.AMapNetwork, 2 * 1000, 2, this);

		}
		Log.i("activate", "activate");
	}

	/**
	 * 停止定位
	 */
	@Override
	public void deactivate() {
		/* mListener = null; */
		if (mAMapLocationManager != null) {
			mAMapLocationManager.removeUpdates(this);
			/* mAMapLocationManager.destroy(); */
		}
		/* mAMapLocationManager = null; */
	}

	/**
	 * 写入文件
	 * 
	 */
	private void writeFileToSD(String filename, String content) {
		String sdStatus = Environment.getExternalStorageState();
		if (!sdStatus.equals(Environment.MEDIA_MOUNTED)) {
			Log.d("TestFile", "SD card is not avaiable/writeable right now.");
			return;
		}
		try {
			String pathName = "/sdcard/test/";
			File file = new File(pathName + filename);
		
			if (!file.exists()) {
				Log.d("TestFile", "Create the file:" + filename);
				file.createNewFile();
			}
			FileOutputStream stream = new FileOutputStream(file);
			byte[] buf = content.getBytes();
			stream.write(buf);
			stream.close();

		} catch (Exception e) {
			Log.e("TestFile", "Error on writeFilToSD.");
			e.printStackTrace();
		}
	}

	// 获得较大的瞬时速度
	private double getMax(double v1, double v2) {
		if (v1 < v2)
			return v2;
		else
			return v1;
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			exit();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private void exit() {
		if ((System.currentTimeMillis() - clickTime) > 2000) {
			Toast.makeText(getApplicationContext(), "再按一次后退键退出程序",
					Toast.LENGTH_SHORT).show();
			clickTime = System.currentTimeMillis();
		} else {
			this.finish();
			// System.exit(0);
		}
	}
}

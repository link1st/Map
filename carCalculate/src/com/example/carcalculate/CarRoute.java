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
 * ��϶�λʾ��
 * */
public class CarRoute extends Activity implements LocationSource,
		AMapLocationListener {

	// ÿ��ľ��룬�ּ�����100km/h
	private static final int INTERVAL_DISTANCE = 28;
	private static final double DELAY_DISTANCE = 3.4;
	private Polyline polyline;
	private AMap aMap;
	private MapView mapView;
	private OnLocationChangedListener mListener;
	private LocationManagerProxy mAMapLocationManager;
	// ��¼ÿ����λ����Ϣ������
	private List<LatLng> list = new ArrayList<LatLng>();
	private List<Double> timeList = new ArrayList<Double>();
	private List<Double> speedList = new ArrayList<Double>();
	private List<LatLng> beforeList = new ArrayList<LatLng>();
	// ��ť
	private Button end;
	private TextView jifei;
	// �Ƿ��һ�ζ�λ
	private boolean isFirstLocate = true;
	LatLng lastLatLng;

	/* �Ƿ��ϳ� */
	private boolean isGetOn = false;
	/* �Ƿ��һ���� */
	private boolean isNext = true;
	/* ������˼���ʱ�� */
	private long clickTime = 0;
	/*�ж��Ƿ��ϳ� �����³�*/
	private int count = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);// ����ʾ����ı�����
		setContentView(R.layout.activity_map);
		mapView = (MapView) findViewById(R.id.map);

		mapView.onCreate(savedInstanceState);// �˷���������д
		init();
		end = (Button) findViewById(R.id.end);
		jifei = (TextView) findViewById(R.id.jifei);

		/*
		 * �³� ������
		 */
		end.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				/* deactivate(); */
				
				if(count == 0){
					init();
					// ���붨λһ��
					if (!isNext)
						mAMapLocationManager.requestLocationData(
								LocationProviderProxy.AMapNetwork, 2 * 1000, 2,
								CarRoute.this);
	
					isGetOn = true;
					if (polyline != null) {
						polyline.remove();
						Log.i("remove", "remove");
					}
	
					jifei.setText("��ʼ����۸���...");
					end.setText("�³�");
					count = 1;
				}else{
					/*
					 * �³�
					 */
					// ��·��
					if (timeList.size() != 0 && list.size() != 0
							&& speedList.size() != 0) {
						double total = 0;
						double total_jifei = 0;
						double total_delay_time_jifei = 0;
						int delay_time_count = 0;
						double total_delay = 0;
						// ��ӡ���
						String all_distances = "before:\r\n";
						String right_distance = "after:\r\n";
						String unGetOn = "û�ϳ�:\r\n";
						// ǰһ����ȷ����Ϣ
						double first = timeList.get(0);
						LatLng firstPosition = list.get(0);
						double first_speed = speedList.get(0);
						// ǰһ���Ƿ���ٶ��Ƿ��и���
						boolean isRight = true;
						// ǰһ���ж��Ƿ��ǡ���ȷ���Ľ��
						boolean isResult = true;

						int count = 0;
						// δ�ϳ�����Ϣ
						for (int i = 1; i < beforeList.size(); i++) {
							double distance = AMapUtils.calculateLineDistance(
									beforeList.get(i), beforeList.get(i - 1));
							unGetOn += distance + "\r\n";
						}
						// �������
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

							// �����жϣ��뾶&���ٶ��ж�
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
								// �ƷѲ��ֵ���
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
						//����ȥ��С������ʾ��  (int)Math.floor(total)
						jifei.setText("�ܾ���:"+(int)Math.floor(total)+"��\n������"
								+ String.valueOf(inital_jifei.Getfujiafei()
										+ inital_jifei.Getqibujia() + total_jifei
										+ total_delay_time_jifei));
						Log.i("JIFEI",
								total_jifei + " " + inital_jifei.Getqibujia());
						

					} else {
						jifei.setText("û�з���");
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
					end.setText("�ϳ�");
					/*
					 * �³�
					 */
				}
				
				
			}
		});

	}

	/**
	 * ��ʼ��
	 */
	private void init() {
		if (aMap == null) {
			aMap = mapView.getMap();
			setUpMap();
			Log.i("init", "init");
		}

	}

	/**
	 * ����һЩamap������
	 */
	private void setUpMap() {
		aMap.setLocationSource(this);// ���ö�λ����
		aMap.getUiSettings().setMyLocationButtonEnabled(false);// ����Ĭ�϶�λ��ť�Ƿ���ʾ
		aMap.setMyLocationEnabled(true);// ����Ϊtrue��ʾ��ʾ��λ�㲢�ɴ�����λ��false��ʾ���ض�λ�㲢���ɴ�����λ��Ĭ����false
		// ���ö�λ������Ϊ��λģʽ �������ɶ�λ��������ͼ������������ת����,����ģʽ
		aMap.setMyLocationType(AMap.LOCATION_TYPE_MAP_FOLLOW);
		/* �������ż��� */
		aMap.moveCamera(CameraUpdateFactory.zoomTo(15));
		aMap.getUiSettings().setZoomControlsEnabled(false);
		Log.i("setup", "setup");
	}

	protected void onResume() {
		super.onResume();
		mapView.onResume();
	}

	/**
	 * ����������д
	 */

	@Override
	protected void onPause() {
		super.onPause();
		mapView.onPause();

	}

	/**
	 * ����������д
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mapView.onSaveInstanceState(outState);
	}

	/**
	 * ����������д
	 */

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mapView.onDestroy();
		deactivate();
	}

	/**
	 * �˷����Ѿ�����
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
	 * ��λ�ɹ���ص�����
	 */
	@Override
	public void onLocationChanged(AMapLocation amapLocation) {
		if (mListener != null && amapLocation != null) {
			if (amapLocation.getAMapException().getErrorCode() == 0) {

				double lan = amapLocation.getLatitude();
				double lon = amapLocation.getLongitude();

				LatLng p1 = new LatLng(lan, lon);
				mListener.onLocationChanged(amapLocation);// ��ʾϵͳС����

				// String address = amapLocation.getAddress();
//				Time t = new Time();
//				t.setToNow();
//				current_timeList.add(t.hour);
				Calendar calendar = Calendar.getInstance();
				double lastTime = calendar.getTimeInMillis();
				// �����λ׼ȷ
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
				Toast.makeText(CarRoute.this, "��·�����⿩", 1000).show();
			}
		}
		Log.i("change", "change");
	}

	/* �жϼ���Ƿ���ȷ */
	public boolean isProperDistance(double x, double interval) {
		if (x <= INTERVAL_DISTANCE * interval)
			return true;
		else
			return false;
	}

	/* �ж��Ƿ�³� */

	public boolean isDelay(double x, double interval) {
		if (x <= DELAY_DISTANCE * interval)
			return true;
		else
			return false;
	}

	/**
	 * ���λ
	 */
	@Override
	public void activate(OnLocationChangedListener listener) {
		mListener = listener;
		if (mAMapLocationManager == null) {
			mAMapLocationManager = LocationManagerProxy.getInstance(this);
			// �˷���Ϊÿ���̶�ʱ��ᷢ��һ�ζ�λ����Ϊ�˼��ٵ������Ļ������������ģ�
			// ע�����ú��ʵĶ�λʱ��ļ������С���֧��Ϊ2000ms���������ں���ʱ�����removeUpdates()������ȡ����λ����
			// �ڶ�λ�������ں��ʵ��������ڵ���destroy()����
			// ����������ʱ��Ϊ-1����λֻ��һ��
			// �ڵ��ζ�λ����£���λ���۳ɹ���񣬶��������removeUpdates()�����Ƴ����󣬶�λsdk�ڲ����Ƴ�

			/* ȷ���״ζ�λ׼ȷ��������λ5�� */
			// �ϳ��Ͷ�λ
			// �����ֱ�Ϊ ��λ��ʽ  ��λʱ���� 2s λ�ñ仯֪ͨ
			mAMapLocationManager.requestLocationData(
					LocationProviderProxy.AMapNetwork, 2 * 1000, 2, this);

		}
		Log.i("activate", "activate");
	}

	/**
	 * ֹͣ��λ
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
	 * д���ļ�
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

	// ��ýϴ��˲ʱ�ٶ�
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
			Toast.makeText(getApplicationContext(), "�ٰ�һ�κ��˼��˳�����",
					Toast.LENGTH_SHORT).show();
			clickTime = System.currentTimeMillis();
		} else {
			this.finish();
			// System.exit(0);
		}
	}
}

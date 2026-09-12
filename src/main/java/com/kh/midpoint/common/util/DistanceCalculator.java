package com.kh.midpoint.common.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// 위경도 두 점의 직선거리를 미터로 낸다(Haversine).
// 전에는 같은 계산이 TrackingService, MidPointService, TmapRouteClient 에 각각 있었다.
// 앞의 둘은 문자 그대로 같았고 TmapRouteClient 만 atan2 로 쓰여 있었다. 수학적으로는
// 같지만 지구 반지름이나 정밀도 처리를 바꿀 때 세 곳을 모두 고쳐야 했다.
//
// asin 쪽으로 통일한다. atan2 판에는 없던 클램프가 여기에는 있다. 부동소수 오차로
// a 가 1을 아주 조금 넘으면 sqrt(1-a) 가 NaN 이 되는데, 같은 좌표 두 개를 넣는 경우가
// 실제로 있다(추적 시작 직후, 재설정 전후 비교).
@Component
public class DistanceCalculator {

	@Value("${route.earth-radius-meters}")
	private double earthRadiusMeters;

	public double findDistanceMeters(double lat1, double lng1, double lat2, double lng2) {
		double latSin = Math.sin(Math.toRadians(lat2 - lat1) / 2);
		double lngSin = Math.sin(Math.toRadians(lng2 - lng1) / 2);
		double a = latSin * latSin
				+ Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * lngSin * lngSin;
		return earthRadiusMeters * 2 * Math.asin(Math.sqrt(Math.min(1.0, Math.max(0.0, a))));
	}

}

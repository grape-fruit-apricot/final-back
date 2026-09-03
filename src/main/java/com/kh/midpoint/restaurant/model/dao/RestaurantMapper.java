package com.kh.midpoint.restaurant.model.dao;

import com.kh.midpoint.restaurant.model.dto.RestaurantResponseDto;
import com.kh.midpoint.restaurant.model.vo.Restaurant;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RestaurantMapper {

	void insertRestaurant(Restaurant restaurant);

	void insertRestaurantList(List<Restaurant> restaurants);

	List<RestaurantResponseDto> findRestaurantList(String roomUuid);

}

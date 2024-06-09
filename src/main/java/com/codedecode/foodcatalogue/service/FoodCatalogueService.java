package com.codedecode.foodcatalogue.service;

import com.codedecode.foodcatalogue.dto.FoodCataloguePage;
import com.codedecode.foodcatalogue.dto.FoodItemDTO;
import com.codedecode.foodcatalogue.dto.Restaurant;
import com.codedecode.foodcatalogue.entity.FoodItem;
import com.codedecode.foodcatalogue.mapper.FoodItemMapper;
import com.codedecode.foodcatalogue.repo.FoodItemRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class FoodCatalogueService {

    @Autowired
    FoodItemRepo foodItemRepo;

    @Autowired
    RestTemplate restTemplate;

    public FoodItemDTO addFoodItem(FoodItemDTO foodItemDTO) {
        //remember you can save entity, you cannot save DTO
        //but you need to return DTO, therefore use mapper to switch in between
        FoodItem foodItemSavedInDB = foodItemRepo.save(FoodItemMapper.INSTANCE.mapFoodItemDTOToFoodItem(foodItemDTO));
        return FoodItemMapper.INSTANCE.mapFoodItemToFoodItemDto(foodItemSavedInDB);
    }

    public FoodCataloguePage fetchFoodCataloguePageDetails(Integer restaurantId) {
        //first, determine what to return, we are going to return a DTO item - FoodCataloguePage
        //whihc includes two info: (1)a list of food item entity (2) restaurant DTO
        //following 3 steps are defined in other methods
        List<FoodItem> foodItemList =  fetchFoodItemList(restaurantId); //just use the repo.findBy method
        Restaurant restaurant = fetchRestaurantDetailsFromRestaurantMS(restaurantId);
        //this one needs to use the restTemplate to fetch RestaurantDetails from Restaurant microservice
        //here we use the restTemplate to communicate between services
        //of course inject restTemplate dependecy above first
        return createFoodCataloguePage(foodItemList, restaurant);
        //createFoodCataloguePage method is responsible
        // to merge two responses as one FoodCataloguePage which will be returned
    }

    private FoodCataloguePage createFoodCataloguePage(List<FoodItem> foodItemList, Restaurant restaurant) {
        FoodCataloguePage foodCataloguePage = new FoodCataloguePage();
        //use the setter method provided by @lombok
        foodCataloguePage.setFoodItemsList(foodItemList);
        foodCataloguePage.setRestaurant(restaurant);
        return foodCataloguePage;
    }

    private Restaurant fetchRestaurantDetailsFromRestaurantMS(Integer restaurantId) {
        //this method is going to hit restaurant microservice
        //(1) inject restTemplate in the service file
        //(2) update the application file
//          (a)import org.springframework.web.client.RestTemplate;
//          (b)
//	@LoadBalanced
// we need to use loadbalance
// becuase there could be multiple instances of the restaurant listing microservice
//	public RestTemplate getRestTemplate() {
//		return new RestTemplate();
//	}
       return restTemplate.getForObject("http://RESTAURANT-SERVICE/restaurant/fetchById/"+restaurantId, Restaurant.class);
    //  (1) with this url, you can run this microservie
    //  (2) the second parameter that getforobject method takes is the response type.
    }

    //additional comments about (1) above
    //So currently we are making this restaurant listing microservice up and running.
    //We know it's up and running on 9091.
    //What we can do is we can directly hit http localhost 9091 and fetch the restaurant details,
    // but that is not the right way to do it because we are using Eureka in between.
    //You should always use the name of the service that is restaurant-service
    // that you have given in the yml file of restaurant-service.

    //Since there can be multiple instances at different ports and different instances running, you should
    //always use the load balanced URL format to hit the particular Crossovers in a microservice architecture.
    //You can do it localhost 9091 but that will not fulfill your request when you have load balanced multiple
    //instances running on Eureka on cloud.

    //So do it in a better way---create a URL in the load balanced way.
    //The URL can be http://microservice-name, (rather than http://localhost:9091)
    //and then it will be the task of Eureka to understand this.


    //additional comments about (2) above
    //So you will get a response, but you need to map that response to your own Pojo.
    //So we have already created one Pojo or DTO that is restaurant listing microservice dto folder.
    //So whatever response you get from the restaurant listing, microservice will be mapped to restaurant

    //(3)
    //Now remember, this is a very bad habit to use the URL directly here.
    //What you can do is you can rather using the hard coding here,
    //you can do it in the yml file
    // or use the config server and add this URL to that properties file
    // so that whenever the URL changes or some nitpicker changes here, you don't have to change your source code.

    private List<FoodItem> fetchFoodItemList(Integer restaurantId) {
        return foodItemRepo.findByRestaurantId(restaurantId);
    }
}

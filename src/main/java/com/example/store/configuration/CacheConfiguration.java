package com.example.store.configuration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfiguration {

    /*
       This is a simple ConcurrentMapCacheManager. In a production environment (like a kubernetes cluster) one would
       side load a single Redis container so that multiple application pods could access a single cache.
    */

    public static final String ORDERS = "orders";
    public static final String ALL_ORDERS = "all_orders";
    public static final String CUSTOMERS = "customers";
    public static final String ALL_CUSTOMERS = "all_customers";

    @Bean(name = "cacheManager")
    public CacheManager cacheManager() {
        /*
           I have created caches for list get all and get by ID for each instance. One could attempt of manage
           both cases with a single cache but that approach has a bunch of issues around concurrency.
        */
        return new ConcurrentMapCacheManager(ORDERS, ALL_CUSTOMERS, CUSTOMERS, ALL_ORDERS);
    }
}

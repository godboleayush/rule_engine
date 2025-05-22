package org.example.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class CustomEvaluatorService {

    public Integer customEvaluator(String fun , List<Integer> arr){
        if(Objects.equals(fun, "sum")){
            int sum = 0;
            for(Integer x : arr){
                sum += x;
            }
            return sum;
        }
        else if(Objects.equals(fun, "minus")){
            int diff = arr.get(0);
            for (int i = 1 ; i < arr.size() ; i++){
                diff -= arr.get(i);
            }
            return diff;
        }
        else if(Objects.equals(fun, "multiply")){
            int product = 1;
            for(Integer x : arr){
                product *= x;
            }
            return product;
        }
        else if(Objects.equals(fun, "divide")){
            return arr.get(0) / arr.get(1);
        }
        else if(Objects.equals(fun, "mod")){
            return arr.get(0) % arr.get(1);
        }
        else if(Objects.equals(fun, "custom")){
            int sum = 0;
            for(Integer x : arr){
                sum += x;
            }
            return sum;
        }

        return -1;
    }

}
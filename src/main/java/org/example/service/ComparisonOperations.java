package org.example.service;

import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class ComparisonOperations {

    public boolean EqualEqualOperationString(String a , String b){
        return Objects.equals(a, b);
    }

    public boolean NotEqualOperationString(String a , String b){
        return !Objects.equals(a, b);
    }

    public boolean EqualEqualOperationInteger(Integer a , Integer b){
        return Objects.equals(a, b);
    }

    public boolean NotEqualOperationInteger(Integer a , Integer b){
        return !Objects.equals(a, b);
    }

    public boolean GreaterThanOperation(Integer a , Integer b){
        return a > b;
    }

    public boolean LessThanOperation(Integer a , Integer b){
        return a < b;
    }

    public boolean GreaterThanEqualToOperation(Integer a , Integer b){
        return a >= b;
    }

    public boolean LessThanEqualToOperation(Integer a , Integer b){
        return a <= b;
    }

}
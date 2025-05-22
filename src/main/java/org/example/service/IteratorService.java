package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class IteratorService {

    @Autowired
    private ComparisonOperations comparisonOperations;

    @Autowired
    private CustomEvaluatorService customEvaluatorService;

    @Autowired
    private MrService mrService;


    public boolean checkIteration (JsonNode data , Map<String, Object> flatMap){
        JsonNode op = data.get("op");
        String operation = op.textValue();
        AtomicBoolean andFlag = new AtomicBoolean(false);
        AtomicBoolean orFlag = new AtomicBoolean(false);
        AtomicBoolean xorFlag = new AtomicBoolean(false);
        AtomicBoolean notFlag = new AtomicBoolean(false);
        boolean xorReturnVal = false;
        boolean notReturnVal = false;
        boolean returnVal = true;
        List<String> funs = new ArrayList<>(Arrays.asList("sum", "minus", "multiply", "divide", "mod", "custom"));

        if(Objects.equals(operation, "and") ||Objects.equals(operation, "or") || Objects.equals(operation, "xor") || Objects.equals(operation, "not")){
            JsonNode params = data.get("params");
            for (JsonNode param : params) {
                boolean result = checkIteration(param , flatMap);
                if(Objects.equals(operation, "and")){
                    if(!result){
                        andFlag.set(true);
                        break;
                    }
                }
                if(Objects.equals(operation, "or")){
                    if(result){
                        orFlag.set(true);
                        break;
                    }
                }
                if(Objects.equals(operation, "xor")){
                    xorFlag.set(true);
                    xorReturnVal = xorReturnVal ^ result;
                }
                if (Objects.equals(operation, "not")){
                    notFlag.set(true);
                    notReturnVal = result;
                }
            };
        }
        else if(Objects.equals(operation, "==")){

            JsonNode params = data.get("params");
            JsonNode d1 = params.get(0);
            JsonNode d2 = params.get(1);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode p1 = mrService.resolveToJsonNode(d1 , flatMap , mapper);
            JsonNode p2 = mrService.resolveToJsonNode(d2 , flatMap , mapper);

//            System.out.println("p1 = " + p1 + ", type = " + p1.getNodeType());
//            System.out.println("p2 = " + p2 + ", type = " + p2.getNodeType());

            if(p1.isTextual() && p2.isTextual()){
                String v1 = p1.textValue();
                String v2 = p2.textValue();
//                System.out.println("section 1");
                return comparisonOperations.EqualEqualOperationString(v1 ,v2);
            }
            else if(p1.isNumber() && p2.isNumber()){
                Integer v1 = p1.intValue();
                Integer v2 = p2.intValue();
//                System.out.println("section 2");
                return comparisonOperations.EqualEqualOperationInteger(v1 ,v2);
            }
            else{
                String v1 = p1.isTextual() ? p1.textValue() : p2.textValue();
                Integer v2 = p1.isTextual() ? p2.intValue() : p1.intValue();
                boolean flag_notreached = false;
                for(String fun : funs){
                    int len = fun.length();
                    String curr = v1.substring(0, len);
                    if(Objects.equals(curr , fun)){
                        List<Integer> arr = new ArrayList<>();
                        String argsStr = v1.substring(len + 1, v1.length() - 1);
                        String[] args = argsStr.split(",");
                        for (String arg : args) {
                            arr.add(Integer.parseInt(arg.trim()));
                        }
                        Integer val = customEvaluatorService.customEvaluator(fun , arr);
                        flag_notreached = true;
                        return comparisonOperations.EqualEqualOperationInteger(val ,v2);
                    }
                }
                if(!flag_notreached){
                    return false;
                }
            }
        }
        else if (Objects.equals(operation, "!=")) {
            JsonNode params = data.get("params");
            JsonNode d1 = params.get(0);
            JsonNode d2 = params.get(1);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode p1 = mrService.resolveToJsonNode(d1 , flatMap , mapper);
            JsonNode p2 = mrService.resolveToJsonNode(d2 , flatMap , mapper);

            if(p1.isTextual() && p2.isTextual()){
                String v1 = p1.textValue();
                String v2 = p2.textValue();
                return comparisonOperations.NotEqualOperationString(v1 ,v2);
            }
            else if(p1.isInt() && p2.isInt()){
                Integer v1 = p1.intValue();
                Integer v2 = p2.intValue();
                return comparisonOperations.NotEqualOperationInteger(v1 ,v2);
            }
            else{
                String v1 = p1.isTextual() ? p1.textValue() : p2.textValue();
                Integer v2 = p1.isTextual() ? p2.intValue() : p1.intValue();
                boolean flag_notreached = false;
                for(String fun : funs){
                    int len = fun.length();
                    String curr = v1.substring(0, len);
                    if(Objects.equals(curr , fun)){
                        List<Integer> arr = new ArrayList<>();
                        String argsStr = v1.substring(len + 1, v1.length() - 1);
                        String[] args = argsStr.split(",");
                        for (String arg : args) {
                            arr.add(Integer.parseInt(arg.trim()));
                        }
                        Integer val = customEvaluatorService.customEvaluator(fun , arr);
                        flag_notreached = true;
                        return comparisonOperations.NotEqualOperationInteger(val ,v2);
                    }
                }
                if(!flag_notreached){
                    return false;
                }
            }
        }
        else if(Objects.equals(operation, ">")){
            JsonNode params = data.get("params");
            JsonNode d1 = params.get(0);
            JsonNode d2 = params.get(1);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode p1 = mrService.resolveToJsonNode(d1 , flatMap , mapper);
            JsonNode p2 = mrService.resolveToJsonNode(d2 , flatMap , mapper);

            if(p1.isInt() && p2.isInt()){
                Integer v1 = p1.intValue();
                Integer v2 = p2.intValue();
                return comparisonOperations.GreaterThanOperation(v1 ,v2);
            }
            else{
                String v1 = p1.isTextual() ? p1.textValue() : p2.textValue();
                Integer v2 = p1.isTextual() ? p2.intValue() : p1.intValue();
                for(String fun : funs){
                    int len = fun.length();
                    String curr = v1.substring(0, len);
                    if(Objects.equals(curr , fun)){
                        List<Integer> arr = new ArrayList<>();
                        String argsStr = v1.substring(len + 1, v1.length() - 1);
                        String[] args = argsStr.split(",");
                        for (String arg : args) {
                            arr.add(Integer.parseInt(arg.trim()));
                        }
                        Integer val = customEvaluatorService.customEvaluator(fun , arr);
                        return p1.isTextual() ? comparisonOperations.GreaterThanOperation(val ,v2) : comparisonOperations.GreaterThanOperation(v2 , val);
                    }
                }
            }
        }
        else if(Objects.equals(operation, "<")){
            JsonNode params = data.get("params");
            JsonNode d1 = params.get(0);
            JsonNode d2 = params.get(1);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode p1 = mrService.resolveToJsonNode(d1 , flatMap , mapper);
            JsonNode p2 = mrService.resolveToJsonNode(d2 , flatMap , mapper);

            if(p1.isInt() && p2.isInt()){
                Integer v1 = p1.intValue();
                Integer v2 = p2.intValue();
                return comparisonOperations.LessThanOperation(v1 ,v2);
            }
            else{
                String v1 = p1.isTextual() ? p1.textValue() : p2.textValue();
                Integer v2 = p1.isTextual() ? p2.intValue() : p1.intValue();
                for(String fun : funs){
                    int len = fun.length();
                    String curr = v1.substring(0, len);
                    if(Objects.equals(curr , fun)){
                        List<Integer> arr = new ArrayList<>();
                        String argsStr = v1.substring(len + 1, v1.length() - 1);
                        String[] args = argsStr.split(",");
                        for (String arg : args) {
                            arr.add(Integer.parseInt(arg.trim()));
                        }
                        Integer val = customEvaluatorService.customEvaluator(fun , arr);
                        return p1.isTextual() ? comparisonOperations.LessThanOperation(val ,v2) : comparisonOperations.LessThanOperation(v2 , val);
                    }
                }
            }

        }
        else if(Objects.equals(operation, "<=")){
            JsonNode params = data.get("params");
            JsonNode d1 = params.get(0);
            JsonNode d2 = params.get(1);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode p1 = mrService.resolveToJsonNode(d1 , flatMap , mapper);
            JsonNode p2 = mrService.resolveToJsonNode(d2 , flatMap , mapper);

            if(p1.isInt() && p2.isInt()){
                Integer v1 = p1.intValue();
                Integer v2 = p2.intValue();
                return comparisonOperations.LessThanEqualToOperation(v1 ,v2);
            }
            else{
                String v1 = p1.isTextual() ? p1.textValue() : p2.textValue();
                Integer v2 = p1.isTextual() ? p2.intValue() : p1.intValue();
                for(String fun : funs){
                    int len = fun.length();
                    String curr = v1.substring(0, len);
                    if(Objects.equals(curr , fun)){
                        List<Integer> arr = new ArrayList<>();
                        String argsStr = v1.substring(len + 1, v1.length() - 1);
                        String[] args = argsStr.split(",");
                        for (String arg : args) {
                            arr.add(Integer.parseInt(arg.trim()));
                        }
                        Integer val = customEvaluatorService.customEvaluator(fun , arr);
                        return p1.isTextual() ? comparisonOperations.LessThanEqualToOperation(val ,v2) : comparisonOperations.LessThanEqualToOperation(v2 , val);
                    }
                }
            }

        }
        else if(Objects.equals(operation, ">=")){
            JsonNode params = data.get("params");
            JsonNode d1 = params.get(0);
            JsonNode d2 = params.get(1);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode p1 = mrService.resolveToJsonNode(d1 , flatMap , mapper);
            JsonNode p2 = mrService.resolveToJsonNode(d2 , flatMap , mapper);

            if(p1.isInt() && p2.isInt()){
                Integer v1 = p1.intValue();
                Integer v2 = p2.intValue();
                return comparisonOperations.GreaterThanEqualToOperation(v1 ,v2);
            }
            else{
                String v1 = p1.isTextual() ? p1.textValue() : p2.textValue();
                Integer v2 = p1.isTextual() ? p2.intValue() : p1.intValue();
                for(String fun : funs){
                    int len = fun.length();
                    String curr = v1.substring(0, len);
                    if(Objects.equals(curr , fun)){
                        List<Integer> arr = new ArrayList<>();
                        String argsStr = v1.substring(len + 1, v1.length() - 1);
                        String[] args = argsStr.split(",");
                        for (String arg : args) {
                            arr.add(Integer.parseInt(arg.trim()));
                        }
                        Integer val = customEvaluatorService.customEvaluator(fun , arr);
                        return p1.isTextual() ? comparisonOperations.GreaterThanEqualToOperation(val ,v2) : comparisonOperations.GreaterThanEqualToOperation(v2 , val);
                    }
                }
            }

        }
        else if(Objects.equals(operation, "in")){
            JsonNode params = data.get("params");
            JsonNode d1 = params.get(0);
            JsonNode p2 = params.get(1);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode p1 = mrService.resolveToJsonNode(d1 , flatMap , mapper);

            int key = p1.asInt();
            for (JsonNode element : p2) {
                if (element.asInt() == key) {
                    return true;
                }
            }
            return false;
        }

        if(andFlag.get()){
            return false;
        }
        if(orFlag.get()){
            return true;
        }
        if(xorFlag.get()){
            return xorReturnVal;
        }
        if(notFlag.get()){
            return !notReturnVal;
        }

        if(Objects.equals(operation, "and")){
            return true;
        }
        if(Objects.equals(operation, "or")){
            return false;
        }

        return true;
    }
}

package com.eema.backend.service;
import com.eema.backend.entity.Venue;
import com.eema.backend.entity.enums.ProductionActivity;
import com.eema.backend.service.ai.ParsedBrief;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.*;
import static org.junit.jupiter.api.Assertions.*;

public class MatchingEligibilityContractTest {
    public record Case(String label,Map<String,Object> requirement,Map<String,Object> facts,boolean eligible){public String toString(){return label;}}
    public static final Map<String,String> BOOLEAN_FIELDS=Map.of("requiresNaturalLight","naturalLight","requiresBlackout","blackoutPossible","requiresLoadingAccess","loadingAccess","requiresParking","parkingAvailable","requiresWheelchairAccessibility","wheelchairAccessible","requiresLift","liftAvailable","requiresBathrooms","bathroomsAvailable","requiresSoundRecordingSuitable","soundRecordingSuitable");
    public static Stream<Case> cases(){
        List<Case> cases=new ArrayList<>();
        BOOLEAN_FIELDS.forEach((required,actual)->{
            for(Boolean value:Arrays.asList(null,false,true))for(Boolean need:Arrays.asList(null,false,true)){
                Map<String,Object> r=new HashMap<>(),f=new HashMap<>();r.put(required,need);f.put(actual,value);
                cases.add(new Case(required+"="+need+" / "+value,r,f,!Boolean.TRUE.equals(need)||Boolean.TRUE.equals(value)));
            }
        });
        for(var pair:List.of(new String[]{"numPeople","maxCrewSize","12"},new String[]{"minimumParkingSpaces","parkingSpaces","2"},new String[]{"minimumCeilingHeightM","ceilingHeightM","3.5"})){
            var minimum=new java.math.BigDecimal(pair[2]);
            for(var value:Arrays.asList(null,minimum,minimum.subtract(new java.math.BigDecimal("0.01")),minimum.add(java.math.BigDecimal.ONE))){
                if(!pair[0].equals("minimumCeilingHeightM")&&value!=null)value=value.setScale(0,java.math.RoundingMode.FLOOR);
                Map<String,Object> f=new HashMap<>();f.put(pair[1],value);f.put("parkingAvailable",true);
                cases.add(new Case(pair[0]+" / "+value,Map.of(pair[0],minimum),f,value!=null&&value.compareTo(minimum)>=0));
            }
        }
        for(Boolean parking:Arrays.asList(null,false,true)){
            Map<String,Object> f=new HashMap<>();f.put("parkingAvailable",parking);f.put("parkingSpaces",0);
            cases.add(new Case("zero parking / "+parking,Map.of("minimumParkingSpaces",0),f,Boolean.TRUE.equals(parking)));
        }
        for(var a:ProductionActivity.values()){
            for(String variant:List.of("permitted","prohibited","unknown","empty","unknownProhibition")){
                Map<String,Object> f=new HashMap<>();f.put("permittedActivities",variant.equals("unknown")?null:variant.equals("empty")?List.of():List.of(a));
                f.put("prohibitedActivities",variant.equals("unknownProhibition")?null:variant.equals("prohibited")?List.of(a):List.of());
                cases.add(new Case(a+" / "+variant,Map.of("productionActivity",a),f,variant.equals("permitted")||variant.equals("unknownProhibition")));
            }
        }
        return cases.stream();
    }
    public static Venue facts(Map<String,Object> values)throws Exception{
        Venue v=Venue.builder().build();ObjectMapper mapper=new ObjectMapper().findAndRegisterModules();
        for(var e:values.entrySet()){
            var field=Venue.class.getDeclaredField(e.getKey());field.setAccessible(true);field.set(v,mapper.convertValue(e.getValue(),mapper.getTypeFactory().constructType(field.getGenericType())));
        }return v;
    }
    @ParameterizedTest @MethodSource("cases") void hardEligibility(Case c)throws Exception{
        var brief=new ObjectMapper().findAndRegisterModules().convertValue(c.requirement(),ParsedBrief.class);
        assertEquals(c.eligible(),new VenueHardRequirementEvaluator().isEligible(facts(c.facts()),brief));
    }
}

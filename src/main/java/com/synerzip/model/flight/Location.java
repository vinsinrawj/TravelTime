package com.synerzip.model.flight;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
"latitude",
"longitude"
})
public class Location {

@JsonProperty("latitude")
private double latitude;
@JsonProperty("longitude")
private double longitude;
@JsonIgnore
private Map<String, Object> additionalProperties = new HashMap<String, Object>();

/**
* 
* @return
* The latitude
*/
@JsonProperty("latitude")
public double getLatitude() {
return latitude;
}

/**
* 
* @param latitude
* The latitude
*/
@JsonProperty("latitude")
public void setLatitude(double latitude) {
this.latitude = latitude;
}

public Location withLatitude(double latitude) {
this.latitude = latitude;
return this;
}

/**
* 
* @return
* The longitude
*/
@JsonProperty("longitude")
public double getLongitude() {
return longitude;
}

/**
* 
* @param longitude
* The longitude
*/
@JsonProperty("longitude")
public void setLongitude(double longitude) {
this.longitude = longitude;
}

public Location withLongitude(double longitude) {
this.longitude = longitude;
return this;
}

@JsonAnyGetter
public Map<String, Object> getAdditionalProperties() {
return this.additionalProperties;
}

@JsonAnySetter
public void setAdditionalProperty(String name, Object value) {
this.additionalProperties.put(name, value);
}

public Location withAdditionalProperty(String name, Object value) {
this.additionalProperties.put(name, value);
return this;
}

}
package com.synerzip.client.rest;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.synerzip.supplier.amadeus.model.flights.AffiliateFlightSearchRQ;
import com.synerzip.supplier.amadeus.model.flights.AffiliateFlightSearchRS;
import com.synerzip.supplier.amadeus.model.flights.AirportAutocompleteRQ;
import com.synerzip.supplier.amadeus.model.flights.AirportAutocompleteRS;
import com.synerzip.supplier.amadeus.model.flights.ExtensiveSearchRQ;
import com.synerzip.supplier.amadeus.model.flights.ExtensiveSearchRS;
import com.synerzip.supplier.amadeus.model.flights.FlightInspirationSearchRS;
import com.synerzip.supplier.amadeus.model.flights.FlightInspirationSearchRQ;
import com.synerzip.supplier.amadeus.model.flights.LocationInformationSearchRQ;
import com.synerzip.supplier.amadeus.model.flights.LocationInformationSearchRS;
import com.synerzip.supplier.amadeus.model.flights.LowFareFlightSearchRQ;
import com.synerzip.supplier.amadeus.model.flights.LowFareFlightSearchRS;
import com.synerzip.supplier.amadeus.model.flights.NearestAirportSearchRQ;
import com.synerzip.supplier.amadeus.model.flights.NearestAirportSearchRS;
import com.synerzip.supplier.sabre.model.flights.instaflight_gen.InstaFlightRequest;
import com.synerzip.supplier.sabre.model.flights.instaflight_gen.InstaFlightResponse;
import com.synerzip.supplier.service.AmadeusFlightService;
import com.synerzip.supplier.service.SabreFlightService;
import com.synerzip.utilities.sabre2amadeus.writers.InstaFlightRequestWriter;
import com.synerzip.utilities.sabre2amadeus.writers.LowFareFlightSearchRSWriter;

@RestController
public class FlightsController {

	private Logger logger = LoggerFactory.getLogger(FlightsController.class);

	@Autowired
	private InstaFlightRequestWriter instaFlightRequestWriter;

	@Autowired
	private LowFareFlightSearchRSWriter lowFareFlightSearchRSWriter;

	@Autowired
	private AmadeusFlightService amadeusFlightService;

	@Autowired
	private SabreFlightService sabreFlightService;

	@Autowired
	private ThreadPoolTaskExecutor executor;
	
	private boolean lowfareSearchAsync = false;
	
	
	@RequestMapping(value = "/rest/searchFlights", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<LowFareFlightSearchRS> searchFlights(
			@RequestBody LowFareFlightSearchRQ lowFareFlightSearchRQ) {
		ConcurrentLinkedDeque<LowFareFlightSearchRS> collection = new ConcurrentLinkedDeque<>();
		
		CountDownLatch latch = new CountDownLatch(2); // count-down of two, one for Sabre and other for Amadeus.
		
		executor.execute(() -> {
			try {
				// prepare a Sabre's InstaFlightRequest from Amadeus's LowFareFlightSearchRQ
				InstaFlightRequest instaFlightRequest = instaFlightRequestWriter.write.apply(lowFareFlightSearchRQ);
				
				// fetch the response from Sabre's service
				InstaFlightResponse instaFlightResponse = sabreFlightService.doInstaFlightSearch(instaFlightRequest);
				
				// write Amadeus's LowFareFlightSearchRS using Sabre's InstaFlightResponse
				LowFareFlightSearchRS response = lowFareFlightSearchRSWriter.write.apply(instaFlightResponse);
				
				collection.add(response);
			} catch(Exception e) {
				logger.error("An error has occured while processing Sabre Request", e);
			} finally {
				latch.countDown();
			}
		});

		executor.execute(() -> {
			try {
				collection.add(amadeusFlightService.fetchLowFareFlights(lowFareFlightSearchRQ,lowfareSearchAsync));
			} catch (Exception e) {
				logger.error("An error has occured while processing Amadeus Request", e);
			} finally {
				latch.countDown();
			}
		});
		
		try {
			latch.await();
		} catch (InterruptedException e) {
			logger.error("Request executor thread has been interrupted", e);
			e.printStackTrace();
		}

		// the two responses are from Amadeus's and Sabre's. we just merge them into the one
		LowFareFlightSearchRS first = collection.getFirst();
		LowFareFlightSearchRS second = collection.getLast();
		
		first.getResults().addAll(second.getResults());
		
		return new ResponseEntity<LowFareFlightSearchRS>(first, HttpStatus.OK);
	}
	
	//this is asynchronous search call for low fare 
	//not tested
	@RequestMapping(value = "/rest/async/searchLowFare", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public AsyncResult<LowFareFlightSearchRS> searchLowFareFlightsAsync(@RequestBody LowFareFlightSearchRQ lowFareFlightSearchRQ) {
		return new AsyncResult<LowFareFlightSearchRS>(amadeusFlightService.fetchLowFareFlights(lowFareFlightSearchRQ,lowfareSearchAsync));
	}
	
	// This service retrieves prices of flights over a large number of days.
	@RequestMapping(value = "/rest/searchExtensive", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ExtensiveSearchRS> searchFlightExtensive(@RequestBody ExtensiveSearchRQ extensiveSearchRQ) {
		return new ResponseEntity<ExtensiveSearchRS>(amadeusFlightService.fetchExtensiveFlights(extensiveSearchRQ),
				HttpStatus.OK);
	}

	// The Inspiration Flight Search allows you to find the prices of one-way
	// and return flights from an origin city without necessarily having a
	// destination, or even a flight date
	@RequestMapping(value = "/rest/searchFlightInspiration", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<FlightInspirationSearchRS> searchFlightInspiration(
			@RequestBody FlightInspirationSearchRQ inspirationSearchRQ) {
		return new ResponseEntity<FlightInspirationSearchRS>(amadeusFlightService.fetchInspirationFlights(inspirationSearchRQ),
				HttpStatus.OK);
	}

	// This service retrieves the location information corresponding to a IATA
	// city or airport code.
	@RequestMapping(value = "/rest/searchLocationInformation", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<LocationInformationSearchRS> searchLocationInformation(
			@RequestBody LocationInformationSearchRQ locationInformationSearchRQ) {
		return new ResponseEntity<LocationInformationSearchRS>(
				amadeusFlightService.fetchAiportLocation(locationInformationSearchRQ), HttpStatus.OK);
	}

	@RequestMapping(value = "/rest/searchAffiliate", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<AffiliateFlightSearchRS> searchFlightAffiliate(
			@RequestBody AffiliateFlightSearchRQ affiliateFlightSearchRQ) {
		return new ResponseEntity<AffiliateFlightSearchRS>(
				amadeusFlightService.fetchAffiliateFlights(affiliateFlightSearchRQ), HttpStatus.OK);
	}

	// This service gives the most relevant airports in a radius of 500 km
	// around the given coordinates.
	@RequestMapping(value = "/rest/searchNearestAirport", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<NearestAirportSearchRS[]> searchNearestAirport(
			@RequestBody NearestAirportSearchRQ nearestAirportSearchRQ) {
		return new ResponseEntity<NearestAirportSearchRS[]>(
				amadeusFlightService.fetchNearestAirport(nearestAirportSearchRQ), HttpStatus.OK);
	}
	
	// This service provides a full name of IATA location with their IATA code.
	@RequestMapping(value = "/rest/airportAutocomplete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<AirportAutocompleteRS[]> searchairportAutoComplete(
			@RequestBody AirportAutocompleteRQ airportAutocompleteRQ) {
		return new ResponseEntity<AirportAutocompleteRS[]>(
				amadeusFlightService.fetchAutocompleteAirport(airportAutocompleteRQ), HttpStatus.OK);
	}
}

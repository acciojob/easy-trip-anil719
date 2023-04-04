package com.driver.controllers;

import com.driver.model.Airport;
import com.driver.model.City;
import com.driver.model.Flight;
import com.driver.model.Passenger;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class AirportRepository {


    Map<String, Airport> airportMap ;
    Map<Integer,Flight> flightMap;

    Map<Integer, List<Integer>> flightToPassenger;

    Map<Integer, Passenger> passengerMap;

    AirportRepository(){
        this.airportMap = new HashMap<>();
        this.flightMap = new HashMap<>();
        this.passengerMap = new HashMap<>();
        this.flightToPassenger = new HashMap<>();
    }

    public void addAirport(Airport airport){
        airportMap.put(airport.getAirportName(), airport);
    }

    public String getLargestAirportName(){
        //Largest airport is in terms of terminals. 3 terminal airport is larger than 2 terminal airport
        //Incase of a tie return the Lexicographically smallest airportName
        int noOfTerminals = 0;
        for(Airport airport : airportMap.values()){
            noOfTerminals = Math.max(noOfTerminals, airport.getNoOfTerminals());
        }
        int terminalCountForTie = 0;

        for(Airport airport : airportMap.values()){
            if(airport.getNoOfTerminals() == noOfTerminals) terminalCountForTie ++;
        }
        if(terminalCountForTie == 1){
            for(Airport airport : airportMap.values()){
                if(airport.getNoOfTerminals() == noOfTerminals) return airport.getAirportName();
            }
        }
        else{
            List<String> airportName = new ArrayList<>();
            for(Airport airport : airportMap.values()){
                if(airport.getNoOfTerminals() == terminalCountForTie){
                    airportName.add(airport.getAirportName());
                }
            }
            Collections.sort(airportName);
            return airportName.get(0);
        }
        return null;
    }

    public double getShortestDurationOfPossibleBetweenTwoCities(City fromCity, City toCity){

        //Find the duration by finding the shortest flight that connects these 2 cities directly
        //If there is no direct flight between 2 cities return -1.
        double shorterDuration = Double.MAX_VALUE;
        for(Flight flight : flightMap.values()){
            if(flight.getFromCity() == fromCity && flight.getToCity() == toCity){
                shorterDuration = Math.min(flight.getDuration(), shorterDuration);
            }
        }
        if(shorterDuration == Double.MAX_VALUE) return -1;
        return shorterDuration;
    }

    public int getNumberOfPeopleOn(Date date, String airportName){
        //Calculate the total number of people who have flights on that day on a particular airport
        //This includes both the people who have come for a flight and who have landed on an airport after their flight
        Airport airport = airportMap.get(airportName);
        if(Objects.isNull(airport)) return 0;

        City city = airport.getCity();
        int count = 0;
        for(Flight flight : flightMap.values()){
            if(date.equals(flight.getFlightDate())){
                if(flight.getFromCity().equals(city) || flight.getToCity().equals(city)){
                    int flightId = flight.getFlightId();
                    count += flightToPassenger.get(flightId).size();
                }
            }
        }
        return count;
    }

    public int calculateFlightFare(Integer flightId){

        //Calculation of flight prices is a function of number of people who have booked the flight already.
        //Price for any flight will be : 3000 + noOfPeopleWhoHaveAlreadyBooked*50
        //Suppose if 2 people have booked the flight already : the price of flight for the third person will be 3000 + 2*50 = 3100
        //This will not include the current person who is trying to book, he might also be just checking price
        int totalBookings = flightToPassenger.get(flightId).size();
        return totalBookings * 50 + 3000 ;
    }

    public String bookATicket(Integer flightId, Integer passengerId){
        //If the numberOfPassengers who have booked the flight is greater than : maxCapacity, in that case :
        //return a String "FAILURE"
        //Also if the passenger has already booked a flight then also return "FAILURE".
        //else if you are able to book a ticket then return "SUCCESS"
        if(!flightToPassenger.containsKey(flightId)) return "FAILUE";

        int totalBookings = flightToPassenger.get(flightId).size();
        Flight flight = flightMap.get(flightId);
        if(totalBookings > flight.getMaxCapacity()) return "FAILURE";
        else if(flightToPassenger.get(flightId).contains(passengerId)) return "FAILURE" ;
        else{
            List<Integer> passengers = flightToPassenger.get(flightId);
            passengers.add(passengerId);
            flightToPassenger.put(flightId, passengers);
        }
        return "SUCCESS";
    }

    public String cancelATicket(Integer flightId, Integer passengerId){
        //If the passenger has not booked a ticket for that flight or the flightId is invalid or in any other failure case
        // then return a "FAILURE" message
        // Otherwise return a "SUCCESS" message
        // and also cancel the ticket that passenger had booked earlier on the given flightId
        if(!flightToPassenger.containsKey(flightId) || flightToPassenger.get(flightId).contains(passengerId) == false) return "FAILURE";

        List<Integer> bookedPassengerIds = flightToPassenger.get(flightId);
        bookedPassengerIds.remove(passengerId);
        flightToPassenger.put(flightId, bookedPassengerIds);
        return "SUCCESS";
    }

    public int countOfBookingsDoneByPassengerAllCombined(Integer passengerId){
        //Tell the count of flight bookings done by a passenger: This will tell the total count of flight bookings done by a passenger :
        int cnt = 0;
        for(List<Integer> passengersList : flightToPassenger.values()){
            for(Integer passengerIds : passengersList){
                if(passengerIds == passengerId) cnt++;
            }
        }
        return cnt;
    }

    public String addFlight(Flight flight){
        flightMap.put(flight.getFlightId(), flight);
        return "SUCCESS";
    }

    public String getAirportNameFromFlightId(Integer flightId){
        //We need to get the starting airportName from where the flight will be taking off (Hint think of City variable if that can be of some use)
        //return null incase the flightId is invalid or you are not able to find the airportName
        if(flightMap.containsKey(flightId)){
            City city = flightMap.get(flightId).getFromCity();
            for(Airport airport : airportMap.values()){
                if(airport.getCity() == city) return airport.getAirportName();
            }
            return "null" ;
        }
        return null;
    }

    public int calculateRevenueOfAFlight(Integer flightId){
        //Calculate the total revenue that a flight could have
        //That is of all the passengers that have booked a flight till now and then calculate the revenue
        //Revenue will also decrease if some passenger cancels the flight

        int totalBookings = flightToPassenger.get(flightId).size();
        int variableFare = (totalBookings*totalBookings+1)*25;
        int fixedFare = 3000* totalBookings;
        int totalFare = variableFare + fixedFare;
        return totalFare;
    }

    public String addPassenger(Passenger passenger){
        //Add a passenger to the database
        //And return a "SUCCESS" message if the passenger has been added successfully.
        Integer passengerId = passenger.getPassengerId();
        passengerMap.put(passengerId, passenger);
        return "SUCCESS";
    }
}

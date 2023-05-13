package org.ridango.example;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) {

        String stopId = args[0];
        int nextBusNum = Integer.parseInt(args[1]);
        String relativeOrAbsolute = args[2];

        LocalTime timeNow = LocalTime.now();

        Map<String, String> tripIdRouteIdMap = fillTripIdRouteIdMap();

        Map<String, String> stopIdStopNameMap = fillStopIdStopNameMap();

        Map<String, List<LocalTime>> routesArrivals = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader("stop_times.txt"))) {
            String line;
            br.readLine(); // skip first line with names of values
            while ((line = br.readLine()) != null) {

                StopTime stopTime = StopTime.parse(line);

                if (stopId.equals(stopTime.stopId) && timeNow.isBefore(stopTime.arrivalTime) && timeNow.plusHours(2).isAfter(stopTime.arrivalTime)) {
                    routesArrivals.compute(tripIdRouteIdMap.get(stopTime.tripId), (routeId, busArrivals) -> {

                        if (busArrivals == null) {
                            busArrivals = new ArrayList<>();
                        }

                        int previousSize = busArrivals.size();
                        boolean inserted = false;

                        for (int i = previousSize - 1; i >= 0; i--) {
                            if (busArrivals.get(i).isBefore(stopTime.arrivalTime)) {
                                busArrivals.add(i + 1, stopTime.arrivalTime);
                                inserted = true;
                                break;
                            }
                        }

                        if (!inserted) {
                            busArrivals.add(0, stopTime.arrivalTime);
                        }

                        if (previousSize == nextBusNum) {
                            busArrivals.remove(previousSize);
                        }

                        return busArrivals;
                    });
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading csv file: " + "(stop_times.txt) " + e.getMessage());
        }

        System.out.println("Postajalisce: " + stopIdStopNameMap.get(stopId));

        routesArrivals.forEach((route, arrivalTimes) -> {
            String arrivalTimesString;
            if (relativeOrAbsolute.equals("absolute")) {
                arrivalTimesString = arrivalTimes.stream().map(LocalTime::toString).collect(Collectors.joining(", "));
            } else {
                arrivalTimesString = arrivalTimes.stream().map(time -> {
                    long s = timeNow.until(time, ChronoUnit.SECONDS);
                    return String.format("%d:%02d", s / 3600, (s % 3600) / 60);
                }).collect(Collectors.joining(", "));
            }

            System.out.println(route + ": " + arrivalTimesString);
        });

    }

    public static class StopTime {
        String tripId;
        int stopSequence;
        String stopId;
        LocalTime arrivalTime;
        LocalTime departureTime;

        public static StopTime parse(String line) {
            String[] values = line.split(",");
            StopTime stopTime = new StopTime();
            stopTime.tripId = values[0];
            stopTime.stopSequence = Integer.parseInt(values[4]);
            stopTime.stopId = values[3];
            stopTime.arrivalTime = LocalTime.parse(values[1]);
            stopTime.departureTime = LocalTime.parse(values[2]);

            return stopTime;
        }
    }

    private static Map<String, String> fillTripIdRouteIdMap() {
        Map<String, String> tripIds = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader("trips.txt"))) {
            String line;
            br.readLine(); // skip first line with names of values
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                tripIds.put(values[2], values[0]);
            }
        } catch (IOException e) {
            System.out.println("Error reading csv file: " + "(trips.txt) " + e.getMessage());
        }
        return tripIds;
    }

    private static Map<String, String> fillStopIdStopNameMap() {
        Map<String, String> stopIdToNamesMap = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader("stops.txt"))) {
            String line;
            br.readLine(); // skip first line with names of values
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                stopIdToNamesMap.put(values[0], values[2]);
            }
        } catch (IOException e) {
            System.out.println("Error reading csv file: " + "(stops.txt) " + e.getMessage());
        }
        return stopIdToNamesMap;
    }
}
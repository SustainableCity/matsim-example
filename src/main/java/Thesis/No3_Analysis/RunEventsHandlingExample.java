package Thesis.No3_Analysis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.FileNotFoundException;
import java.io.IOException;

public class RunEventsHandlingExample {

    public static void main(String[] args) throws IOException {

        //path to events file. For this you first need to run a simulation.

        Network network = NetworkUtils.createNetwork();
        MatsimNetworkReader reader = new MatsimNetworkReader(network);
        reader.readFile("C:/matsimfiles/input/mergedNetwork2018.xml");
        //String inputFile = ("C:/matsimfiles/output/simulation/output_events.xml.gz");
//        String inputFile = ("C:/matsimfiles/output/simulation/Final/Garage_seed1/ITERS/it.30/30.events.xml.gz");
        String inputFile = ("C:/matsimfiles/output/simulation/Final/WithoutCentre_Garage_seed1/ITERS/it.50/50.events.xml.gz");
//        String inputFile = ("C:/matsimfiles/output/simulation/stateoftheart/ITERS/it.30/30.events.xml.gz");

        //create an event object
        EventsManager events = EventsUtils.createEventsManager();


//        CongestionDetectionEventHandler handler = new CongestionDetectionEventHandler(network);
//        events.addHandler(handler);

//        GaragePersonEventHandler garageLegDuration = new GaragePersonEventHandler(network);
//        events.addHandler(garageLegDuration);

        GarageLegEventHandler garageLegDistance = new GarageLegEventHandler(network);
        garageLegDistance.readPersonCSV();
        events.addHandler(garageLegDistance);


        //create the reader and read the file
        MatsimEventsReader eventsReader = new MatsimEventsReader(events);
        eventsReader.readFile(inputFile);

//        System.out.println("average travel time: " + handler.getTotalTravelTime());
//        handler.writeChart("output/departuresPerHour.png");
//        handler.writeChart("C:/matsimfiles/output/simulation/Final/notNeeded.csv");
          garageLegDistance.writeList("C:/matsimfiles/output/simulation/Final/GarageDistances2TT.csv");
//        garageLegDuration.writeList("C:/matsimfiles/output/simulation/Final/garageTimeG1.csv");
        System.out.println("Events file read!");
    }
}

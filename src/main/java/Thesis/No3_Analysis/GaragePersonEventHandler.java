package Thesis.No3_Analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class GaragePersonEventHandler implements ActivityStartEventHandler, ActivityEndEventHandler {


    private Network network;

    private Map<Id<Person>, Double> personLeaveGarage;
    private Map<Id<Person>, Double> personBeenPickedUp;
    private Map<Id<Person>, Double> personBeenDropped;
    private Map<Id<Person>, Double> personAtGarage;

//    private static String csvFile = "C:/matsimfiles/output/eventAnalysis/DetectionTest.csv" ;

    public GaragePersonEventHandler(Network network ) {

        this.network = network;
        this.personLeaveGarage = new HashMap<>();
        this.personBeenPickedUp = new HashMap<>();
        this.personBeenDropped = new HashMap<>();
        this.personAtGarage = new HashMap<>();
    }



    @Override
    public void reset(int iteration) {
        this.personBeenPickedUp.clear();
        this.personLeaveGarage.clear();
        this.personBeenDropped.clear();
        this.personAtGarage.clear();
    }

    @Override
    public void handleEvent(ActivityStartEvent event) {
        if (!event.getActType().equals("garage") && this.personBeenPickedUp.get(event.getPersonId())!=null && this.personBeenPickedUp.get(event.getPersonId())==0.0) {
            this.personBeenPickedUp.put(event.getPersonId(), event.getTime());
        }
        if (event.getActType().equals("garage")) { // && this.personBeenDropped.get(event.getPersonId())!=null && this.personBeenDropped.get(event.getPersonId())!=0
            this.personAtGarage.put(event.getPersonId(), event.getTime());
        } else {
            this.personAtGarage.put(event.getPersonId(), null);
        }
    }


    @Override
    public void handleEvent(ActivityEndEvent event) {
        if (event.getActType().equals("garage") && this.personLeaveGarage.get(event.getPersonId())==null && this.personBeenPickedUp.get(event.getPersonId())==null) {
            this.personLeaveGarage.put(event.getPersonId(), event.getTime());
            this.personBeenPickedUp.put(event.getPersonId(), 0.0);
        }
        if (!event.getActType().equals("garage")) {
            this.personBeenDropped.put(event.getPersonId(), event.getTime()); // how to make sure to get the one just before garage
            this.personAtGarage.put(event.getPersonId(), 0.0);
        }
    }

    public void writeList(String filename) throws FileNotFoundException {

        //PrintWriter pw = new PrintWriter(filename);
        PrintWriter pw = new PrintWriter(new File(filename));
        pw.println("personId,garageEndTime,NextActivityStartTime, Duration, PreviousActivityEndTime,GarageArrival, Duration2");

        for (Id<Person> key : this.personLeaveGarage.keySet()){
            StringBuilder sb = new StringBuilder();
            sb.append(key).append(",");
            sb.append(this.personLeaveGarage.get(key)).append(",");
            sb.append(this.personBeenPickedUp.get(key)).append(",");
            if (this.personBeenPickedUp.get(key) != 0.0) {
                double Dur = (this.personBeenPickedUp.get(key) - this.personLeaveGarage.get(key));
                sb.append(Dur).append(",");
            }
            sb.append(this.personBeenDropped.get(key)).append(",");
            sb.append(this.personAtGarage.get(key)).append(",");
            if (this.personBeenDropped.get(key) != null && this.personAtGarage.get(key) != null && this.personAtGarage.get(key) !=0.0) {
                double Dur2 = (this.personAtGarage.get(key) - this.personBeenDropped.get(key));
                sb.append(Dur2).append(",");
            }
            pw.println(sb);
        }
        pw.close();
    }
}


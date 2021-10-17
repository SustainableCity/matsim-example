package Thesis.No3_Analysis;


import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class GarageLegEventHandler implements LinkEnterEventHandler, LinkLeaveEventHandler, PersonDepartureEventHandler {


    private Network network;

    private Map<Id<Person>, Double> TimeOfGarageTrip;
    private Map<Id<Person>, Double> TimeOfNextActivity;
    private Map<Id<Person>, Double> TimeOfPrevActivity;
    private Map<Id<Person>, Double> TimeOfGarageArrival;

    private Map<Id<Vehicle>, Double> legTravelTime;
    private Map<Id<Vehicle>, Double> legDistance;
    private Map<Id<Vehicle>, Double> legTravelTime2;
    private Map<Id<Vehicle>, Double> legDistance2;

    private static String PersonList = "C:/matsimfiles/output/simulation/Final/garageTimePRD.csv" ;
    private static String csvFile = "C:/matsimfiles/output/eventAnalysis/GarageDistancesPRD.csv" ;

    public GarageLegEventHandler(Network network ) throws IOException {


        TimeOfGarageTrip = new HashMap<>();
        TimeOfNextActivity = new HashMap<>();
        TimeOfPrevActivity = new HashMap<>();
        TimeOfGarageArrival = new HashMap<>();

        legTravelTime = new HashMap<>();
        legDistance = new HashMap<>();
        legTravelTime2 = new HashMap<>();
        legDistance2 = new HashMap<>();

        this.network = network ;
        //readPersonCSV();

    }

    public void readPersonCSV() throws IOException {

        BufferedReader garageReader = new BufferedReader(new FileReader(PersonList)); // garagePath
        garageReader.readLine();

        String line;
        while ((line = garageReader.readLine()) != null) {

            System.out.println(line);

            String[] spalten = line.split(",");
            if (spalten.length < 5) {
                continue;
            }
            String Spalte3 = line.split(",")[3];
            String Spalte4 = line.split(",")[4];
            if (Spalte3.equals("null")) {
                continue;
            }
            if (Spalte4.equals("null")) {
                continue;
            }
            int PersonID = Integer.parseInt((line.split(",")[0]));
            double garageEndTime = Double.parseDouble((line.split(",")[1]));
            double NextActStartTime = Double.parseDouble((line.split(",")[2]));
            double PreviousActivityEndTime = Double.parseDouble(Spalte3);
            double GarageArrival = Double.parseDouble(Spalte4);

            this.TimeOfGarageTrip.put(Id.createPersonId(PersonID), garageEndTime);
            this.TimeOfNextActivity.put(Id.createPersonId(PersonID), NextActStartTime);
            this.TimeOfPrevActivity.put(Id.createPersonId(PersonID), PreviousActivityEndTime);
            this.TimeOfGarageArrival.put(Id.createPersonId(PersonID), GarageArrival);

        }
    }

    @Override
    public void reset(int iteration) {
        this.TimeOfGarageTrip.clear();
        this.TimeOfNextActivity.clear();
        this.TimeOfPrevActivity.clear();
        this.TimeOfGarageArrival.clear();
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {

        String id = event.getVehicleId().toString();
// this.TimeOfGarageTrip.get(Id.createPersonId(id))!=null &&
/*        System.out.println("Start " +this.TimeOfGarageTrip.get(Id.createVehicleId(id)));
        System.out.println("stop " + event.getTime());
        System.out.println("dre " + this.TimeOfNextActivity.get(Id.createVehicleId(id)));*/
        // System.out.println("stop" +     );
        if(this.TimeOfGarageTrip.get(event.getVehicleId())!=null && this.TimeOfGarageTrip.get(event.getVehicleId())<=event.getTime() && this.TimeOfNextActivity.get(event.getVehicleId())>=event.getTime()){
            Link link = network.getLinks().get( event.getLinkId() ) ;
            double distance = this.legDistance.get(event.getVehicleId()) + link.getLength();
            System.out.println("distance " + distance);
            this.legDistance.put(event.getVehicleId(), distance);
        }

        if(this.TimeOfPrevActivity.get(event.getVehicleId())!=null && this.TimeOfGarageArrival.get(event.getVehicleId())!=null && this.TimeOfPrevActivity.get(event.getVehicleId())<=event.getTime() && this.TimeOfGarageArrival.get(event.getVehicleId())>=event.getTime()){
            Link link = network.getLinks().get( event.getLinkId() ) ;
            double distance2 = this.legDistance2.get(event.getVehicleId()) + link.getLength();
            System.out.println("distance " + distance2);
            this.legDistance2.put(event.getVehicleId(), distance2);
        }

//        System.out.println("stop");

    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        Id<Vehicle> vehId = Id.create( event.getPersonId(), Vehicle.class ) ; // unfortunately necessary since vehicle departures are not uniformly registered


        if (this.TimeOfGarageTrip.get(event.getPersonId())!=null && this.TimeOfGarageTrip.get(event.getPersonId())>=event.getTime() ){
            this.legDistance.put( vehId, 0.0 ) ;
            this.legTravelTime.put(vehId, 0.0);
        }

        if (this.TimeOfPrevActivity.get(event.getPersonId())!=null && this.TimeOfPrevActivity.get(event.getPersonId())>=event.getTime() ){
            this.legDistance2.put( vehId, 0.0 ) ;
            this.legTravelTime2.put(vehId, 0.0);
        }
//        this.earliestLinkExitTime.put( vehId, event.getTime() ) ;
//        this.enterLinkTime.put(vehId, event.getTime());
//        this.freeFlowTravelTime.put(vehId, event.getTime());
//        this.latestLinkExitTime.put(vehId, event.getTime());
//        this.congestedTravelTime.put(vehId, event.getTime());

    }

    public void writeList(String filename) throws FileNotFoundException {

        //PrintWriter pw = new PrintWriter(filename);
        PrintWriter pw = new PrintWriter(new File(csvFile));
        pw.println("vehicleId,legDistance,legDistance2");

        for (Id<Vehicle> key : this.legDistance.keySet()){
            StringBuilder sb = new StringBuilder();
            sb.append(key).append(",");
            //           sb.append(this.personLeaveGarage.get(key)).append(",");
            sb.append(this.legDistance.get(key)).append(",");
            sb.append(this.legDistance2.get(key)).append(",");
            pw.println(sb);
        }
        pw.close();
    }

    @Override
    public void handleEvent(LinkLeaveEvent linkLeaveEvent) {

    }
}
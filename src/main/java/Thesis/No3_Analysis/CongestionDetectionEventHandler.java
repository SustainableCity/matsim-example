package Thesis.No3_Analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.locationchoice.utils.PlanUtils;
import org.matsim.vehicles.Vehicle;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class CongestionDetectionEventHandler implements LinkEnterEventHandler, LinkLeaveEventHandler, PersonDepartureEventHandler {

        private Map<Id<Vehicle>,Double> enterLinkTime = new HashMap<>() ;
        private Map<Id<Vehicle>,Double> earliestLinkExitTime = new HashMap<>() ;
        private Map<Id<Vehicle>,Double> latestLinkExitTime = new HashMap<>() ;
        private Map<Id<Vehicle>,Double> freeFlowTravelTime = new HashMap<>() ;
        private Map<Id<Vehicle>,Double> congestedTravelTime = new HashMap<>() ;
        private Map<Id<Vehicle>,Double> vehicleDelayTime = new HashMap<>() ;
        private Map<Id<Link>, Integer> garageLinks = new HashMap<>() ;
        private Map<Id<Vehicle>, Double> garageTripLength = new HashMap<>() ;
        private Map<Id<Vehicle>, Double> garageTripTime = new HashMap<>() ;
        private Map<Id<Vehicle>, Double> tripLength = new HashMap<>() ;
        private Network network;

        private Map<Integer, Map<Integer,Map<Double, Map<Double, Integer>>>> output = new HashMap<>();

    private static String csvFile = "C:/matsimfiles/output/eventAnalysis/CongestionPR.csv" ;



        //public ActivityStartEventHandler( Network network ) { this.network = network ; }


	public CongestionDetectionEventHandler( Network network ) {
            this.network = network ;
        }

        @Override
        public void reset(int iteration) {
            this.earliestLinkExitTime.clear();
        }

        @Override
        public void handleEvent(LinkEnterEvent event) {
            Link link = network.getLinks().get( event.getLinkId() ) ;
            double linkTravelTime = link.getLength() / link.getFreespeed( event.getTime() );
            this.enterLinkTime.put(event.getVehicleId(), event.getTime());
            this.earliestLinkExitTime.put( event.getVehicleId(), event.getTime() + linkTravelTime ) ;
            this.freeFlowTravelTime.put( event.getVehicleId(), this.freeFlowTravelTime.get(event.getVehicleId())+linkTravelTime ) ;


//                double distance = this.tripLength.get(event.getVehicleId()) + link.getLength();
//                System.out.println("distance" + distance);
//                this.tripLength.put(event.getVehicleId(), distance);


        }

        @Override
        public void handleEvent(LinkLeaveEvent event) {
            double excessTravelTime = event.getTime() - this.earliestLinkExitTime.get( event.getVehicleId() ) ;
            this.latestLinkExitTime.put( event.getVehicleId(), event.getTime() + excessTravelTime ) ;
            this.congestedTravelTime.put(event.getVehicleId(), this.congestedTravelTime.get(event.getVehicleId()) +event.getTime() - enterLinkTime.get(event.getVehicleId()));
//            this.garageTripTime.put(event.getVehicleId(), this.garageTripTime.get(event.getVehicleId()) + event.getTime() - enterLinkTime.get(event.getVehicleId()));
            Link link = network.getLinks().get( event.getLinkId() ) ;

        }

        @Override
        public void handleEvent(PersonDepartureEvent event) {
            Id<Vehicle> vehId = Id.create( event.getPersonId(), Vehicle.class ) ; // unfortunately necessary since vehicle departures are not uniformly registered
            this.earliestLinkExitTime.put( vehId, event.getTime() ) ;
            this.enterLinkTime.put(vehId, event.getTime());
            this.freeFlowTravelTime.put(vehId, event.getTime());
            this.latestLinkExitTime.put(vehId, event.getTime());
            this.congestedTravelTime.put(vehId, event.getTime());

        }



    public void writeChart(String filename) throws FileNotFoundException {

        Map<Id<Vehicle>,Double> tempMap = congestedTravelTime;

        //PrintWriter pw = new PrintWriter(filename);
        PrintWriter pw = new PrintWriter(new File(csvFile));
        pw.println("vehId,freeFlowTravelTime,congestedTravelTime,congestion");



        for (Id<Vehicle> key : this.congestedTravelTime.keySet()){
            StringBuilder sb = new StringBuilder();
            sb.append(key).append(",");
            sb.append(this.freeFlowTravelTime.get(key)).append(",");
            sb.append(this.congestedTravelTime.get(key)).append(",");
            double Cong = this.congestedTravelTime.get(key)-this.freeFlowTravelTime.get(key);
            sb.append(Cong).append(",");
//            sb.append(this.tripLength.get(key)).append(",");
            pw.println(sb);
        }

        pw.close();
    }
}

package Thesis.No3_Analysis;

import Thesis.No3_Analysis.CongestionDetectionEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;

public class RunEventsExample {

        public static void main(String[] args) {
            EventsManager manager = EventsUtils.createEventsManager();



            Network network = NetworkUtils.createNetwork();
            MatsimNetworkReader reader = new MatsimNetworkReader(network);
            reader.readFile("C:/matsimfiles/input/mergedNetwork2018.xml");

            manager.addHandler(new CongestionDetectionEventHandler(network));

            EventsReaderXMLv1 eventsReader = new EventsReaderXMLv1(manager);
            eventsReader.readFile("C:/matsimfiles/output/simulation/output_events.xml.gz");
        }
}

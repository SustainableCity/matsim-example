package RandomDemandGeneration;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;



public class RunModifyNetworkExample {
    public static void main(String[] args) {

        // read in the network
        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile("src/main/java/RandomDemandGeneration/network.xml");

        // iterate through all links
        for (Link l : network.getLinks().values()){
            String a = "43";
            //set new type
            NetworkUtils.setType(l,a);

        }
        new NetworkWriter(network).write("src/main/java/RandomDemandGeneration/network1.xml");
    }




}

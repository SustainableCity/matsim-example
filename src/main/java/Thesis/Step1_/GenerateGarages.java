package Thesis.Step1_;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class GenerateGarages {

        //Specify all input files
        private static final String NETWORKFILE = "C:/matsimfiles/input/MergedNetwork2018.xml";      //Network for traffic assignment
        private static final String COUNTIES = "C:/matsimfiles/input/lkr_ex.shp";          //Polygon shapefile for demand generation
        private static final String PLANSFILEOUTPUT = "C:/matsimfiles/output/plans_NEW.xml";    //The output file of demand generation
        //Define objects and parameters
        private Scenario scenario;
        private Map<String, Geometry> shapeMap;
        //Due to the huge number of commuters, it's preferable to decrease the size of simulating agents.
        //+ This scale factor should be the same as the flow capacity factor. Try different value to evaluate the performance.
        private static double SCALEFACTOR = 0.01;
        //Define the coordinate transformation function
        private final CoordinateTransformation transformation =
                TransformationFactory.getCoordinateTransformation("EPSG:31468", TransformationFactory.DHDN_GK4);


        // Entering point of the class ""Generate Garages
        public static void main(String[] args) {
            GenerateGarages gG = new GenerateGarages();
            gG.run();
        }

        //A constructor for this class, which is to set up the scenario container.
        GenerateGarages (){
            this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
            new MatsimNetworkReader(scenario.getNetwork()).readFile(NETWORKFILE);
        }
        //Generate randomly sampling demand
        private void run() {
            this.shapeMap = readShapeFile(COUNTIES, "SCH");

            //write a new method to create OD relations
/*        createOD(2005,0.6,"09162","09761","Munich City-Augsburg City");
        createOD(5491,0.4,"09162","09174","Munich City-Dachau");
        createOD(5761,0.4,"09162","09175","Munich City-Ebersberg");
        createOD(3364,0.5,"09162","09177","Munich City-Erding");
        createOD(9918,0.5,"09162","09178","Munich City-Freising");
        createOD(7065,0.6,"09162","09179","Munich City-Fuerstenfeldbruck");
        createOD(1776,0.8,"09162","09161","Munich City-Ingolstadt City");
        createOD(78001,0.3,"09162","09184","Munich City-Munich County");
        createOD(2169,0.7,"09162","09182","Munich City-Miesbach");
        createOD(8137,0.5,"09162","09188","Munich City-Starnberg");

        createOD(9170,0.6,"09761","09162","Augsburg City-Munich City");
        createOD(27831,0.4,"09174","09162","Dachau-Munich City");
        createOD(21257,0.4,"09175","09162","Ebersberg-Munich City");
        createOD(12767,0.5,"09177","09162","Erding-Munich City");
        createOD(18339,0.5,"09178","09162","Freising-Munich City");
        createOD(35780,0.6,"09179","09162","Fuerstenfeldbruck-Munich City");
        createOD(64479,0.3,"09184","09162","Munich County-Munich City");
        createOD(15669,0.5,"09188","09162","Starnberg-Munich City");
        createOD(8065,0.6,"09186","09162","Pfaffenhofen-Munich City");
        createOD(8360,0.6,"09163","09162","Rosenheim-Munich City");*/

            createOD(466885,0.24,"09162","09162","Munich City-Munich City");

            //Write the population file to specified folder
            PopulationWriter pw = new PopulationWriter(scenario.getPopulation(),scenario.getNetwork());
            pw.write(PLANSFILEOUTPUT);
        }

        //Read in shapefile
        public Map<String,Geometry> readShapeFile(String filename, String attrString){
            Map<String,Geometry> shapeMap = new HashMap<String, Geometry>();
            for (SimpleFeature ft : ShapeFileReader.getAllFeatures(filename)) {
                GeometryFactory geometryFactory= new GeometryFactory();
                WKTReader wktReader = new WKTReader(geometryFactory);
                Geometry geometry;
                try {
                    geometry = wktReader.read((ft.getAttribute("the_geom")).toString());
                    shapeMap.put(ft.getAttribute(attrString).toString(),geometry);
                } catch (ParseException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            return shapeMap;
        }
        //Create random coordinates within a given polygon
        private Coord drawRandomPointFromGeometry(Geometry g) {
            Random rnd = MatsimRandom.getLocalInstance();
            Point p;
            double x, y;
            do {
                x = g.getEnvelopeInternal().getMinX() + rnd.nextDouble() * (g.getEnvelopeInternal().getMaxX() - g.getEnvelopeInternal().getMinX());
                y = g.getEnvelopeInternal().getMinY() + rnd.nextDouble() * (g.getEnvelopeInternal().getMaxY() - g.getEnvelopeInternal().getMinY());
                p = MGC.xy2Point(x, y);
            } while (!g.contains(p));
            Coord coord = new Coord(p.getX(), p.getY());
            return coord;
        }


        //Create od relations for each counties
        private void createOD(int pop, double shareOfcar, String origion, String destination, String toFromPrefix) {

            //Specify the number of commuters and the modal split of this relation
            double commuters = pop*SCALEFACTOR;
            double carcommuters = shareOfcar * commuters;
            //Specify the ID of these two cities, which is the SCH attribute.
            Geometry home = this.shapeMap.get(origion);
            Geometry work = this.shapeMap.get(destination);
            //Randomly creating the home and work location of each commuters
            for (int i = 0; i<=commuters;i++) {
                //Specify the transportation mode
                String mode = "car";
                /*            if (i > carcommuters) mode = "pt";*/
                //Specify the home location randomly and transform the coordinate
                Coord homec = drawRandomPointFromGeometry(home);
                homec = transformation.transform(homec);
                //Specify the working location randomly and transform the coordinate
                Coord workc = drawRandomPointFromGeometry(work);
                workc = transformation.transform(workc);
                //Create plan for each commuter
                createOnePerson(i, homec, workc, mode, toFromPrefix);
            }
        }



        //Create plan for each commuters
        private void createOnePerson(int i, Coord coord, Coord coordWork, String mode, String toFromPrefix) {

            double variance = Math.random()*120;

            Id<Person> personId = Id.createPersonId(toFromPrefix+i);
            Person person = scenario.getPopulation().getFactory().createPerson(personId);

            Plan plan = scenario.getPopulation().getFactory().createPlan();

      //      Activity garage = scenario.getPopulation().getFactory().createActivityFromCoord("garage",4471875,5333197);
        //    garage.setEndTime(7*60*60);
      //      plan.addActivity(garage);

            Leg leg2home = scenario.getPopulation().getFactory().createLeg(mode);
            plan.addLeg(leg2home);

            Activity home = scenario.getPopulation().getFactory().createActivityFromCoord("home", coord);
            home.setEndTime(7*60*60+variance*60);
            plan.addActivity(home);

            Leg hinweg = scenario.getPopulation().getFactory().createLeg(mode);
            plan.addLeg(hinweg);

            Activity work = scenario.getPopulation().getFactory().createActivityFromCoord("work", coordWork);
            work.setEndTime(17*60*60+variance*60);
            plan.addActivity(work);

            Leg rueckweg = scenario.getPopulation().getFactory().createLeg(mode);
            plan.addLeg(rueckweg);

            Activity home2 = scenario.getPopulation().getFactory().createActivityFromCoord("home", coord);
            plan.addActivity(home2);

            Leg legback2garage = scenario.getPopulation().getFactory().createLeg(mode);
            plan.addLeg(legback2garage);

       //     Activity garage2 = scenario.getPopulation().getFactory().createActivityFromCoord("garage", 4471875,5333197);
       //     plan.addActivity(garage2);

            person.addPlan(plan);
            scenario.getPopulation().addPerson(person);
        }

    }



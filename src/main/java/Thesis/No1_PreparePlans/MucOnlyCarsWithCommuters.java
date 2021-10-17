package Thesis.No1_PreparePlans;

import java.util.*;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.locationchoice.utils.PlanUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MucOnlyCarsWithCommuters {

    private static final String PLANSFILEINPUT = "C:/matsimfiles/input/plans_2011_5.xml";
    private static final String PLANSFILEOUTPUT = "C:/matsimfiles/output/plans_CarCom_5.xml";
    private static final String Network = "C:/matsimfiles/input/mergedNetwork2018.xml";
    private static final String COUNTIES = "C:/matsimfiles/input/lkr_ex.shp";          //Polygon shapefile for demand generation


    public static void main(String[] args) {
        Config config = ConfigUtils.createConfig();
        config.plans().setInputFile(PLANSFILEINPUT);
        config.network().setInputFile(Network);

        Scenario scenario = ScenarioUtils.loadScenario(config);

        final Population pop = scenario.getPopulation();

        int countPlans = 0;
        System.out.println(pop.getPersons().size());

        Map<String, Geometry> shapeMap;
        shapeMap = readShapeFile(COUNTIES, "SCH");
        Geometry munich = shapeMap.get("09162");
        Point p1;
        Point p2;
        double x1;
        double y1;
        double x2;
        double y2;

        System.out.println(pop.getPersons().size());

        for (Iterator<? extends Person> it = pop.getPersons().values().iterator(); it.hasNext(); ) {
            Person person = it.next();
            Plan plan = person.getSelectedPlan();

            Legloop:
            for (Leg leg : TripStructureUtils.getLegs(plan)) {

                x1 = PlanUtils.getPreviousActivity(plan, leg).getCoord().getX();
                y1 = PlanUtils.getPreviousActivity(plan, leg).getCoord().getY();
                x2 = PlanUtils.getNextActivity(plan, leg).getCoord().getX();
                y2 = PlanUtils.getNextActivity(plan, leg).getCoord().getY();
                p1 = MGC.xy2Point(x1, y1);
                p2 = MGC.xy2Point(x2, y2);

                if (TransportMode.walk.equals(leg.getMode()) || TransportMode.pt.equals(leg.getMode()) || TransportMode.bike.equals(leg.getMode()) || (!munich.contains(p1) && !munich.contains(p2))) {
                    it.remove();

//                    if (PlanUtils.getPreviousActivity(plan, leg).getType().contains("other") || PlanUtils.getPreviousActivity(plan, leg).getType().contains("home") || PlanUtils.getPreviousActivity(plan, leg).getType().contains("education")) {
//                        it.remove();
                }
                break Legloop;
            }

        }

        System.out.println(pop.getPersons().size());

        PopulationWriter pw = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
        pw.write(PLANSFILEOUTPUT);

    }

        //Read in shapefile
        public static Map<String, Geometry> readShapeFile (String filename, String attrString){
            Map<String, Geometry> shapeMap = new HashMap<String, Geometry>();
            for (SimpleFeature ft : ShapeFileReader.getAllFeatures(filename)) {
                GeometryFactory geometryFactory = new GeometryFactory();
                WKTReader wktReader = new WKTReader(geometryFactory);
                Geometry geometry;
                try {
                    geometry = wktReader.read((ft.getAttribute("the_geom")).toString());
                    shapeMap.put(ft.getAttribute(attrString).toString(), geometry);
                } catch (ParseException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            return shapeMap;
        }
    }
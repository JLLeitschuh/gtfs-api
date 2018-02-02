package com.conveyal.gtfs.api.models;

import com.conveyal.gtfs.GTFSFeed;
import com.conveyal.gtfs.model.Route;
import com.conveyal.gtfs.model.Stop;
//import com.googlecode.concurrenttrees.radix.ConcurrentRadixTree;
//import com.googlecode.concurrenttrees.radix.RadixTree;
import com.conveyal.gtfs.stats.FeedStats;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharArrayNodeFactory;
import com.googlecode.concurrenttrees.suffix.ConcurrentSuffixTree;
import com.googlecode.concurrenttrees.suffix.SuffixTree;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.strtree.STRtree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * This is not at all the same thing as a FeedSource in Conveyal Data Manager.
 * It's not an API model class. It's a wrapper around a GTFSFeed with extra indexes and stats.
 */
public class FeedSource {
    private static final Logger LOG = LoggerFactory.getLogger(FeedSource.class);

    public SuffixTree<Stop> stopTree;
    public SuffixTree<Route> routeTree;
    public GTFSFeed feed;
    public FeedStats stats;

    /**
     * a unique ID for this feed source. Not a GTFS feed ID as they are not unique between versions of
     * the same feed.
     */
    public String id;

    public FeedSource (GTFSFeed feed) {
        this.feed = feed;
        // TODO this is hack to keep GTFS API working as it was before. We need actually to have unique IDs for feeds
        // independent of GTFS Feed ID as we track multiple versions of feeds.
        this.id = feed.feedId;
        initIndexes();
        initStats();
    }

    public void initStats () {
        // Initialize stats
        this.stats = new FeedStats(feed);
    }
    public void initIndexes(){
        // Initialize and build route radix tree and spatial index
        this.routeTree = new ConcurrentSuffixTree<Route>( new DefaultCharArrayNodeFactory() ) {};

        // init string index
        for (Route route : this.feed.routes.values()){
            // TODO: consider concatenating short name and long name
            if (route.route_short_name != null && !route.route_short_name.isEmpty())
                this.routeTree.put(route.route_short_name.toUpperCase(), route);

            if (route.route_long_name != null && !route.route_long_name.isEmpty())
                this.routeTree.put(route.route_long_name.toUpperCase(), route);
        }

        // Initialize and build stop radix tree and spatial index
        this.stopTree = new ConcurrentSuffixTree<>( new DefaultCharArrayNodeFactory() );
        for (Stop stop : this.feed.stops.values()){

            // add name string to stopTree
            String stopName = "";
            if (stop.stop_name != null) {
                stopName += stop.stop_name.toUpperCase() + " "; // include space to separate stop_id
            }

            // always add stop_id to stopName to ensure that stops with duplicate names are not filtered out
            if (stop.stop_id != null) {
                stopName += stop.stop_id.toUpperCase();
            }
            if (!"".equals(stopName)) {
                this.stopTree.put(stopName, stop);
            }

            // add stop_code string to stopTree
            String stop_code = "";
            if (stop.stop_code != null){
                stop_code = stop.stop_code;
            }
            else if (stop.stop_id != null) {
                stop_code = stop.stop_id;
            }
            if (!"".equals(stop_code)) {
                this.stopTree.put(stop_code.toUpperCase(), stop);
            }
        }

        // TODO: what happens if new stops or routes need to be added to index (QuadTree)?
    }
}
